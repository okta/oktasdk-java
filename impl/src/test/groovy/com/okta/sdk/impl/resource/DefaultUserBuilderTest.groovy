/*
 * Copyright 2018-Present Okta, Inc.
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
package com.okta.sdk.impl.resource

import com.okta.sdk.client.Client
import com.okta.sdk.resource.user.CreateUserRequest
import com.okta.sdk.resource.user.PasswordCredential
import com.okta.sdk.resource.user.PasswordCredentialHook
import com.okta.sdk.resource.user.UserCredentials
import com.okta.sdk.resource.user.UserNextLogin
import com.okta.sdk.resource.user.UserProfile
import org.mockito.ArgumentCaptor
import org.testng.annotations.Test

import static com.okta.sdk.impl.Util.expect
import static org.hamcrest.Matchers.aMapWithSize
import static org.hamcrest.Matchers.allOf
import static org.hamcrest.Matchers.hasEntry
import static org.hamcrest.MatcherAssert.assertThat
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

class DefaultUserBuilderTest {

    @Test
    void basicUsage() {

        def client = mock(Client)
        def createUserRequest = mock(CreateUserRequest)
        def profile = mock(UserProfile)
        when(client.instantiate(CreateUserRequest)).thenReturn(createUserRequest)
        when(client.instantiate(UserProfile)).thenReturn(profile)
        when(createUserRequest.getProfile()).thenReturn(profile)

        new DefaultUserBuilder()
            .setFirstName("Joe")
            .setLastName("Coder")
            .setEmail("joe.coder@example.com")
            .setNextLogin(UserNextLogin.CHANGEPASSWORD)
            .buildAndCreate(client)

        verify(client).createUser(eq(createUserRequest), eq(null), eq(null), eq(UserNextLogin.CHANGEPASSWORD))
        verify(profile).setFirstName("Joe")
        verify(profile).setLastName("Coder")
        verify(profile).setEmail("joe.coder@example.com")
    }

    @Test
    void importPasswordSha256() {
        def client = mock(Client)
        def createUserRequest = mock(CreateUserRequest)
        def profile = mock(UserProfile)
        def passwordCredential = mock(PasswordCredential)
        def userCredentials = mock(UserCredentials)
        when(client.instantiate(CreateUserRequest)).thenReturn(createUserRequest)
        when(client.instantiate(UserProfile)).thenReturn(profile)
        when(client.instantiate(UserCredentials)).thenReturn(userCredentials)
        when(client.instantiate(PasswordCredential)).thenReturn(passwordCredential)
        when(createUserRequest.getProfile()).thenReturn(profile)
        when(createUserRequest.getCredentials()).thenReturn(userCredentials)

        String salt = "some-salt"
        String hashedPassword = "a-hashed-password"

        new DefaultUserBuilder()
            .setFirstName("Joe")
            .setLastName("Coder")
            .setEmail("joe.coder@example.com")
            .setSha256PasswordHash(hashedPassword, salt, "PREFIX")
            .buildAndCreate(client)

        def hashCapture = ArgumentCaptor.forClass(Map.class)

        verify(userCredentials).setPassword(passwordCredential)
        verify(passwordCredential).put(eq("hash"), hashCapture.capture())

        assertThat hashCapture.value, allOf(
                    hasEntry("salt", salt),
                    hasEntry("saltOrder", "PREFIX"),
                    hasEntry("value", hashedPassword),
                    hasEntry("algorithm", "SHA-256"),
                    aMapWithSize(4))
    }

    @Test
    void importPasswordSha512() {
        def client = mock(Client)
        def createUserRequest = mock(CreateUserRequest)
        def profile = mock(UserProfile)
        def passwordCredential = mock(PasswordCredential)
        def userCredentials = mock(UserCredentials)
        when(client.instantiate(CreateUserRequest)).thenReturn(createUserRequest)
        when(client.instantiate(UserProfile)).thenReturn(profile)
        when(client.instantiate(UserCredentials)).thenReturn(userCredentials)
        when(client.instantiate(PasswordCredential)).thenReturn(passwordCredential)
        when(createUserRequest.getProfile()).thenReturn(profile)
        when(createUserRequest.getCredentials()).thenReturn(userCredentials)

        String salt = "some-salt"
        String hashedPassword = "a-hashed-password"

        new DefaultUserBuilder()
            .setFirstName("Joe")
            .setLastName("Coder")
            .setEmail("joe.coder@example.com")
            .setSha512PasswordHash(hashedPassword, salt, "PREFIX")
            .buildAndCreate(client)

        def hashCapture = ArgumentCaptor.forClass(Map.class)

        verify(userCredentials).setPassword(passwordCredential)
        verify(passwordCredential).put(eq("hash"), hashCapture.capture())

        assertThat hashCapture.value, allOf(
                    hasEntry("salt", salt),
                    hasEntry("saltOrder", "PREFIX"),
                    hasEntry("value", hashedPassword),
                    hasEntry("algorithm", "SHA-512"),
                    aMapWithSize(4))
    }

    @Test
    void importPasswordSha1() {
        def client = mock(Client)
        def createUserRequest = mock(CreateUserRequest)
        def profile = mock(UserProfile)
        def passwordCredential = mock(PasswordCredential)
        def userCredentials = mock(UserCredentials)
        when(client.instantiate(CreateUserRequest)).thenReturn(createUserRequest)
        when(client.instantiate(UserProfile)).thenReturn(profile)
        when(client.instantiate(UserCredentials)).thenReturn(userCredentials)
        when(client.instantiate(PasswordCredential)).thenReturn(passwordCredential)
        when(createUserRequest.getProfile()).thenReturn(profile)
        when(createUserRequest.getCredentials()).thenReturn(userCredentials)

        String salt = "some-salt"
        String hashedPassword = "a-hashed-password"

        new DefaultUserBuilder()
            .setFirstName("Joe")
            .setLastName("Coder")
            .setEmail("joe.coder@example.com")
            .setSha1PasswordHash(hashedPassword, salt, "PREFIX")
            .buildAndCreate(client)

        def hashCapture = ArgumentCaptor.forClass(Map.class)

        verify(userCredentials).setPassword(passwordCredential)
        verify(passwordCredential).put(eq("hash"), hashCapture.capture())

        assertThat hashCapture.value, allOf(
            hasEntry("salt", salt),
            hasEntry("saltOrder", "PREFIX"),
            hasEntry("value", hashedPassword),
            hasEntry("algorithm", "SHA-1"),
            aMapWithSize(4))
    }

    @Test
    void importPasswordBcrypt() {
        def client = mock(Client)
        def createUserRequest = mock(CreateUserRequest)
        def profile = mock(UserProfile)
        def passwordCredential = mock(PasswordCredential)
        def userCredentials = mock(UserCredentials)
        when(client.instantiate(CreateUserRequest)).thenReturn(createUserRequest)
        when(client.instantiate(UserProfile)).thenReturn(profile)
        when(client.instantiate(UserCredentials)).thenReturn(userCredentials)
        when(client.instantiate(PasswordCredential)).thenReturn(passwordCredential)
        when(createUserRequest.getProfile()).thenReturn(profile)
        when(createUserRequest.getCredentials()).thenReturn(userCredentials)

        String salt = "some-salt"
        String hashedPassword = "a-hashed-password"

        new DefaultUserBuilder()
            .setFirstName("Joe")
            .setLastName("Coder")
            .setEmail("joe.coder@example.com")
            .setBcryptPasswordHash(hashedPassword, salt, 10)
            .buildAndCreate(client)

        def hashCapture = ArgumentCaptor.forClass(Map.class)

        verify(userCredentials).setPassword(passwordCredential)
        verify(passwordCredential).put(eq("hash"), hashCapture.capture())

        assertThat hashCapture.value, allOf(
                    hasEntry("salt", salt),
                    hasEntry("workFactor", 10),
                    hasEntry("value", hashedPassword),
                    hasEntry("algorithm", "BCRYPT"),
                    aMapWithSize(4))
    }

    @Test
    void importPasswordMd5() {
        def client = mock(Client)
        def createUserRequest = mock(CreateUserRequest)
        def profile = mock(UserProfile)
        def passwordCredential = mock(PasswordCredential)
        def userCredentials = mock(UserCredentials)
        when(client.instantiate(CreateUserRequest)).thenReturn(createUserRequest)
        when(client.instantiate(UserProfile)).thenReturn(profile)
        when(client.instantiate(UserCredentials)).thenReturn(userCredentials)
        when(client.instantiate(PasswordCredential)).thenReturn(passwordCredential)
        when(createUserRequest.getProfile()).thenReturn(profile)
        when(createUserRequest.getCredentials()).thenReturn(userCredentials)

        String salt = "some-salt"
        String hashedPassword = "a-hashed-password"

        new DefaultUserBuilder()
            .setFirstName("Joe")
            .setLastName("Coder")
            .setEmail("joe.coder@example.com")
            .setMd5PasswordHash(hashedPassword, salt, "PREFIX")
            .buildAndCreate(client)

        def hashCapture = ArgumentCaptor.forClass(Map.class)

        verify(userCredentials).setPassword(passwordCredential)
        verify(passwordCredential).put(eq("hash"), hashCapture.capture())

        assertThat hashCapture.value, allOf(
            hasEntry("salt", salt),
            hasEntry("saltOrder", "PREFIX"),
            hasEntry("value", hashedPassword),
            hasEntry("algorithm", "MD5"),
            aMapWithSize(4))
    }

    @Test
    void createUserWithClearAndImportPassword() {

        def client = mock(Client)
        def createUserRequest = mock(CreateUserRequest)
        def profile = mock(UserProfile)
        def passwordCredential = mock(PasswordCredential)
        def userCredentials = mock(UserCredentials)
        when(client.instantiate(CreateUserRequest)).thenReturn(createUserRequest)
        when(client.instantiate(UserProfile)).thenReturn(profile)
        when(client.instantiate(UserCredentials)).thenReturn(userCredentials)
        when(client.instantiate(PasswordCredential)).thenReturn(passwordCredential)
        when(createUserRequest.getProfile()).thenReturn(profile)
        when(createUserRequest.getCredentials()).thenReturn(userCredentials)

        String salt = "some-salt"
        String hashedPassword = "a-hashed-password"
        String password = "regularPassowrd"

        expect IllegalArgumentException, {
            new DefaultUserBuilder()
                .setFirstName("Joe")
                .setLastName("Coder")
                .setEmail("joe.coder@example.com")
                .setPassword(password.toCharArray())
                .setSha512PasswordHash(hashedPassword, salt, "PREFIX")
                .buildAndCreate(client)
        }
    }

    @Test
    void createUserWithUsePasswordHookForImportDefaultType() {
        createUserWithUsePasswordHookForImport(null)
    }

    @Test
    void createUserWithUsePasswordHookForImportOtherType() {
        createUserWithUsePasswordHookForImport("other")
    }

    void createUserWithUsePasswordHookForImport(String type) {

        def client = mock(Client)
        def createUserRequest = mock(CreateUserRequest)
        def profile = mock(UserProfile)
        def passwordCredential = mock(PasswordCredential)
        def passwordCredentialHook = mock(PasswordCredentialHook)
        def userCredentials = mock(UserCredentials)
        when(client.instantiate(CreateUserRequest)).thenReturn(createUserRequest)
        when(client.instantiate(UserProfile)).thenReturn(profile)
        when(client.instantiate(UserCredentials)).thenReturn(userCredentials)
        when(client.instantiate(PasswordCredential)).thenReturn(passwordCredential)
        when(client.instantiate(PasswordCredentialHook)).thenReturn(passwordCredentialHook)
        when(createUserRequest.getProfile()).thenReturn(profile)
        when(createUserRequest.getCredentials()).thenReturn(userCredentials)

        DefaultUserBuilder defaultUserBuilder = new DefaultUserBuilder()
            .setFirstName("Joe")
            .setLastName("Coder")
            .setEmail("joe.coder@example.com")

        if (type == null) {
            defaultUserBuilder.usePasswordHookForImport()
        } else {
            defaultUserBuilder.usePasswordHookForImport(type)
        }

        defaultUserBuilder.buildAndCreate(client)

        verify(userCredentials).setPassword(passwordCredential)
        verify(passwordCredential).setHook(passwordCredentialHook)

        if (type == null) {
            verify(passwordCredentialHook).setType("default")
        } else {
            verify(passwordCredentialHook).setType(type)
        }
    }
}
