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
import com.okta.sdk.impl.http.Request
import com.okta.sdk.resource.log.Actor
import com.okta.sdk.resource.log.AuthenticationContext
import com.okta.sdk.resource.log.DebugContext
import com.okta.sdk.resource.log.GeographicalContext
import com.okta.sdk.resource.log.Geolocation
import com.okta.sdk.resource.log.Log
import com.okta.sdk.resource.log.LogSeverity
import com.okta.sdk.resource.log.Outcome
import com.okta.sdk.resource.log.SecurityContext
import com.okta.sdk.resource.log.Transaction
import org.mockito.Mockito
import org.testng.annotations.Test

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.stream.Collectors

import static org.hamcrest.Matchers.*
import static org.hamcrest.MatcherAssert.*

class LogsTest {

    @Test
    void testGetLogs() {

        // Mock the response objects in the client
        MockClient client = new MockClient()
                .withMockResponse(Mockito.any(Request), '/stubs/logs.json')

        // get the list of logs
        List<Log> logs = client.getLogs().stream().collect(Collectors.toList())
        assertThat logs, hasSize(100)
        logs.forEach { assertThat it, instanceOf(Log) }

        // grab the first Log item and validate it
        Log log = logs.get(0)

        Date expectedDate = Date.from(Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse("2017-11-30T21:15:16.838Z")))

        assertThat log.actor, instanceOf(Actor)
        assertThat log.actor.id, equalTo("0000222244448888000")
        assertThat log.actor.type, equalTo("User")
        assertThat log.actor.alternateId, equalTo("joe.coder@example.com")
        assertThat log.actor.displayName, equalTo("Joe Coder")
        assertThat log.actor.detail, nullValue()
        assertThat log.actor.type, equalTo("User")
        assertThat log.actor.type, equalTo("User")

        assertThat log.client, instanceOf(com.okta.sdk.resource.log.Client)
//        assertThat log.client.userAgent, equalTo("User")
        assertThat log.client.zone, equalTo("null")
        assertThat log.client.device, equalTo("Computer")
        assertThat log.client.id, nullValue()
        assertThat log.client.ipAddress, equalTo("66.222.111.88")
        assertThat log.client.geographicalContext, instanceOf(GeographicalContext)
        assertThat log.client.geographicalContext.city, equalTo("Concord")
        assertThat log.client.geographicalContext.state, equalTo("New Hampshire")
        assertThat log.client.geographicalContext.country, equalTo("United States")
        assertThat log.client.geographicalContext.postalCode, equalTo("03303")
        assertThat log.client.geographicalContext.geolocation, instanceOf(Geolocation)
        assertThat log.client.geographicalContext.geolocation.lat, equalTo(43.3091d)
        assertThat log.client.geographicalContext.geolocation.lon, equalTo(-71.6861d)

        assertThat log.authenticationContext, instanceOf(AuthenticationContext)
        assertThat log.authenticationContext.authenticationProvider, nullValue()
        assertThat log.authenticationContext.credentialProvider, nullValue()
        assertThat log.authenticationContext.credentialType, nullValue()
        assertThat log.authenticationContext.issuer, nullValue()
        assertThat log.authenticationContext.interface, nullValue()
//        assertThat log.authenticationContext.authenticationStep, equalTo(0)
        assertThat log.authenticationContext.externalSessionId, equalTo("trs-T02AyaeRDKxyrAUXkV-yg")

        assertThat log.displayMessage, equalTo("Deactivate Okta User")
        assertThat log.eventType, equalTo("user.lifecycle.deactivate")
        assertThat log.published, equalTo(expectedDate)
        assertThat log.severity, equalTo(LogSeverity.INFO)
        assertThat log.uuid, equalTo("0626e18a-3d17-40fb-a1f3-094be1f39208")
        assertThat log.version, equalTo("0")
        assertThat log.legacyEventType, equalTo("core.user.config.user_deactivated")

        assertThat log.outcome, instanceOf(Outcome)
        assertThat log.outcome.result, equalTo("SUCCESS")
        assertThat log.outcome.reason, nullValue()

        assertThat log.securityContext, instanceOf(SecurityContext)
        assertThat log.securityContext.asNumber, nullValue()
        assertThat log.securityContext.asOrg, nullValue()
        assertThat log.securityContext.isp, nullValue()
        assertThat log.securityContext.domain, nullValue()
        assertThat log.securityContext.isProxy, equalTo(false)

        assertThat log.debugContext, instanceOf(DebugContext)
        assertThat log.debugContext.debugData.get("requestUri"), equalTo("/api/v1/users/00ud384zryL1GFAg30h7/lifecycle/deactivate")

        assertThat log.transaction, instanceOf(Transaction)
        assertThat log.transaction.type, equalTo("WEB")
        assertThat log.transaction.id, equalTo("WiB04-V4MgacZHWciQq8YwAADpA")
        assertThat log.transaction.detail, anEmptyMap()

        assertThat log.target, hasSize(1)
        assertThat log.target.get(0).id, equalTo("00ud384zryL1GFAg30h7")
        assertThat log.target.get(0).type, equalTo("User")
        assertThat log.target.get(0).alternateId, equalTo("john-with-group@example.com")
        assertThat log.target.get(0).displayName, equalTo("John With-Group")
        assertThat log.target.get(0).detailEntry, nullValue()
    }
}