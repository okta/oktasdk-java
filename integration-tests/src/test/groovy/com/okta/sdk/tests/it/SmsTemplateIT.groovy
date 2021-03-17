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
package com.okta.sdk.tests.it

import com.okta.sdk.resource.template.SmsTemplateTranslations
import com.okta.sdk.resource.template.SmsTemplate
import com.okta.sdk.resource.template.SmsTemplateType
import com.okta.sdk.tests.it.util.ITSupport
import org.testng.annotations.Test

import static com.okta.sdk.tests.it.util.Util.assertPresent
import static com.okta.sdk.tests.it.util.Util.assertNotPresent
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Tests for {@code /api/v1/templates/sms}.
 * @since 2.0.0
 */
class SmsTemplateIT extends ITSupport {

    @Test
    void customTemplatesCrudTest() {
        def templateName = "template-" + UUID.randomUUID().toString()

        // create translations
        SmsTemplateTranslations smsTemplateTranslations = client.instantiate(SmsTemplateTranslations)
        smsTemplateTranslations.put("de", "\${org.name}: ihre bestätigungscode ist \${code}")
        smsTemplateTranslations.put("it", "\${org.name}: il codice di verifica è \${code}")

        // create template
        SmsTemplate smsTemplate = client.createSmsTemplate(client.instantiate(SmsTemplate)
            .setName(templateName)
            .setType(SmsTemplateType.SMS_VERIFY_CODE)
            .setTemplate("\${org.name}: your verification code is \${code}")
            .setTranslations(smsTemplateTranslations))
        registerForCleanup(smsTemplate)

        assertThat(smsTemplate.getId(), notNullValue())

        // list templates
        assertPresent(client.listSmsTemplates(), smsTemplate)

        // retrieve template
        SmsTemplate retrievedSmsTemplate = client.getSmsTemplate(smsTemplate.getId())
        assertThat(retrievedSmsTemplate, notNullValue())
        assertThat(retrievedSmsTemplate.getTranslations().keySet(), hasSize(2))

        // partial update template with 1 empty translation
        SmsTemplateTranslations partialUpdateTranslations = client.instantiate(SmsTemplateTranslations)
        partialUpdateTranslations.put("de", "")  // supplying empty value here so it gets removed by partial update operation (by design)

        smsTemplate.setTranslations(partialUpdateTranslations)

        smsTemplate.partialUpdate()
        assertThat(smsTemplate.getTranslations().keySet(), hasSize(1))

        // partial update again with 2 new translations
        smsTemplate.getTranslations().put("es", "\${org.name}: su código de inscripción es \${code}")
        smsTemplate.getTranslations().put("fr", "\${org.name}: votre code d'inscription est \${code}",)

        smsTemplate.partialUpdate()
        assertThat(smsTemplate.getTranslations().keySet(), hasSize(3))

        // full update template
        SmsTemplateTranslations fullUpdateTranslations = client.instantiate(SmsTemplateTranslations)
        fullUpdateTranslations.put("de", "\${org.name}: Hier ist Ihr Registrierungscode: \${code}")

        smsTemplate.setName("new-" + templateName)
        smsTemplate.setType(SmsTemplateType.SMS_VERIFY_CODE)
        smsTemplate.setTemplate("\${org.name}: Here is your enrollment code: \${code}")
        smsTemplate.setTranslations(fullUpdateTranslations)

        smsTemplate.update()
        assertThat(smsTemplate.getName(), is("new-" + templateName))
        assertThat(smsTemplate.getTranslations().keySet(), hasSize(1))

        // list templates
        assertPresent(client.listSmsTemplates(), smsTemplate)

        // delete template
        smsTemplate.delete()
        assertNotPresent(client.listSmsTemplates(), smsTemplate)
    }
}
