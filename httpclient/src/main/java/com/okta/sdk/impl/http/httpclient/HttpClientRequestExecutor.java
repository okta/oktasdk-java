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
package com.okta.sdk.impl.http.httpclient;

import com.okta.sdk.client.AuthenticationScheme;
import com.okta.sdk.client.Proxy;
import com.okta.sdk.authc.credentials.ClientCredentials;
import com.okta.sdk.impl.config.ClientConfiguration;
import com.okta.sdk.impl.http.HttpHeaders;
import com.okta.sdk.impl.http.MediaType;
import com.okta.sdk.impl.http.QueryString;
import com.okta.sdk.impl.http.Request;
import com.okta.sdk.impl.http.RequestExecutor;
import com.okta.sdk.impl.http.Response;
import com.okta.sdk.impl.http.RestException;
import com.okta.sdk.impl.http.authc.DefaultRequestAuthenticatorFactory;
import com.okta.sdk.impl.http.authc.RequestAuthenticator;
import com.okta.sdk.impl.http.authc.RequestAuthenticatorFactory;
import com.okta.sdk.impl.http.support.BackoffStrategy;
import com.okta.sdk.impl.http.support.DefaultRequest;
import com.okta.sdk.impl.http.support.DefaultResponse;
import com.okta.sdk.lang.Assert;
import com.okta.sdk.lang.Strings;
import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NoHttpResponseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Date;

/**
 * {@code RequestExecutor} implementation that uses the
 * <a href="http://hc.apache.org/httpcomponents-client-ga">Apache HttpClient</a> implementation to
 * execute http requests.
 *
 * @since 0.5.0
 */
public class HttpClientRequestExecutor implements RequestExecutor {

    private static final Logger log = LoggerFactory.getLogger(HttpClientRequestExecutor.class);

    /**
     * Maximum exponential back-off time before retrying a request
     */
    private static final int DEFAULT_MAX_BACKOFF_IN_MILLISECONDS = 20 * 1000;

    private static final int DEFAULT_MAX_RETRIES = 4;

    private static final int DEFAULT_MAX_CONNECTIONS_PER_ROUTE = Integer.MAX_VALUE/2;
    private static final String MAX_CONNECTIONS_PER_ROUTE_PROPERTY_KEY = "com.okta.sdk.impl.http.httpclient.HttpClientRequestExecutor.connPoolControl.maxPerRoute";
    private static final int MAX_CONNECTIONS_PER_ROUTE;

    private static final int DEFAULT_MAX_CONNECTIONS_TOTAL = Integer.MAX_VALUE;
    private static final String MAX_CONNECTIONS_TOTAL_PROPERTY_KEY = "com.okta.sdk.impl.http.httpclient.HttpClientRequestExecutor.connPoolControl.maxTotal";
    private static final int MAX_CONNECTIONS_TOTAL;

    private int maxRetries = DEFAULT_MAX_RETRIES;

    private int maxElapsedMillis = 0;

    private final RequestAuthenticator requestAuthenticator;

    private HttpClient httpClient;

    private BackoffStrategy backoffStrategy;

    private HttpClientRequestFactory httpClientRequestFactory;

    static {
        int connectionMaxPerRoute = DEFAULT_MAX_CONNECTIONS_PER_ROUTE;
        String connectionMaxPerRouteString = System.getProperty(MAX_CONNECTIONS_PER_ROUTE_PROPERTY_KEY);
        if (connectionMaxPerRouteString != null) {
            try {
                connectionMaxPerRoute = Integer.parseInt(connectionMaxPerRouteString);
            } catch (NumberFormatException nfe) {
                log.warn(
                    "Bad max connection per route value: {}. Using default: {}.",
                    connectionMaxPerRouteString, DEFAULT_MAX_CONNECTIONS_PER_ROUTE, nfe
                );
            }
        }
        MAX_CONNECTIONS_PER_ROUTE = connectionMaxPerRoute;

        int connectionMaxTotal = DEFAULT_MAX_CONNECTIONS_TOTAL;
        String connectionMaxTotalString = System.getProperty(MAX_CONNECTIONS_TOTAL_PROPERTY_KEY);
        if (connectionMaxTotalString != null) {
            try {
                connectionMaxTotal = Integer.parseInt(connectionMaxTotalString);
            } catch (NumberFormatException nfe) {
                log.warn(
                    "Bad max connection total value: {}. Using default: {}.",
                    connectionMaxTotalString, DEFAULT_MAX_CONNECTIONS_TOTAL, nfe
                );
            }
        }
        MAX_CONNECTIONS_TOTAL = connectionMaxTotal;
    }

