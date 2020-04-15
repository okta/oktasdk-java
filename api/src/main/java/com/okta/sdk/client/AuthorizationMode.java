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
package com.okta.sdk.client;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration that defines the mapping between available Authentication schemes and Authorization modes.
 */
public enum AuthorizationMode {

    SSWS(AuthenticationScheme.SSWS), // SSWS
    PrivateKey(AuthenticationScheme.OAUTH2), // OAuth2
    NONE(AuthenticationScheme.NONE); // None

    private final AuthenticationScheme authenticationScheme;

    private static final Map<AuthenticationScheme, AuthorizationMode> lookup = new HashMap<>();

    static {
        for (AuthorizationMode authorizationMode : AuthorizationMode.values()) {
            lookup.put(authorizationMode.getAuthenticationScheme(), authorizationMode);
        }
    }

    AuthorizationMode(AuthenticationScheme authenticationScheme) {
        this.authenticationScheme = authenticationScheme;
    }

    public AuthenticationScheme getAuthenticationScheme() {
        return this.authenticationScheme;
    }

    public static AuthorizationMode get(AuthenticationScheme authenticationScheme) {
        return lookup.get(authenticationScheme);
    }
}
