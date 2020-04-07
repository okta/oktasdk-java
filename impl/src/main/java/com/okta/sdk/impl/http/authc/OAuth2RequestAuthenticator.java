package com.okta.sdk.impl.http.authc;

import com.okta.commons.http.Request;
import com.okta.commons.http.authc.RequestAuthenticationException;
import com.okta.commons.http.authc.RequestAuthenticator;
import com.okta.commons.lang.Assert;
import com.okta.sdk.authc.credentials.ClientCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OAuth2RequestAuthenticator implements RequestAuthenticator {

    public static final String AUTHENTICATION_SCHEME = "OAUTH2";

    private final ClientCredentials<String> clientCredentials;

    public OAuth2RequestAuthenticator(ClientCredentials<String> clientCredentials) {
        Assert.notNull(clientCredentials, "clientCredentials must not be null.");
        this.clientCredentials = clientCredentials;
    }

    @Override
    public void authenticate(Request request) throws RequestAuthenticationException {
        request.getHeaders().set(AUTHORIZATION_HEADER, "Bearer " + clientCredentials.getCredentials());
    }
}