    @SuppressWarnings({"deprecation"})
    public HttpClientRequestExecutor(ClientConfiguration clientConfiguration) {
        this(clientConfiguration.getClientCredentialsResolver().getClientCredentials(),
             clientConfiguration.getProxy(),
             clientConfiguration.getAuthenticationScheme(),
             clientConfiguration.getRequestAuthenticatorFactory(),
             clientConfiguration.getConnectionTimeout());

        if (clientConfiguration.getRetryMaxElapsed() >= 0) {
            maxElapsedMillis = clientConfiguration.getRetryMaxElapsed() * 1000;
        }

        if (clientConfiguration.getRetryMaxAttempts() > 0) {
            maxRetries = clientConfiguration.getRetryMaxAttempts();
        }
    }

    /**
     * Creates a new {@code HttpClientRequestExecutor} using the specified {@code ClientCredentials} and optional {@code Proxy}
     * configuration.
     * @param clientCredentials the Okta account API Key that will be used to authenticate the client with Okta's API sever
     * @param proxy the HTTP proxy to be used when communicating with the Okta API server (can be null)
     * @param authenticationScheme the HTTP authentication scheme to be used when communicating with the Okta API server.
     *                             If null, then SSWS will be used.
     */
    @Deprecated
    public HttpClientRequestExecutor(ClientCredentials clientCredentials, Proxy proxy, AuthenticationScheme authenticationScheme, RequestAuthenticatorFactory requestAuthenticatorFactory, Integer connectionTimeout) {
        Assert.notNull(clientCredentials, "clientCredentials argument is required.");
        Assert.isTrue(connectionTimeout >= 0, "Timeout cannot be a negative number.");

        RequestAuthenticatorFactory factory = (requestAuthenticatorFactory != null)
                ? requestAuthenticatorFactory
                : new DefaultRequestAuthenticatorFactory();

        this.requestAuthenticator = factory.create(authenticationScheme, clientCredentials);

        PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager();

        if (MAX_CONNECTIONS_TOTAL >= MAX_CONNECTIONS_PER_ROUTE) {
            connMgr.setDefaultMaxPerRoute(MAX_CONNECTIONS_PER_ROUTE);
            connMgr.setMaxTotal(MAX_CONNECTIONS_TOTAL);
        } else {
            connMgr.setDefaultMaxPerRoute(DEFAULT_MAX_CONNECTIONS_PER_ROUTE);
            connMgr.setMaxTotal(DEFAULT_MAX_CONNECTIONS_TOTAL);

            log.warn(
                "{} ({}) is less than {} ({}). " +
                "Reverting to defaults: connectionMaxTotal ({}) and connectionMaxPerRoute ({}).",
                MAX_CONNECTIONS_TOTAL_PROPERTY_KEY, MAX_CONNECTIONS_TOTAL,
                MAX_CONNECTIONS_PER_ROUTE_PROPERTY_KEY, MAX_CONNECTIONS_PER_ROUTE,
                DEFAULT_MAX_CONNECTIONS_TOTAL, DEFAULT_MAX_CONNECTIONS_PER_ROUTE
            );
        }

        // The connectionTimeout value is specified in seconds in Okta configuration settings.
        // Therefore, multiply it by 1000 to be milliseconds since RequestConfig expects milliseconds.
        int connectionTimeoutAsMilliseconds = connectionTimeout * 1000;

        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(connectionTimeoutAsMilliseconds)
                .setSocketTimeout(connectionTimeoutAsMilliseconds).setRedirectsEnabled(false).build();

        ConnectionConfig connectionConfig = ConnectionConfig.custom().setCharset(Consts.UTF_8).build();

        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig)
                .disableCookieManagement().setDefaultConnectionConfig(connectionConfig).setConnectionManager(connMgr);

        this.httpClientRequestFactory = new HttpClientRequestFactory(requestConfig);

