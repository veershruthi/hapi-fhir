package ca.uhn.fhir.jpa.provider;

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

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.springframework.beans.factory.annotation.Required;

import ca.uhn.fhir.jpa.dao.IFhirResourceDao;
import ca.uhn.fhir.model.api.TagList;
import ca.uhn.fhir.rest.annotation.GetTags;
import ca.uhn.fhir.rest.annotation.History;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Since;
import ca.uhn.fhir.rest.method.RequestDetails;
import ca.uhn.fhir.rest.server.IBundleProvider;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.util.CoverageIgnore;

public abstract class BaseJpaResourceProvider<T extends IBaseResource> extends BaseJpaProvider implements IResourceProvider {

	private IFhirResourceDao<T> myDao;

	public BaseJpaResourceProvider() {
		// nothing
	}

	@CoverageIgnore
	public BaseJpaResourceProvider(IFhirResourceDao<T> theDao) {
		myDao = theDao;
	}

	public IFhirResourceDao<T> getDao() {
		return myDao;
	}

	@History
	public IBundleProvider getHistoryForResourceInstance(HttpServletRequest theRequest, @IdParam IIdType theId, @Since Date theDate, RequestDetails theRequestDetails) {
		startRequest(theRequest);
		try {
			return myDao.history(theId, theDate, theRequestDetails);
		} finally {
			endRequest(theRequest);
		}
	}

	@History
	public IBundleProvider getHistoryForResourceType(HttpServletRequest theRequest, @Since Date theDate, RequestDetails theRequestDetails) {
		startRequest(theRequest);
		try {
			return myDao.history(theDate, theRequestDetails);
		} finally {
			endRequest(theRequest);
		}
	}

	@Override
	public Class<? extends IBaseResource> getResourceType() {
		return myDao.getResourceType();
	}

	@GetTags
	public TagList getTagsForResourceInstance(HttpServletRequest theRequest, @IdParam IIdType theResourceId, RequestDetails theRequestDetails) {
		startRequest(theRequest);
		try {
			return myDao.getTags(theResourceId, theRequestDetails);
		} finally {
			endRequest(theRequest);
		}
	}

	@GetTags
	public TagList getTagsForResourceType(HttpServletRequest theRequest, RequestDetails theRequestDetails) {
		startRequest(theRequest);
		try {
			return myDao.getAllResourceTags(theRequestDetails);
		} finally {
			endRequest(theRequest);
		}
	}

	@Read(version = true)
	public T read(HttpServletRequest theRequest, @IdParam IIdType theId, RequestDetails theRequestDetails) {
		startRequest(theRequest);
		try {
			return myDao.read(theId, theRequestDetails);
		} finally {
			endRequest(theRequest);
		}
	}

	@Required
	public void setDao(IFhirResourceDao<T> theDao) {
		myDao = theDao;
	}

}
