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
package com.okta.sdk.tests.it

import com.google.common.collect.Lists;
import com.okta.sdk.client.Client
import com.okta.sdk.resource.ExtensibleResource
import com.okta.sdk.resource.user.User
import com.okta.sdk.resource.user.factor.CallFactor
import com.okta.sdk.resource.user.factor.Factor
import com.okta.sdk.resource.user.factor.FactorList
import com.okta.sdk.resource.user.factor.FactorResultType
import com.okta.sdk.resource.user.factor.FactorStatus
import com.okta.sdk.resource.user.factor.PushFactor
import com.okta.sdk.resource.user.factor.SecurityQuestionFactor
import com.okta.sdk.resource.user.factor.SecurityQuestionList
import com.okta.sdk.resource.user.factor.SmsFactor
import com.okta.sdk.resource.user.factor.TotpFactor
import com.okta.sdk.resource.user.factor.VerifyFactorRequest
import com.okta.sdk.resource.user.factor.VerifyFactorResponse
import com.okta.sdk.tests.it.util.ITSupport
import org.jboss.aerogear.security.otp.Totp
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

import java.util.stream.Collectors

import static org.hamcrest.Matchers.*
import static org.hamcrest.MatcherAssert.assertThat

class FactorsIT extends ITSupport {


    private String smsTestNumber = "162 840 01133"

    @BeforeClass
    void enableOrgFactors() {
        def requiredFactors = ["okta_push", "okta_sms", "okta_call", "okta_otp", "google_otp", "fido_webauthn", "okta_question", "fido_u2f"]
        def orgFactorsUri = "/api/v1/org/factors"

        // get the list of disabled factors
        ExtensibleResource result = getClient().http().get(orgFactorsUri, ExtensibleResource)
        List<String> deactivatedFactorIds = result.get("items").stream()
            .filter { it.id != null }
            .filter { it.status != "ACTIVE" }
            .map { it.id }
            .collect(Collectors.toList())

        // enable any factors that are currently disabled
        requiredFactors.forEach {
            if (deactivatedFactorIds.contains(it)) {
                println "activating: ${it}"
                println getClient().http().post("${orgFactorsUri}/${it}/lifecycle/activate", ExtensibleResource)
            }
        }
    }

    @Test
    void factorListTest() {

        Client client = getClient()
        User user = randomUser()

        assertThat user.listFactors(), emptyIterable()

        SmsFactor smsFactor = client.instantiate(SmsFactor)
        smsFactor.getProfile().phoneNumber = smsTestNumber
        user.addFactor(smsFactor)

        SecurityQuestionFactor securityQuestionFactor = client.instantiate(SecurityQuestionFactor)
        securityQuestionFactor.getProfile()
                .setQuestion("disliked_food")
                .setAnswer("pizza")
        user.addFactor(securityQuestionFactor)

        FactorList factorsList = user.listFactors()
        List<Factor> factorsArrayList = Lists.newArrayList(factorsList)
        assertThat factorsArrayList, allOf(hasSize(2), containsInAnyOrder(
                allOf(
                        instanceOf(SmsFactor),
                        hasProperty("id", is(smsFactor.getId()))),
                allOf(
                        instanceOf(SecurityQuestionFactor),
                        hasProperty("id", is(securityQuestionFactor.getId())))))
    }

    @Test
    void testSecurityQuestionFactorCreation() {
        Client client = getClient()
        User user = randomUser()

        assertThat user.listFactors(), emptyIterable()

        SecurityQuestionFactor securityQuestionFactor = client.instantiate(SecurityQuestionFactor)
        securityQuestionFactor.getProfile()
                .setQuestion("disliked_food")
                .setAnswer("pizza")

        assertThat securityQuestionFactor.id, nullValue()
        assertThat securityQuestionFactor, sameInstance(user.addFactor(securityQuestionFactor))
        assertThat securityQuestionFactor.id, notNullValue()
    }

    @Test
    void testCallFactorCreation() {
        Client client = getClient()
        User user = randomUser()

        assertThat user.listFactors(), emptyIterable()

        CallFactor callFactor = client.instantiate(CallFactor)
        callFactor.getProfile().phoneNumber = smsTestNumber

        assertThat callFactor.id, nullValue()
        assertThat callFactor, sameInstance(user.addFactor(callFactor))
        assertThat callFactor.id, notNullValue()
    }

    @Test
    void testSmsFactorCreation() {
        Client client = getClient()
        User user = randomUser()

        assertThat user.listFactors(), emptyIterable()

        SmsFactor smsFactor = client.instantiate(SmsFactor)
        smsFactor.getProfile().phoneNumber = smsTestNumber

        assertThat smsFactor.id, nullValue()
        assertThat smsFactor, sameInstance(user.addFactor(smsFactor))
        assertThat smsFactor.id, notNullValue()
    }

    @Test
    void testPushFactorCreation() {
        Client client = getClient()
        User user = randomUser()
        assertThat user.listFactors(), emptyIterable()

        PushFactor pushFactor = client.instantiate(PushFactor)
        assertThat pushFactor.id, nullValue()
        assertThat pushFactor, sameInstance(user.addFactor(pushFactor))
        assertThat pushFactor.id, notNullValue()
    }

    @Test
    void testListSecurityQuestionsNotEmpty() {
        User user = randomUser()
        SecurityQuestionList securityQuestions = user.listSupportedSecurityQuestions()
        assertThat securityQuestions, iterableWithSize(greaterThan(1))
    }

    @Test
    void testAvailableFactorsNotEmpty() {
        User user = randomUser()
        FactorList factors = user.listSupportedFactors()
        assertThat factors, iterableWithSize(greaterThan(1))
    }

    @Test
    void activateTotpFactor() {
        User user = randomUser()
        assertThat user.listFactors(), emptyIterable()
        TotpFactor totpFactor = client.instantiate(TotpFactor)
        user.addFactor(totpFactor)

        assertThat totpFactor.getStatus(), is(FactorStatus.PENDING_ACTIVATION)
        Totp totp = new Totp(totpFactor.getEmbedded().get("activation").get("sharedSecret"))

        VerifyFactorRequest verifyFactorRequest = client.instantiate(VerifyFactorRequest)
        verifyFactorRequest.passCode = totp.now()
        Factor factorResult = totpFactor.activate(verifyFactorRequest)
        assertThat factorResult.getStatus(), is(FactorStatus.ACTIVE)
        assertThat factorResult, instanceOf(TotpFactor)
    }

    @Test
    void verifyQuestionFactor() {
        User user = randomUser()

        SecurityQuestionFactor securityQuestionFactor = client.instantiate(SecurityQuestionFactor)
        securityQuestionFactor.getProfile()
                .setQuestion("disliked_food")
                .setAnswer("pizza")
        user.addFactor(securityQuestionFactor)

        VerifyFactorRequest request = client.instantiate(VerifyFactorRequest)
        request.answer = "pizza"
        VerifyFactorResponse response = securityQuestionFactor.verify(request)
        assertThat response.getFactorResult(), is(FactorResultType.SUCCESS)
    }

    @Test
    void deleteFactorTest() {

        User user = randomUser()
        assertThat user.listFactors(), emptyIterable()
        TotpFactor totpFactor = client.instantiate(TotpFactor)
        totpFactor.provider = "OKTA"
        user.addFactor(totpFactor)
        totpFactor.delete()
    }
}
