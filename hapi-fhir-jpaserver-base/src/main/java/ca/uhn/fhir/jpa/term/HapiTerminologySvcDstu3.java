package ca.uhn.fhir.jpa.term;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/*
 * #%L
 * HAPI FHIR JPA Server
 * %%
 * Copyright (C) 2014 - 2016 University Health Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hl7.fhir.dstu3.hapi.validation.IValidationSupport;
import org.hl7.fhir.dstu3.model.CodeSystem;
import org.hl7.fhir.dstu3.model.CodeSystem.CodeSystemContentMode;
import org.hl7.fhir.dstu3.model.CodeSystem.ConceptDefinitionComponent;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.dstu3.model.ValueSet.ConceptReferenceComponent;
import org.hl7.fhir.dstu3.model.ValueSet.ConceptSetComponent;
import org.hl7.fhir.dstu3.model.ValueSet.ConceptSetFilterComponent;
import org.hl7.fhir.dstu3.model.ValueSet.FilterOperator;
import org.hl7.fhir.dstu3.model.ValueSet.ValueSetExpansionComponent;
import org.hl7.fhir.dstu3.model.ValueSet.ValueSetExpansionContainsComponent;
import org.hl7.fhir.dstu3.terminologies.ValueSetExpander;
import org.hl7.fhir.dstu3.terminologies.ValueSetExpander.ValueSetExpansionOutcome;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.dao.IFhirResourceDaoCodeSystem;
import ca.uhn.fhir.jpa.entity.ResourceTable;
import ca.uhn.fhir.jpa.entity.TermCodeSystem;
import ca.uhn.fhir.jpa.entity.TermCodeSystemVersion;
import ca.uhn.fhir.jpa.entity.TermConcept;
import ca.uhn.fhir.rest.method.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.util.CoverageIgnore;
import ca.uhn.fhir.util.UrlUtil;

public class HapiTerminologySvcDstu3 extends BaseHapiTerminologySvc implements IValidationSupport, IHapiTerminologySvcDstu3 {
	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(HapiTerminologySvcDstu3.class);

	@Autowired
	private IFhirResourceDaoCodeSystem<CodeSystem, Coding, CodeableConcept> myCodeSystemResourceDao;

	@Autowired
	private ValueSetExpander myValueSetExpander;

	@Override
	public List<VersionIndependentConcept> expandValueSet(String theValueSet) {
		ValueSet source = new ValueSet();
		source.getCompose().addImport(theValueSet);
		try {
			ArrayList<VersionIndependentConcept> retVal = new ArrayList<VersionIndependentConcept>();

			ValueSetExpansionOutcome outcome = myValueSetExpander.expand(source);
			for (ValueSetExpansionContainsComponent next : outcome.getValueset().getExpansion().getContains()) {
				retVal.add(new VersionIndependentConcept(next.getSystem(), next.getCode()));
			}

			return retVal;

		} catch (Exception e) {
			throw new InternalErrorException(e);
		}

	}

	@Override
	public void storeNewCodeSystemVersion(String theSystem, TermCodeSystemVersion theCodeSystemVersion, RequestDetails theRequestDetails) {
		CodeSystem cs = new org.hl7.fhir.dstu3.model.CodeSystem();
		cs.setUrl(theSystem);
		cs.setContent(CodeSystemContentMode.NOTPRESENT);
		IIdType csId = myCodeSystemResourceDao.create(cs, "CodeSystem?url=" + UrlUtil.escape(theSystem), theRequestDetails).getId().toUnqualifiedVersionless();
		ResourceTable resource = (ResourceTable) myCodeSystemResourceDao.readEntity(csId);
		Long codeSystemResourcePid = resource.getId();

		ourLog.info("CodeSystem resource has ID: {}", csId.getValue());

		theCodeSystemVersion.setResource(resource);
		theCodeSystemVersion.setResourceVersionId(resource.getVersion());
		super.storeNewCodeSystemVersion(codeSystemResourcePid, theSystem, theCodeSystemVersion);

	}

	@Override
	public ValueSetExpansionComponent expandValueSet(FhirContext theContext, ConceptSetComponent theInclude) {
		String system = theInclude.getSystem();
		TermCodeSystem cs = myCodeSystemDao.findByCodeSystemUri(system);
		TermCodeSystemVersion csv = cs.getCurrentVersion();

		FullTextEntityManager em = org.hibernate.search.jpa.Search.getFullTextEntityManager(myEntityManager);
		QueryBuilder qb = em.getSearchFactory().buildQueryBuilder().forEntity(TermConcept.class).get();
		BooleanJunction<?> bool = qb.bool();

		bool.must(qb.keyword().onField("myCodeSystemVersionPid").matching(csv.getPid()).createQuery());

		Set<String> wantCodes = new HashSet<String>();
		for (ConceptReferenceComponent next : theInclude.getConcept()) {
			String nextCode = next.getCode();
			if (isNotBlank(nextCode)) {
				wantCodes.add(nextCode);
				bool.should(qb.keyword().onField("myCode").matching(nextCode).createQuery());
			}
		}

		for (ConceptSetFilterComponent nextFilter : theInclude.getFilter()) {
			if (nextFilter.getProperty().equals("display") && nextFilter.getOp() == FilterOperator.EQUAL) {
				if (isNotBlank(nextFilter.getValue())) {
					bool.must(qb.phrase().onField("myDisplay").sentence(nextFilter.getValue()).createQuery());
				}
			} else if (nextFilter.getOp() == FilterOperator.ISA) {
				if (isNotBlank(nextFilter.getValue())) {
					TermConcept code = super.findCode(system, nextFilter.getValue());
					bool.must(qb.keyword().onField("myParentPids").matching(code.getId()).createQuery());
				}
			} else {
				throw new InvalidRequestException("Unknown filter property[" + nextFilter + "] + op[" + nextFilter.getOpElement().getValueAsString() + "]");
			}
		}
		
		ValueSetExpansionComponent retVal = new ValueSetExpansionComponent();
		
		Query luceneQuery = bool.createQuery();
		FullTextQuery jpaQuery = em.createFullTextQuery(luceneQuery, TermConcept.class);
		@SuppressWarnings("unchecked")
		List<TermConcept> result = jpaQuery.getResultList();
		for (TermConcept nextConcept : result) {
			if (!wantCodes.isEmpty() && !wantCodes.contains(nextConcept.getCode())) {
				continue;
			}
			ValueSetExpansionContainsComponent contains = retVal.addContains();
			contains.setCode(nextConcept.getCode());
			contains.setSystem(system);
			contains.setDisplay(nextConcept.getDisplay());
		}
		
		return retVal;
	}

	private void addCodeIfFilterMatches(ValueSetExpansionComponent retVal, TermConcept termCode, List<ConceptSetFilterComponent> theFilters, String theSystem) {
		ValueSetExpansionContainsComponent contains = retVal.addContains();
		contains.setSystem(theSystem);
		contains.setCode(termCode.getCode());
		contains.setDisplay(termCode.getDisplay());
	}

	@Override
	public List<StructureDefinition> fetchAllStructureDefinitions(FhirContext theContext) {
		return Collections.emptyList();
	}

	@CoverageIgnore
	@Override
	public CodeSystem fetchCodeSystem(FhirContext theContext, String theSystem) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T extends IBaseResource> T fetchResource(FhirContext theContext, Class<T> theClass, String theUri) {
		return null;
	}

	@CoverageIgnore
	@Override
	public StructureDefinition fetchStructureDefinition(FhirContext theCtx, String theUrl) {
		return null;
	}

	@Override
	public boolean isCodeSystemSupported(FhirContext theContext, String theSystem) {
		return super.supportsSystem(theSystem);
	}

	@CoverageIgnore
	@Override
	public CodeValidationResult validateCode(FhirContext theContext, String theCodeSystem, String theCode, String theDisplay) {
		TermConcept code = super.findCode(theCodeSystem, theCode);
		if (code != null) {
			ConceptDefinitionComponent def = new ConceptDefinitionComponent();
			def.setCode(code.getCode());
			def.setDisplay(code.getDisplay());
			return new CodeValidationResult(def);
		}

		return new CodeValidationResult(IssueSeverity.ERROR, "Unkonwn code {" + theCodeSystem + "}" + theCode);
	}

}
