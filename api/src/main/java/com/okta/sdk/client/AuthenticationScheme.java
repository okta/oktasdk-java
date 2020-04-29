/*
 * Copyright 2014 Stormpath, Inc.
 * Modifications Copyright 2018 Okta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.okta.sdk.client;

import com.okta.commons.http.authc.DisabledAuthenticator;
import com.okta.commons.http.authc.RequestAuthenticator;
import com.okta.commons.lang.Assert;

/**
 * Enumeration that defines the available HTTP authentication schemes to be used when communicating with the Okta API server.
 * <p>
 * The Authentication Scheme setting is helpful in cases where the code is run in a platform where the header information for
 * outgoing HTTP requests is modified and thus causing communication issues.
 * <p>
 * The SSWS (Okta session bearer token) should be used for the management SDK, {code NONE} should be used for non
 * authenticated requests.
 *
 * @since 0.5.0
 */
public enum AuthenticationScheme {

    SSWS("com.okta.sdk.impl.http.authc.SswsAuthenticator"), //SSWS Authentication
    OAUTH2("com.okta.sdk.impl.http.authc.OAuth2RequestAuthenticator"), // OAuth2 access token
    NONE(DisabledAuthenticator.class);

    private final String requestAuthenticatorClassName;

    AuthenticationScheme(Class<? extends RequestAuthenticator> requestAuthenticatorClass) {
        this(requestAuthenticatorClass.getName());
    }

    AuthenticationScheme(String requestAuthenticatorClassName) {
        Assert.notNull(requestAuthenticatorClassName, "requestAuthenticatorClassName cannot be null");
        this.requestAuthenticatorClassName = requestAuthenticatorClassName;
    }

    public String getRequestAuthenticatorClassName() {
        return this.requestAuthenticatorClassName;
    }
}
