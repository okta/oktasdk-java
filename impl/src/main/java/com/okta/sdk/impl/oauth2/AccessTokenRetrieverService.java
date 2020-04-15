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
package com.okta.sdk.impl.oauth2;

import com.okta.commons.http.HttpException;

import java.io.IOException;
import java.security.InvalidKeyException;

public interface AccessTokenRetrieverService {
    /**
     * Obtain OAuth2 access token from Authorization Server endpoint.
     *
     * @return {@link OAuth2AccessToken}
     * @throws IOException
     * @throws InvalidKeyException
     * @throws OAuth2TokenRetrieverException
     */
    OAuth2AccessToken getOAuth2AccessToken() throws IOException, InvalidKeyException, OAuth2TokenRetrieverException;
}