/*
 * Copyright 2017 Okta
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
package com.okta.sdk.impl.resource.log

import com.okta.sdk.impl.client.MockClient
import com.okta.commons.http.Request
import com.okta.sdk.resource.log.LogActor
import com.okta.sdk.resource.log.LogAuthenticationContext
import com.okta.sdk.resource.log.LogClient
import com.okta.sdk.resource.log.LogDebugContext
import com.okta.sdk.resource.log.LogEvent
import com.okta.sdk.resource.log.LogGeographicalContext
import com.okta.sdk.resource.log.LogGeolocation
import com.okta.sdk.resource.log.LogIpAddress
import com.okta.sdk.resource.log.LogOutcome
import com.okta.sdk.resource.log.LogRequest
import com.okta.sdk.resource.log.LogSecurityContext
import com.okta.sdk.resource.log.LogSeverity
import com.okta.sdk.resource.log.LogTransaction
import com.okta.sdk.resource.log.LogUserAgent
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import static org.mockito.Mockito.verify

import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.stream.Collectors

import static org.hamcrest.Matchers.*
import static org.hamcrest.MatcherAssert.*

class LogsTest {
    MockClient client

    @BeforeMethod
    void setup() {
        // mock the response objects in the client
        client = new MockClient()
            .withMockResponse(Mockito.any(Request), '/stubs/logs.json')
    }

    @Test
    void testGetLogs() {

        // get the list of logs
        List<LogEvent> logs = client.getLogs().stream().collect(Collectors.toList())
        assertThat logs, hasSize(100)
        logs.forEach { assertThat it, instanceOf(LogEvent) }

        // grab the first Log item and validate it
        LogEvent log = logs.get(0)

        Date expectedDate = Date.from(Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse("2017-11-30T21:15:16.838Z")))

        assertThat log.getActor(), instanceOf(LogActor)
        assertThat log.getActor().id, equalTo("0000222244448888000")
        assertThat log.getActor().type, equalTo("User")
        assertThat log.getActor().alternateId, equalTo("joe.coder@example.com")
        assertThat log.getActor().displayName, equalTo("Joe Coder")
        assertThat log.getActor().detail, nullValue()
        assertThat log.getActor().type, equalTo("User")
        assertThat log.getActor().type, equalTo("User")

        assertThat log.getClient(), instanceOf(LogClient)
        assertThat log.getClient().getUserAgent(), instanceOf(LogUserAgent)
        assertThat log.getClient().getUserAgent().rawUserAgent, equalTo("okta-sdk-java/0.9.0-SNAPSHOT okta-sdk-java/0.9.0-SNAPSHOT jetty/9.2.22.v20170606 java/1.8.0_121 Mac OS X/10.13.1")
        assertThat log.getClient().getUserAgent().os, equalTo( "Mac OS X")
        assertThat log.getClient().getUserAgent().browser, equalTo( "UNKNOWN")

        assertThat log.getClient().zone, equalTo("null")
        assertThat log.getClient().device, equalTo("Computer")
        assertThat log.getClient().id, nullValue()
        assertThat log.getClient().ipAddress, equalTo("66.222.111.88")
        assertThat log.getClient().getGeographicalContext(), instanceOf(LogGeographicalContext)
        assertThat log.getClient().getGeographicalContext().city, equalTo("Concord")
        assertThat log.getClient().getGeographicalContext().state, equalTo("New Hampshire")
        assertThat log.getClient().getGeographicalContext().country, equalTo("United States")
        assertThat log.getClient().getGeographicalContext().postalCode, equalTo("03303")
        assertThat log.getClient().getGeographicalContext().getGeolocation(), instanceOf(LogGeolocation)
        assertThat log.getClient().getGeographicalContext().getGeolocation().lat, equalTo(43.3091d)
        assertThat log.getClient().getGeographicalContext().getGeolocation().lon, equalTo(-71.6861d)

        assertThat log.getAuthenticationContext(), instanceOf(LogAuthenticationContext)
        assertThat log.getAuthenticationContext().authenticationProvider, nullValue()
        assertThat log.getAuthenticationContext().credentialProvider, nullValue()
        assertThat log.getAuthenticationContext().credentialType, nullValue()
        assertThat log.getAuthenticationContext().issuer, nullValue()
        assertThat log.getAuthenticationContext().interface, nullValue()
//        assertThat log.getAuthenticationContext().authenticationStep, equalTo(0)
        assertThat log.getAuthenticationContext().externalSessionId, equalTo("trs-T02AyaeRDKxyrAUXkV-yg")

        assertThat log.displayMessage, equalTo("Deactivate Okta User")
        assertThat log.eventType, equalTo("user.lifecycle.deactivate")
        assertThat log.getPublished(), equalTo(expectedDate)
        assertThat log.getSeverity(), equalTo(LogSeverity.INFO)
        assertThat log.uuid, equalTo("0626e18a-3d17-40fb-a1f3-094be1f39208")
        assertThat log.version, equalTo("0")
        assertThat log.legacyEventType, equalTo("core.user.config.user_deactivated")

        assertThat log.getOutcome(), instanceOf(LogOutcome)
        assertThat log.getOutcome().result, equalTo("SUCCESS")
        assertThat log.getOutcome().reason, nullValue()

        assertThat log.getSecurityContext(), instanceOf(LogSecurityContext)
        assertThat log.getSecurityContext().asNumber, nullValue()
        assertThat log.getSecurityContext().asOrg, nullValue()
        assertThat log.getSecurityContext().isp, nullValue()
        assertThat log.getSecurityContext().domain, nullValue()
        assertThat log.getSecurityContext().IsProxy(), equalTo(false)

        assertThat log.getDebugContext(), instanceOf(LogDebugContext)
        assertThat log.getDebugContext().getDebugData().get("requestUri"), equalTo("/api/v1/users/00ud384zryL1GFAg30h7/lifecycle/deactivate")

        assertThat log.getTransaction(), instanceOf(LogTransaction)
        assertThat log.getTransaction().type, equalTo("WEB")
        assertThat log.getTransaction().id, equalTo("WiB04-V4MgacZHWciQq8YwAADpA")
        assertThat log.getTransaction().detail, anEmptyMap()

        assertThat log.getTarget(), hasSize(1)
        assertThat log.getTarget().get(0).id, equalTo("00ud384zryL1GFAg30h7")
        assertThat log.getTarget().get(0).type, equalTo("User")
        assertThat log.getTarget().get(0).alternateId, equalTo("john-with-group@example.com")
        assertThat log.getTarget().get(0).displayName, equalTo("John With-Group")
        assertThat log.getTarget().get(0).detailEntry, nullValue()

        assertThat log.getRequest(), instanceOf(LogRequest)
        assertThat log.getRequest().getIpChain().get(0), instanceOf(LogIpAddress)
        assertThat log.getRequest().getIpChain().get(0).ip, equalTo("66.222.111.88")
        assertThat log.getRequest().getIpChain().get(0).version, equalTo("V4")
        assertThat log.getRequest().getIpChain().get(0).source, nullValue()
        assertThat log.getRequest().getIpChain().get(0).getGeographicalContext(), instanceOf(LogGeographicalContext)
        assertThat log.getRequest().getIpChain().get(0).getGeographicalContext().city, equalTo("Concord")
        assertThat log.getRequest().getIpChain().get(0).getGeographicalContext().state, equalTo("New Hampshire")
        assertThat log.getRequest().getIpChain().get(0).getGeographicalContext().country, equalTo("United States")
        assertThat log.getRequest().getIpChain().get(0).getGeographicalContext().postalCode, equalTo("03303")
        assertThat log.getRequest().getIpChain().get(0).getGeographicalContext().getGeolocation(), instanceOf(LogGeolocation)
        assertThat log.getRequest().getIpChain().get(0).getGeographicalContext().getGeolocation().lat, equalTo(43.3091d)
        assertThat log.getRequest().getIpChain().get(0).getGeographicalContext().getGeolocation().lon, equalTo(-71.6861d)
    }

    @Test
    void testGetLogsBetweenDates() {

        Date sinceDate = Date.from(Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse("2017-11-30T21:15:16.838Z")))
        Date untilDate = Date.from(Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse("2017-11-30T21:16:00.081Z")))
        
        // get log events between dates
        List<LogEvent> logs = client.getLogs(sinceDate, untilDate, null, null, null).stream().collect(Collectors.toList())
        logs.forEach { assertThat it, instanceOf(LogEvent) }

        ArgumentCaptor<Request> argument = ArgumentCaptor.forClass(Request.class)
        verify(client.mockRequestExecutor).executeRequest(argument.capture())
        assertThat argument.getValue().queryString.since, equalTo("2017-11-30T21:15:16.838Z")
        assertThat argument.getValue().queryString.until, equalTo("2017-11-30T21:16:00.081Z")
    }

    @Test
    void testGetLogsSinceDate() {

        Date sinceDate = Date.from(Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse("2017-11-30T21:15:16.838Z")))

        // get log events since given date
        List<LogEvent> logs = client.getLogs(sinceDate, null, null, null, null).stream().collect(Collectors.toList())
        assertThat logs, hasSize(100)
        logs.forEach { assertThat it, instanceOf(LogEvent) }

        ArgumentCaptor<Request> argument = ArgumentCaptor.forClass(Request.class)
        verify(client.mockRequestExecutor).executeRequest(argument.capture())
        assertThat argument.getValue().queryString.since, equalTo("2017-11-30T21:15:16.838Z")
    }

    @Test
    void testGetLogsUntilDate() {

        Date untilDate = Date.from(Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse("2017-11-30T21:16:00.081Z")))

        // get log events until given date
        List<LogEvent> logs = client.getLogs(null, untilDate, null, null, null).stream().collect(Collectors.toList())
        assertThat logs, hasSize(100)
        logs.forEach { assertThat it, instanceOf(LogEvent) }

        ArgumentCaptor<Request> argument = ArgumentCaptor.forClass(Request.class)
        verify(client.mockRequestExecutor).executeRequest(argument.capture())
        assertThat argument.getValue().queryString.until, equalTo("2017-11-30T21:16:00.081Z")
    }
}