        if (proxy != null) {
            //We have some proxy setting to use!
            HttpHost httpProxyHost = new HttpHost(proxy.getHost(), proxy.getPort());
            httpClientBuilder.setProxy(httpProxyHost);

            if (proxy.isAuthenticationRequired()) {
                AuthScope authScope = new AuthScope(proxy.getHost(), proxy.getPort());
                Credentials credentials = new UsernamePasswordCredentials(proxy.getUsername(), proxy.getPassword());
                CredentialsProvider credentialsProviderProvider = new BasicCredentialsProvider();
                credentialsProviderProvider.setCredentials(authScope, credentials);
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProviderProvider);
            }
        }

        this.httpClient = httpClientBuilder.build();
    }

    @Deprecated
    public int getNumRetries() {
        return maxRetries;
    }

    @Deprecated
    public void setNumRetries(int numRetries) {
        this.maxRetries = numRetries;
    }

    public BackoffStrategy getBackoffStrategy() {
        return this.backoffStrategy;
    }

    public void setBackoffStrategy(BackoffStrategy backoffStrategy) {
        this.backoffStrategy = backoffStrategy;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public Response executeRequest(Request request) throws RestException {

        Assert.notNull(request, "Request argument cannot be null.");

        int retryCount = 0;
        URI redirectUri = null;
        HttpEntity entity = null;
        HttpResponse httpResponse = null;
        String requestId = null;
        Timer timer = new Timer();

        // Make a copy of the original request params and headers so that we can
        // permute them in the loop and start over with the original every time.
        QueryString originalQuery = new QueryString();
        originalQuery.putAll(request.getQueryString());

        HttpHeaders originalHeaders = new HttpHeaders();
        originalHeaders.putAll(request.getHeaders());

        while (true) {

            if (redirectUri != null) {
                request = new DefaultRequest(
                        request.getMethod(),
                        redirectUri.toString(),
                        null,
                        null,
                        request.getBody(),
                        request.getHeaders().getContentLength()
                );
            }

            if (retryCount > 0) {
                request.setQueryString(originalQuery);
                request.setHeaders(originalHeaders);

                // remember the request-id header if we need to retry
                if (requestId == null) {
                    requestId = getRequestId(httpResponse);
                }
            }

            // Sign the request
            this.requestAuthenticator.authenticate(request);

            HttpRequestBase httpRequest = this.httpClientRequestFactory.createHttpClientRequest(request, entity);

            if (httpRequest instanceof HttpEntityEnclosingRequest) {
                entity = ((HttpEntityEnclosingRequest) httpRequest).getEntity();
            }

            try {
                // We don't want to treat a redirect like a retry,
                // so if redirectUri is not null, we won't pause
                // before executing the request below.
                if (retryCount > 0 && redirectUri == null) {
                    try {
                        // if we cannot pause, then return the original response
                        pauseBeforeRetry(retryCount, httpResponse, timer.split());
                    } catch (RestException e) {
                        if (log.isDebugEnabled()) {
                            log.warn("Unable to pause for retry: {}", e.getMessage(), e);
                        } else {
                            log.warn("Unable to pause for retry: {}", e.getMessage());
                        }

                        return toSdkResponse(httpResponse);
                    }

                    resetStream(entity);
                }

                // reset redirectUri so that if there is an exception, we will pause on retry
                redirectUri = null;
                retryCount++;

                // include X-Okta headers when retrying
                setOktaHeaders(request, requestId, retryCount);

                httpResponse = httpClient.execute(httpRequest);

                if (isRedirect(httpResponse)) {
                    redirectUri = getRedirectUri(httpResponse);
                    httpRequest.setURI(redirectUri);
                } else {

                    Response response = toSdkResponse(httpResponse);

                    //allow the loop to continue to execute a retry request
                    if (!shouldRetry(response, retryCount, timer.split())) {
                        return response;
                    }
                }
            } catch (Throwable t) {
                log.warn("Unable to execute HTTP request: ", t.getMessage(), t);

                if (!shouldRetry(httpRequest, t, retryCount, timer.split())) {
                    throw new RestException("Unable to execute HTTP request: " + t.getMessage(), t);
                }
            } finally {
                try {
                    httpResponse.getEntity().getContent().close();
                } catch (Throwable ignored) { // NOPMD
                }
            }
        }
    }

    /**
     * Resets entities InputStream if supported
     */
    private void resetStream(HttpEntity entity) throws IOException {
        if (entity != null) {
            InputStream content = entity.getContent();
            if (content.markSupported()) {
                content.reset();
            }
        }
    }

    /**
     * Adds {@code X-Okta-Retry-For} and {@code X-Okta-Retry-Count} headers to request if not null/empty or zero.
     *
     * @param request the request to add headers too
     * @param requestId request ID of the original request that failed
     * @param retryCount the number of times the request has been retried
     */
    private void setOktaHeaders(Request request, String requestId, int retryCount) {
        if (Strings.hasText(requestId)) {
            request.getHeaders().add("X-Okta-Retry-For", requestId);
        }
        if (retryCount > 1) {
            request.getHeaders().add("X-Okta-Retry-Count", Integer.toString(retryCount));
        }
    }

    private String getRequestId(HttpResponse httpResponse) {
        if (httpResponse != null) {
            return getHeaderValue(httpResponse.getFirstHeader("X-Okta-Request-Id"));
        }
        return null;
    }

    private String getOnlySingleHeaderValue(HttpResponse response, String name) {
        Header[] headers = response.getHeaders(name);
        if (headers == null || headers.length != 1) {
            return null;
        }
        return headers[0].getValue();
    }

    private String getHeaderValue(Header header) {
        if (header != null) {
            return header.getValue();
        }
        return null;
    }

    private boolean isRedirect(HttpResponse response) {
        int status = response.getStatusLine().getStatusCode();
        return (status == HttpStatus.SC_MOVED_PERMANENTLY ||
                status == HttpStatus.SC_MOVED_TEMPORARILY ||
                status == HttpStatus.SC_TEMPORARY_REDIRECT) &&
                response.getHeaders("Location") != null &&
                response.getHeaders("Location").length > 0;
    }

    private URI getRedirectUri(HttpResponse httpResponse) {
        Header[] locationHeaders = httpResponse.getHeaders("Location");
        String location = locationHeaders[0].getValue();
        log.debug("Redirecting to: {}", location);
        return URI.create(location);
    }

    /**
     * Exponential sleep on failed request to avoid flooding a service with
     * retries.
     *
     * @param retries           Current retry count.
     */
    private void pauseBeforeRetry(int retries, HttpResponse httpResponse, long timeElapsed) throws RestException {
        long delay = -1;
        long timeElapsedLeft = maxElapsedMillis - timeElapsed;
        if (backoffStrategy != null) {
            delay = Math.min(this.backoffStrategy.getDelayMillis(retries), timeElapsedLeft);
        } else if (httpResponse != null && httpResponse.getStatusLine().getStatusCode() == 429) {
            delay = get429DelayMillis(httpResponse);
            if (!shouldRetry(retries, timeElapsed + delay)) {
                throw new RestException("Cannot retry request, next request will exceed retry configuration.");
            }
            log.debug("429 detected, will retry in {}ms, attempt number: {}", delay, retries);
        }

        // default / fallback strategy
        if (delay < 0) {
            delay = Math.min(getDefaultDelayMillis(retries), timeElapsedLeft);
        }

        log.debug("Retryable condition detected, will retry in {}ms, attempt number: {}", delay, retries);

        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RestException(e.getMessage(), e);
        }
    }

    private long get429DelayMillis(HttpResponse httpResponse) {

        // the time at which the rate limit will reset, specified in UTC epoch time.
        String resetLimit = getOnlySingleHeaderValue(httpResponse,"X-Rate-Limit-Reset");
        if (resetLimit == null || !resetLimit.chars().allMatch(Character::isDigit)) {
            return -1;
        }

        // If the Date header is not set, do not continue
        Date requestDate = dateFromHeader(httpResponse);
        if (requestDate == null) {
            return -1;
        }

        long waitUntil = Long.parseLong(resetLimit) * 1000L;
        long requestTime = requestDate.getTime();
        long delay = waitUntil - requestTime + 1000;
        log.debug("429 wait: {} - {} + {} = {}", waitUntil, requestTime, 1000, delay);

        return delay;
    }

    private Date dateFromHeader(HttpResponse httpResponse) {
        Date result = null;
        Header dateHeader = httpResponse.getFirstHeader("Date");
        if (dateHeader != null) {
            result = DateUtils.parseDate(dateHeader.getValue());
        }
        return result;
    }

    private long getDefaultDelayMillis(int retries) {
        long scaleFactor = 300;
        long result = (long) (Math.pow(2, retries) * scaleFactor);
        return Math.min(result, DEFAULT_MAX_BACKOFF_IN_MILLISECONDS);
    }

    /**
     * Returns true if a failed request should be retried.
     *
     * @param method  The current HTTP method being executed.
     * @param t       The throwable from the failed request.
     * @param retryCount  The number of times the current request has been attempted.
     * @param timeElapsed The time elapsed for this attempt.
     * @return True if the failed request should be retried.
     */
    private boolean shouldRetry(HttpRequestBase method, Throwable t, int retryCount, long timeElapsed) {
        if (!shouldRetry(retryCount, timeElapsed)) {
            return false;
        }

        if (method instanceof HttpEntityEnclosingRequest) {
            HttpEntity entity = ((HttpEntityEnclosingRequest) method).getEntity();
            if (entity != null && !entity.isRepeatable()) {
                return false;
            }
        }

        if (t instanceof NoHttpResponseException ||
                t instanceof SocketException ||
                t instanceof SocketTimeoutException ||
                t instanceof ConnectTimeoutException) {
            log.debug("Retrying on {}: {}", t.getClass().getName(), t.getMessage());
            return true;
        }

        return false;
    }

    private boolean shouldRetry(int retryCount, long timeElapsed) {
               // either maxRetries or maxElapsedMillis is enabled
        return (maxRetries > 0 || maxElapsedMillis > 0)

               // maxRetries count is disabled OR if set check it
               && (maxRetries <= 0 || retryCount <= this.maxRetries)

               // maxElapsedMillis is disabled OR if set check it
               && (maxElapsedMillis <= 0 || timeElapsed < maxElapsedMillis);
    }

    private boolean shouldRetry(Response response, int retryCount, long timeElapsed) {
        int httpStatus = response.getHttpStatus();

        // supported status codes
        return shouldRetry(retryCount, timeElapsed)
            && (httpStatus == 429
             || httpStatus == 503
             || httpStatus == 504);
    }

    protected byte[] toBytes(HttpEntity entity) throws IOException {
        return EntityUtils.toByteArray(entity);
    }

    protected Response toSdkResponse(HttpResponse httpResponse) throws IOException {

        int httpStatus = httpResponse.getStatusLine().getStatusCode();

        HttpHeaders headers = getHeaders(httpResponse);
        MediaType mediaType = headers.getContentType();

        HttpEntity entity = getHttpEntity(httpResponse);

        InputStream body = entity != null ? entity.getContent() : null;
        long contentLength = entity != null ? entity.getContentLength() : -1;

        //ensure that the content has been fully acquired before closing the http stream
        if (body != null) {
            byte[] bytes = toBytes(entity);

            if(bytes != null) {
                body = new ByteArrayInputStream(bytes);
            }  else {
                body = null;
            }
        }

        Response response = new DefaultResponse(httpStatus, mediaType, body, contentLength);

        response.getHeaders().add(HttpHeaders.OKTA_REQUEST_ID, headers.getOktaRequestId());
        response.getHeaders().put(HttpHeaders.LINK, headers.getLinkHeaders());

        return response;
    }

    private HttpEntity getHttpEntity(HttpResponse response) {

        HttpEntity entity = response.getEntity();
        if (entity != null) {
            Header contentEncodingHeader = entity.getContentEncoding();
            if (contentEncodingHeader != null) {
                for (HeaderElement element : contentEncodingHeader.getElements()) {
                    if (element.getName().equalsIgnoreCase("gzip")) {
                        return new GzipDecompressingEntity(response.getEntity());
                    }
                }
            }
        }
        return entity;
    }

    private HttpHeaders getHeaders(HttpResponse response) {

        HttpHeaders headers = new HttpHeaders();
        Header[] httpHeaders = response.getAllHeaders();

        if (httpHeaders != null) {
            for (Header httpHeader : httpHeaders) {
                headers.add(httpHeader.getName(), httpHeader.getValue());
            }
        }

        return headers;
    }
}