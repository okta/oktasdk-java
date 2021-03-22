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
package com.okta.sdk.resource.user;

import com.okta.commons.lang.Classes;
import com.okta.sdk.client.Client;
import com.okta.sdk.resource.user.type.UserType;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public interface UserBuilder {

    static UserBuilder instance() {
        return Classes.newInstance("com.okta.sdk.impl.resource.DefaultUserBuilder");
    }

    UserBuilder setPassword(char[] password);

    UserBuilder usePasswordHookForImport();

    UserBuilder usePasswordHookForImport(String type);

    UserBuilder setSecurityQuestion(String question);

    UserBuilder setSecurityQuestionAnswer(String answer);

    UserBuilder setEmail(String email);

    UserBuilder setFirstName(String firstName);

    UserBuilder setLastName(String lastName);

    UserBuilder setLogin(String login);

    UserBuilder setMobilePhone(String mobilePhone);

    UserBuilder setSecondEmail(String secondEmail);

    UserBuilder setActive(Boolean active);

    UserBuilder setProvider(Boolean provider);

    UserBuilder setType(UserType userType);

    UserBuilder setType(String userTypeId);

    UserBuilder setProfileProperties(Map<String, Object> profileProperties);

    UserBuilder putAllProfileProperties(Map<String, Object> profileProperties);

    UserBuilder putProfileProperty(String key, Object value);

    default UserBuilder setGroups(String... groupIds) {
        return setGroups(Arrays.stream(groupIds).collect(Collectors.toSet()));
    }

    UserBuilder setGroups(Set<String> groupIds);

    UserBuilder addGroup(String groupId);

    UserBuilder setNextLogin(UserNextLogin nextLogin);

    UserBuilder setBcryptPasswordHash(String value, String salt, int workFactor);

    UserBuilder setSha256PasswordHash(String value, String salt, String saltOrder);

    UserBuilder setSha512PasswordHash(String value, String salt, String saltOrder);

    UserBuilder setSha1PasswordHash(String value, String salt, String saltOrder);

    UserBuilder setMd5PasswordHash(String value, String salt, String saltOrder);

    User buildAndCreate(Client client);
}
