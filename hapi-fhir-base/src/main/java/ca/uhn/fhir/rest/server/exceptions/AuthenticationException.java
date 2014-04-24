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


/**
 * Created by dsotnikov on 3/10/2014.
 */
public class AuthenticationException extends BaseServerResponseException {

    private static final long serialVersionUID = 1L;

    public AuthenticationException() {
        super(401, "Client unauthorized");
    }

    public AuthenticationException(String theMessage) {
        super(401, theMessage);
    }

    public AuthenticationException(int theStatusCode, String theMessage) {
        super(theStatusCode, theMessage);
    }
}