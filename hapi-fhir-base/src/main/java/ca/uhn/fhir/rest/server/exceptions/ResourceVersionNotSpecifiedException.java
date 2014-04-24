package ca.uhn.fhir.rest.server.exceptions;

/*
 * #%L
 * HAPI FHIR Library
 * %%
 * Copyright (C) 2014 University Health Network
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

import ca.uhn.fhir.rest.server.Constants;

/**
 * Thrown for an Update operation if that operation requires a version to 
 * be specified in an HTTP header, and none was.
 */
public class ResourceVersionNotSpecifiedException extends BaseServerResponseException {
	private static final long serialVersionUID = 1L;

	public ResourceVersionNotSpecifiedException(String error) {
		super(Constants.STATUS_HTTP_412_PRECONDITION_FAILED, error);
	}
}