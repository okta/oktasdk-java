/*
 * Copyright 2020-Present Okta, Inc.
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
package com.okta.sdk.impl.http.authc;

import com.okta.commons.http.Request;
import com.okta.commons.http.authc.RequestAuthenticator;
import com.okta.commons.lang.Assert;
import com.okta.commons.lang.Strings;
import com.okta.sdk.authc.credentials.ClientCredentials;

public class OAuth2RequestAuthenticator implements RequestAuthenticator {

    public static final String AUTHENTICATION_SCHEME = "OAUTH2";

    private final ClientCredentials<String> clientCredentials;

    public OAuth2RequestAuthenticator(ClientCredentials<String> clientCredentials) {
        Assert.notNull(clientCredentials, "clientCredentials must be not be null.");
        this.clientCredentials = clientCredentials;
    }

    @Override
    public void authenticate(Request request) {

        String accessToken = clientCredentials.getCredentials();
        if (Strings.hasText(accessToken)) {
            request.getHeaders().set(AUTHORIZATION_HEADER, "Bearer " + accessToken);
        }
        // TODO should we fail?, there are a few publicly readable endpoints
    }
}
