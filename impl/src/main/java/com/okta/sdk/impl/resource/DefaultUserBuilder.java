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
package com.okta.sdk.impl.resource;

import com.okta.sdk.client.Client;
import com.okta.commons.lang.Collections;
import com.okta.commons.lang.Strings;
import com.okta.sdk.resource.user.PasswordCredentialHook;
import com.okta.sdk.resource.user.RecoveryQuestionCredential;
import com.okta.sdk.resource.user.UserBuilder;
import com.okta.sdk.resource.user.PasswordCredential;
import com.okta.sdk.resource.user.User;
import com.okta.sdk.resource.user.UserCredentials;
import com.okta.sdk.resource.user.UserNextLogin;
import com.okta.sdk.resource.user.UserProfile;
import com.okta.sdk.resource.user.type.UserType;
import com.okta.sdk.resource.user.CreateUserRequest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class DefaultUserBuilder implements UserBuilder {

    private char[] password;
    private String securityQuestion;
    private String securityQuestionAnswer;
    private String email;
    private String login;
    private String secondEmail;
    private String firstName;
    private String lastName;
    private String mobilePhone;
    private Boolean active;
    private Boolean provider;
    private UserType userType;
    private String userTypeId;
    private UserNextLogin nextLogin;
    private Set<String> groupIds = new HashSet<>();
    private Map<String, Object> passwordHashProperties;
    private String passwordHookImportType;

    private Map<String, Object> customProfileAttributes = new LinkedHashMap<>();

    public UserBuilder setPassword(char[] password) {
        this.password = Arrays.copyOf(password, password.length);
        return this;
    }

    public UserBuilder usePasswordHookForImport() {
        return usePasswordHookForImport("default");
    }

    public UserBuilder usePasswordHookForImport(String type) {
        passwordHookImportType = type;
        return this;
    }

    public UserBuilder setSecurityQuestion(String securityQuestion) {
        this.securityQuestion = securityQuestion;
        return this;
    }

    public UserBuilder setSecurityQuestionAnswer(String answer) {
        this.securityQuestionAnswer = answer;
        return this;
    }

    public UserBuilder setEmail(String email) {
        this.email = email;
        return this;
    }

    public UserBuilder setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public UserBuilder setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public UserBuilder setLogin(String login) {
        this.login = login;
        return this;
    }

    public UserBuilder setMobilePhone(String mobilePhone) {
        this.mobilePhone = mobilePhone;
        return this;
    }

    public UserBuilder setSecondEmail(String secondEmail) {
        this.secondEmail = secondEmail;
        return this;
    }

    public UserBuilder setActive(Boolean active) {
        this.active = active;
        return this;
    }

    public UserBuilder setProvider(Boolean provider) {
        this.provider = provider;
        return this;
    }

    @Override
    public UserBuilder setType(UserType userType) {
        this.userType = userType;
        return this;
    }

    @Override
    public UserBuilder setType(String userTypeId) {
        this.userTypeId = userTypeId;
        return this;
    }

    public UserBuilder setProfileProperties(Map<String, Object> profileProperties) {

        this.customProfileAttributes.clear();
        return putAllProfileProperties(profileProperties);
    }

    public UserBuilder putAllProfileProperties(Map<String, Object> profileProperties) {

        this.customProfileAttributes.putAll(profileProperties);
        return this;
    }

    public UserBuilder putProfileProperty(String key, Object value) {
        this.customProfileAttributes.put(key, value);
        return this;
    }

    public UserBuilder setGroups(Set<String> groupIds) {
        this.groupIds = groupIds;
        return this;
    }

    public UserBuilder addGroup(String groupId) {
        this.groupIds.add(groupId);
        return this;
    }

    @Override
    public UserBuilder setNextLogin(UserNextLogin nextLogin) {
        this.nextLogin = nextLogin;
        return this;
    }

    private CreateUserRequest build(Client client) {

        CreateUserRequest createUserRequest = client.instantiate(CreateUserRequest.class);
        createUserRequest.setProfile(client.instantiate(UserProfile.class));
        UserProfile userProfile = createUserRequest.getProfile();
        if (Strings.hasText(firstName)) userProfile.setFirstName(firstName);
        if (Strings.hasText(lastName)) userProfile.setLastName(lastName);
        if (Strings.hasText(email)) userProfile.setEmail(email);
        if (Strings.hasText(secondEmail)) userProfile.setSecondEmail(secondEmail);
        if (Strings.hasText(mobilePhone)) userProfile.setMobilePhone(mobilePhone);

        if (Strings.hasText(login)) {
            userProfile.setLogin(login);
        }
        else {
            userProfile.setLogin(email);
        }

        if (Strings.hasText(userTypeId)) {
            createUserRequest.setType(client.instantiate(UserType.class).setId(userTypeId));
        }
        else if (userType != null) {
            createUserRequest.setType(userType);
        }

        if (!Collections.isEmpty(groupIds)) {
            if (createUserRequest instanceof AbstractResource) {
                ((AbstractResource) createUserRequest).setProperty("groupIds", groupIds, true);
            } else {
                throw new IllegalArgumentException("'User' is not an instance of 'AbstractResource', so 'groupIds' cannot be set. This would only happen if the implementation of 'User' has been customized.");
            }
        }

        userProfile.putAll(customProfileAttributes);

        // security question
        if (Strings.hasText(securityQuestion)) {
            RecoveryQuestionCredential question = client.instantiate(RecoveryQuestionCredential.class);
            question.setQuestion(securityQuestion);
            question.setAnswer(securityQuestionAnswer);
            createCredentialsIfNeeded(createUserRequest, client).setRecoveryQuestion(question);
        }

        // user password
        if (password != null && password.length > 0) {

            if (passwordHashProperties != null) {
                throw new IllegalArgumentException("Cannot specify both password and password hash, use one or the other.");
            }

            PasswordCredential passwordCredential = client.instantiate(PasswordCredential.class);
            createCredentialsIfNeeded(createUserRequest, client).setPassword(passwordCredential.setValue(password));
        }

        // direct password import
        if (passwordHashProperties != null) {
            PasswordCredential passwordCredential = client.instantiate(PasswordCredential.class);
            passwordCredential.put("hash", passwordHashProperties);
            createCredentialsIfNeeded(createUserRequest, client).setPassword(passwordCredential);
        }

        // password hook import
        if (passwordHookImportType != null) {
            PasswordCredential passwordCredential = client.instantiate(PasswordCredential.class);
            PasswordCredentialHook passwordCredentialHook = client.instantiate(PasswordCredentialHook.class);
            passwordCredentialHook.setType(passwordHookImportType);
            passwordCredential.setHook(passwordCredentialHook);
            createCredentialsIfNeeded(createUserRequest, client).setPassword(passwordCredential);
        }

        return createUserRequest;
    }

    private UserCredentials createCredentialsIfNeeded(CreateUserRequest createUserRequest, Client client) {
        if (createUserRequest.getCredentials() == null) {
            UserCredentials credentials = client.instantiate(UserCredentials.class);
            createUserRequest.setCredentials(credentials);
        }
        return createUserRequest.getCredentials();
    }

    public UserBuilder setBcryptPasswordHash(String value, String salt, int workFactor) {
        passwordHashProperties = new HashMap<>();
        passwordHashProperties.put("algorithm", "BCRYPT");
        passwordHashProperties.put("workFactor", workFactor);
        passwordHashProperties.put("salt", salt);
        passwordHashProperties.put("value", value);
        return this;
    }

    public UserBuilder setSha256PasswordHash(String value, String salt, String saltOrder) {
        return setPasswordHash("SHA-256", value, salt, saltOrder);
    }

    public UserBuilder setSha512PasswordHash(String value, String salt, String saltOrder) {
        return setPasswordHash("SHA-512", value, salt, saltOrder);
    }

    public UserBuilder setSha1PasswordHash(String value, String salt, String saltOrder) {
        return setPasswordHash("SHA-1", value, salt, saltOrder);
    }

    public UserBuilder setMd5PasswordHash(String value, String salt, String saltOrder) {
        return setPasswordHash("MD5", value, salt, saltOrder);
    }

    private UserBuilder setPasswordHash(String shaAlgorithm, String value, String salt, String saltOrder) {
        passwordHashProperties = new HashMap<>();
        passwordHashProperties.put("algorithm", shaAlgorithm);
        passwordHashProperties.put("salt", salt);
        passwordHashProperties.put("value", value);
        passwordHashProperties.put("saltOrder", saltOrder);
        return this;
    }

    @Override
    public User buildAndCreate(Client client) {
        return client.createUser(build(client), active, provider, nextLogin);
    }
}