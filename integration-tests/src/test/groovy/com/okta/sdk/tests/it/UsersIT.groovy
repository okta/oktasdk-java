/*
 * Copyright 2017-Present Okta, Inc.
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

import com.okta.sdk.client.Client
import com.okta.sdk.resource.ExtensibleResource
import com.okta.sdk.resource.Resource
import com.okta.sdk.resource.ResourceException
import com.okta.sdk.resource.group.Group
import com.okta.sdk.resource.group.GroupBuilder
import com.okta.sdk.resource.policy.PasswordPolicyPasswordSettings
import com.okta.sdk.resource.policy.PasswordPolicyPasswordSettingsAge
import com.okta.sdk.resource.policy.PasswordPolicyRule
import com.okta.sdk.resource.policy.PasswordPolicyRuleAction
import com.okta.sdk.resource.policy.PasswordPolicyRuleActions
import com.okta.sdk.resource.policy.PasswordPolicyRuleConditions
import com.okta.sdk.resource.policy.PasswordPolicySettings
import com.okta.sdk.resource.policy.PolicyNetworkCondition
import com.okta.sdk.resource.role.AssignRoleRequest
import com.okta.sdk.resource.role.RoleType
import com.okta.sdk.resource.user.AuthenticationProviderType
import com.okta.sdk.resource.user.ChangePasswordRequest
import com.okta.sdk.resource.user.PasswordCredential
import com.okta.sdk.resource.user.PasswordCredentialHashAlgorithm
import com.okta.sdk.resource.user.RecoveryQuestionCredential
import com.okta.sdk.resource.user.ResetPasswordToken
import com.okta.sdk.resource.user.Role
import com.okta.sdk.resource.user.User
import com.okta.sdk.resource.user.UserBuilder
import com.okta.sdk.resource.user.UserCredentials
import com.okta.sdk.resource.user.UserList
import com.okta.sdk.resource.user.UserStatus
import com.okta.sdk.resource.user.type.UserType
import com.okta.sdk.tests.Scenario
import com.okta.sdk.tests.it.util.ITSupport
import org.testng.Assert
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import wiremock.org.apache.commons.lang3.RandomStringUtils

import java.time.Duration
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.stream.Collectors

import static com.okta.sdk.tests.it.util.Util.assertGroupTargetPresent
import static com.okta.sdk.tests.it.util.Util.assertNotPresent
import static com.okta.sdk.tests.it.util.Util.assertPresent
import static com.okta.sdk.tests.it.util.Util.expect
import static com.okta.sdk.tests.it.util.Util.validateGroup
import static com.okta.sdk.tests.it.util.Util.validateUser
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Tests for {@code /api/v1/users}.
 * @since 0.5.0
 */
class UsersIT extends ITSupport implements CrudTestSupport {

    @BeforeClass
    void initCustomProperties() {
        ensureCustomProperties()
    }

    @Test
    void userProfileNumberValues() {

        def email = "joe.coder+${uniqueTestName}@example.com"
        User user = UserBuilder.instance()
                .setEmail(email)
                .setFirstName("Joe")
                .setLastName("Code")
                .setPassword("Password1".toCharArray())
                .setSecurityQuestion("Favorite security question?")
                .setSecurityQuestionAnswer("None of them!")
                .putAllProfileProperties([
                    customNumber: 1.5d,
                    customBoolean: true,
                    customInteger: 123,
                    customStringArray: ["one", "two", "three"],
                    customNumberArray: [1.5d, 2.5d, 3.5d],
                    customIntegerArray: [1, 2, 3]
                ])
                .buildAndCreate(getClient())
        registerForCleanup(user)

        // check the values after create
        assertThat(user.getProfile().getString("firstName"), equalTo("Joe"))
        assertThat(user.getProfile().getNumber("customNumber"), equalTo(1.5d))
        assertThat(user.getProfile().getBoolean("customBoolean"), equalTo(true))
        assertThat(user.getProfile().getInteger("customInteger"), equalTo(123))
        assertThat(user.getProfile().getStringList("customStringArray"), equalTo(["one", "two", "three"]))
        assertThat(user.getProfile().getNumberList("customNumberArray"), equalTo([1.5d, 2.5d, 3.5d]))
        assertThat(user.getProfile().getIntegerList("customIntegerArray"), equalTo([1, 2, 3]))

        // Use the 'get' to update the values to make sure there are no conversion issues
        user.getProfile().putAll([
                        customNumber: user.getProfile().getNumber("customNumber"),
                        customBoolean: true,
                        customStringArray: user.getProfile().getStringList("customStringArray"),
                        customNumberArray: user.getProfile().getNumberList("customNumberArray"),
                        customIntegerArray: user.getProfile().getIntegerList("customIntegerArray")])
        user.update()

        // same check as before
        assertThat(user.getProfile().getNumber("customNumber"), equalTo(1.5d))
        assertThat(user.getProfile().getBoolean("customBoolean"), equalTo(true))
        assertThat(user.getProfile().getInteger("customInteger"), equalTo(123))
        assertThat(user.getProfile().getStringList("customStringArray"), equalTo(["one", "two", "three"]))
        assertThat(user.getProfile().getNumberList("customNumberArray"), equalTo([1.5d, 2.5d, 3.5d]))
        assertThat(user.getProfile().getIntegerList("customIntegerArray"), equalTo([1, 2, 3]))

        // test again but null out all of the values
        user.getProfile().remove("customNumber")
        user.getProfile().remove("customBoolean")
        user.getProfile().remove("customInteger")
        user.getProfile().remove("customStringArray")
        user.getProfile().remove("customNumberArray")
        user.getProfile().remove("customIntegerArray")
        user.update()

        // everything should be null
        assertThat(user.getProfile().getNumber("customNumber"), nullValue())
        assertThat(user.getProfile().getBoolean("customBoolean"), nullValue())
        assertThat(user.getProfile().getInteger("customInteger"), nullValue())
        assertThat(user.getProfile().getStringList("customStringArray"), nullValue())
        assertThat(user.getProfile().getNumberList("customNumberArray"), nullValue())
        assertThat(user.getProfile().getIntegerList("customIntegerArray"), nullValue())
    }


    @Override
    def create(Client client) {
        def email = "joe.coder+${uniqueTestName}@example.com"
        User user = UserBuilder.instance()
                .setEmail(email)
                .setFirstName("Joe")
                .setLastName("Code")
                .setPassword("Password1".toCharArray())
                .setSecurityQuestion("Favorite security question?")
                .setSecurityQuestionAnswer("None of them!")
                .buildAndCreate(client)
        registerForCleanup(user)
        return user
    }

    @Override
    def read(Client client, String id) {
        return client.getUser(id)
    }

    @Override
    void update(Client client, def user) {
        user.getProfile().lastName = "Coder"
        user.update()
    }

    @Override
    void assertUpdate(Client client, Object resource) {
        assertThat resource.getProfile().lastName, equalTo("Coder")
    }

    @Override
    Iterator getResourceCollectionIterator(Client client) {
        return client.listUsers().iterator()
    }

    @Test
    @Scenario("create-user-with-user-type")
    void createUserWithUserTypeTest() {

        def password = 'Passw0rd!2@3#'
        def firstName = 'John'
        def lastName = 'Activate'
        def email = "john-activate=${uniqueTestName}@example.com"

        // 1. Create a User Type
        String name = "java_sdk_user_type_" + RandomStringUtils.randomAlphanumeric(15)

        UserType createdUserType = client.createUserType(client.instantiate(UserType)
            .setName(name)
            .setDisplayName(name)
            .setDescription(name + "_test_description"))
        registerForCleanup(createdUserType)

        assertThat(createdUserType.getId(), notNullValue())

        // 2. Create a user with the User Type
        User user = UserBuilder.instance()
            .setEmail(email)
            .setFirstName(firstName)
            .setLastName(lastName)
            .setPassword(password.toCharArray())
            .setActive(true)
        // See https://developer.okta.com/docs/reference/api/user-types/#specify-the-user-type-of-a-new-user
            .setType(createdUserType.getId())
            .buildAndCreate(client)
        registerForCleanup(user)
        validateUser(user, firstName, lastName, email)

        // 3. Assert User Type
        assertThat(user.getType().getId(), equalTo(createdUserType.getId()))

        // fix flakiness seen in PDV tests
        Thread.sleep(getTestOperationDelay())

        // 4.Verify user in list of active users
        UserList users = client.listUsers(null, 'status eq \"ACTIVE\"', null, null, null)
        assertPresent(users, user)
    }

    @Test
    @Scenario("user-activate")
    void userActivateTest() {

        def password = 'Passw0rd!2@3#'
        def firstName = 'John'
        def lastName = 'Activate'
        def email = "john-activate=${uniqueTestName}@example.com"

        // 1. Create a user
        User user = UserBuilder.instance()
                .setEmail(email)
                .setFirstName(firstName)
                .setLastName(lastName)
                .setPassword(password.toCharArray())
                .setActive(false)
                .buildAndCreate(client)
        registerForCleanup(user)
        validateUser(user, firstName, lastName, email)

        // 2. Activate the user and verify user in list of active users
        user.activate(false)

        // fix flakiness seen in PDV tests
        Thread.sleep(getTestOperationDelay())

        UserList users = client.listUsers(null, 'status eq \"ACTIVE\"', null, null, null)
        assertPresent(users, user)
    }

    @Test
    @Scenario("user-role-assign")
    void roleAssignTest() {

        def password = 'Passw0rd!2@3#'
        def firstName = 'John'
        def lastName = 'Role'
        def email = "john-${uniqueTestName}@example.com"

        // 1. Create a user
        User user = UserBuilder.instance()
                .setEmail(email)
                .setFirstName(firstName)
                .setLastName(lastName)
                .setPassword(password.toCharArray())
                .setActive(true)
                .buildAndCreate(client)
        registerForCleanup(user)
        validateUser(user, firstName, lastName, email)

        // 2. Assign USER_ADMIN role to the user
        AssignRoleRequest assignRoleRequest = client.instantiate(AssignRoleRequest)
        assignRoleRequest.setType(RoleType.USER_ADMIN)

        Role role = user.assignRole(assignRoleRequest)

        // 3. List roles for the user and verify added role
        assertPresent(user.listAssignedRoles(), role)

        // 4. Remove role for the user
        user.removeRole(role.getId())

        // 5. List roles for user and verify role was removed
        assertNotPresent(user.listAssignedRoles(), role)
    }

    @Test
    @Scenario("user-change-password")
    void changePasswordTest() {

        def password = 'Passw0rd!2@3#'
        def firstName = 'John'
        def lastName = 'Change-Password'
        def email = "john-${uniqueTestName}@example.com"

        // 1. Create a user
        User user = UserBuilder.instance()
                .setEmail(email)
                .setFirstName(firstName)
                .setLastName(lastName)
                .setPassword(password.toCharArray())
                .setActive(true)
                .buildAndCreate(client)
        registerForCleanup(user)
        validateUser(user, firstName, lastName, email)

        // 2. Change the user's password
        UserCredentials credentials = user.changePassword(client.instantiate(ChangePasswordRequest)
            .setOldPassword(client.instantiate(PasswordCredential).setValue('Passw0rd!2@3#'.toCharArray()))
            .setNewPassword(client.instantiate(PasswordCredential).setValue('!2@3#Passw0rd'.toCharArray())), true)
        assertThat credentials.getProvider().getType(), equalTo(AuthenticationProviderType.OKTA)

        // 3. make the test recording happy, and call a get on the user
        // TODO: fix har file
        client.getUser(user.getId())
    }

    @Test
    void changeStrictPasswordTest() {

        def password = 'Passw0rd!2@3#'
        def firstName = 'John'
        def lastName = 'Change-Password'
        def email = "john-${uniqueTestName}@example.com"

        def group = randomGroup()
        def policy = randomPasswordPolicy(group.getId())
        policy.setSettings(client.instantiate(PasswordPolicySettings)
            .setPassword(client.instantiate(PasswordPolicyPasswordSettings)
                .setAge(client.instantiate(PasswordPolicyPasswordSettingsAge)
                    .setMinAgeMinutes(Duration.ofDays(2L).toMinutes() as int))))
            .update()

        def policyRuleName = "policyRule+" + UUID.randomUUID().toString()
        PasswordPolicyRule policyRule = policy.createRule(client.instantiate(PasswordPolicyRule)
            .setConditions(client.instantiate(PasswordPolicyRuleConditions)
                .setNetwork(client.instantiate(PolicyNetworkCondition)
                    .setConnection(PolicyNetworkCondition.ConnectionEnum.ANYWHERE)))
                .setActions(client.instantiate(PasswordPolicyRuleActions)
                    .setPasswordChange(client.instantiate(PasswordPolicyRuleAction)
                        .setAccess(PasswordPolicyRuleAction.AccessEnum.ALLOW))
                    .setSelfServicePasswordReset(client.instantiate(PasswordPolicyRuleAction)
                        .setAccess(PasswordPolicyRuleAction.AccessEnum.ALLOW))
                    .setSelfServiceUnlock(client.instantiate(PasswordPolicyRuleAction)
                        .setAccess(PasswordPolicyRuleAction.AccessEnum.DENY)))
            .setName(policyRuleName))
        registerForCleanup(policyRule)

        // 1. Create a user
        User user = UserBuilder.instance()
                .setEmail(email)
                .setFirstName(firstName)
                .setLastName(lastName)
                .setPassword(password.toCharArray())
                .setActive(true)
                .setGroups(group.getId())
                .buildAndCreate(client)
        registerForCleanup(user)
        validateUser(user, firstName, lastName, email)

        // 2. Change the user's password
        def request = client.instantiate(ChangePasswordRequest)
            .setOldPassword(client.instantiate(PasswordCredential).setValue('Passw0rd!2@3#'.toCharArray()))
            .setNewPassword(client.instantiate(PasswordCredential).setValue('!2@3#Passw0rd'.toCharArray()))

        def exception = expect ResourceException, {user.changePassword(request, true)}
        assertThat exception.getMessage(), containsString("E0000014") // Update of credentials failed.

        def credentials = user.changePassword(request, false)
        assertThat credentials.getProvider().getType(), equalTo(AuthenticationProviderType.OKTA)

        // 3. make the test recording happy, and call a get on the user
        // TODO: fix har file
        client.getUser(user.getId())
    }

    @Test(expectedExceptions = ResourceException)
    @Scenario("user-change-recovery-question")
    void changeRecoveryQuestionTest() {

        def password = 'Passw0rd!2@3#'
        def firstName = 'John'
        def lastName = 'Change-Recovery-Question'
        def email = "john-${uniqueTestName}@example.com"

        // 1. Create a user with password & recovery question
        User user = UserBuilder.instance()
            .setEmail(email)
            .setFirstName(firstName)
            .setLastName(lastName)
            .setPassword(password.toCharArray())
            .setActive(true)
            .buildAndCreate(client)
        registerForCleanup(user)
        validateUser(user, firstName, lastName, email)

        // 2. Change the recovery question
        UserCredentials userCredentials = user.changeRecoveryQuestion(client.instantiate(UserCredentials)
            .setPassword(client.instantiate(PasswordCredential)
                .setValue('Passw0rd!2@3#'.toCharArray()))
            .setRecoveryQuestion(client.instantiate(RecoveryQuestionCredential)
                .setQuestion('How many roads must a man walk down?')
                .setAnswer('forty two')))

        assertThat userCredentials.getProvider().getType(), equalTo(AuthenticationProviderType.OKTA)
        assertThat userCredentials.getRecoveryQuestion().question, equalTo('How many roads must a man walk down?')

        // 3. Update the user password through updated recovery question
        userCredentials.getPassword().value = '!2@3#Passw0rd'.toCharArray()
        userCredentials.getRecoveryQuestion().answer = 'forty two'

        // below would throw HTTP 403 exception
        user.changeRecoveryQuestion(userCredentials)

        // 4. make the test recording happy, and call a get on the user
        // TODO: fix har file
        client.getUser(user.getId())
    }

    @Test (groups = "bacon")
    void forgotPasswordTest() {

        def password = 'Passw0rd!2@3#'
        def firstName = 'John'
        def lastName = 'Forgot-Password'
        def email = "john-${uniqueTestName}@example.com"

        // 1. Create a user
        User user = UserBuilder.instance()
                .setEmail(email)
                .setFirstName(firstName)
                .setLastName(lastName)
                .setPassword(password.toCharArray())
                .setActive(true)
                .buildAndCreate(client)
        registerForCleanup(user)
        validateUser(user, firstName, lastName, email)

        ResetPasswordToken response = user.resetPassword(false)
        assertThat response.getResetPasswordUrl(), containsString("/reset_password/")
    }

    @Test
    @Scenario("user-expire-password")
    void expirePasswordTest() {

        def password = 'Passw0rd!2@3#'
        def firstName = 'John'
        def lastName = 'Expire-Password'
        def email = "john-${uniqueTestName}@example.com"

        // 1. Create a user
        User user = UserBuilder.instance()
                .setEmail(email)
                .setFirstName(firstName)
                .setLastName(lastName)
                .setPassword(password.toCharArray())
                .setActive(true)
                .buildAndCreate(client)
        registerForCleanup(user)
        validateUser(user, firstName, lastName, email)

        // 2. Expire the user's password
        User updatedUser = user.expirePassword()
        assertThat updatedUser, notNullValue()
        assertThat updatedUser.getStatus(), is(UserStatus.PASSWORD_EXPIRED)
    }


    @Test
    @Scenario("user-get-reset-password-url")
    void resetPasswordUrlTest() {

        def password = 'Passw0rd!2@3#'
        def firstName = 'John'
        def lastName = 'Get-Reset-Password-URL'
        def email = "john-${uniqueTestName}@example.com"

        // 1. Create a user
        User user = UserBuilder.instance()
                .setEmail(email)
                .setFirstName(firstName)
                .setLastName(lastName)
                .setPassword(password.toCharArray())
                .setActive(true)
                .buildAndCreate(client)
        registerForCleanup(user)
        validateUser(user, firstName, lastName, email)

        // 2. Get the reset password link
        ResetPasswordToken token = user.resetPassword(false)
        assertThat token.getResetPasswordUrl(), notNullValue()
    }

    @Test
    @Scenario("user-get")
    void getUserTest() {

        def password = 'Passw0rd!2@3#'
        def firstName = 'John'
        def lastName = 'Get-User'
        def email = "john-${uniqueTestName}@example.com"

        // 1. Create a user
        User createUser = UserBuilder.instance()
                .setEmail(email)
                .setFirstName(firstName)
                .setLastName(lastName)
                .setPassword(password.toCharArray())
                .setActive(false)
                .buildAndCreate(client)
        registerForCleanup(createUser)
        validateUser(createUser, firstName, lastName, email)

        // 2. Get the user by user ID
        User user = client.getUser(createUser.getId())
        validateUser(user, firstName, lastName, email)

        // 3. Get the user by user login
        User userByLogin = client.getUser(createUser.getProfile().getLogin())
        validateUser(userByLogin, firstName, lastName, email)

        // 3. delete the user
        user.deactivate()
        user.delete()

        // 4. get user expect 404
        expect(ResourceException) {
            client.getUser(email)
        }
    }

    @Test
    @Scenario("user-group-target-role")
    void groupTargetRoleTest() {

        def password = 'Passw0rd!2@3#'
        def firstName = 'John'
        def lastName = 'Group-Target'
        def email = "john-${uniqueTestName}@example.com"
        def groupName = "Group-Target Test Group ${uniqueTestName}"

        // 1. Create a user
        User user = UserBuilder.instance()
                .setEmail(email)
                .setFirstName(firstName)
                .setLastName(lastName)
                .setPassword(password.toCharArray())
                .setActive(false)
                .buildAndCreate(client)
        registerForCleanup(user)
        validateUser(user, firstName, lastName, email)

        Group group = GroupBuilder.instance()
            .setName(groupName)
            .buildAndCreate(client)
        registerForCleanup(group)
        validateGroup(group, groupName)

        // 2. Assign USER_ADMIN role to the user
        AssignRoleRequest assignRoleRequest = client.instantiate(AssignRoleRequest)
        assignRoleRequest.setType(RoleType.USER_ADMIN)

        Role role = user.assignRole(assignRoleRequest)

        // 3. Add Group Target to User Admin Role
        user.addGroupTarget(role.getId(), group.getId())

        // 4. List Group Targets for Role
        assertGroupTargetPresent(user, group, role)

        // 5. Remove Group Target from Admin User Role and verify removed
        // Note: Don’t remove the last group target from a role assignment, as this causes an exception.
        // To get around this, create a new group and add this group target to user admin role

        def adminGroupName = "Group-Target User Admin Test Group ${uniqueTestName}"
        deleteGroup(adminGroupName, client)

        Group adminGroup = GroupBuilder.instance()
            .setName(adminGroupName)
            .buildAndCreate(client)
        registerForCleanup(adminGroup)
        validateGroup(adminGroup, adminGroupName)

        user.addGroupTarget(role.getId(), adminGroup.getId())
        user.removeGroupTarget(role.getId(), adminGroup.getId())

        assertGroupTargetPresent(user, group, role)
    }

    @Test
    @Scenario("user-profile-update")
    void userProfileUpdate() {

        def password = 'Passw0rd!2@3#'
        def firstName = 'John'
        def lastName = 'Profile-Update'
        def email = "john-${uniqueTestName}@example.com"

        // 1. Create a user
        User user = UserBuilder.instance()
                .setEmail(email)
                .setFirstName(firstName)
                .setLastName(lastName)
                .setPassword(password.toCharArray())
                .setActive(false)
                .buildAndCreate(client)
        registerForCleanup(user)
        validateUser(user, firstName, lastName, email)

        def originalLastUpdated = user.lastUpdated

        // 2. Update the user profile and verify that profile was updated
        // Need to wait 1 second here as that is the minimum time resolution of the 'lastUpdated' field
        sleep(1000)
        user.getProfile().put("nickName", "Batman")
        user.update()
        assertThat(user.lastUpdated, greaterThan(originalLastUpdated))
        User updatedUser = client.getUser(user.getId())
        assertThat(updatedUser.getProfile().get("nickName"), equalTo("Batman"))
    }

    @Test
    @Scenario("user-suspend")
    void userSuspendTest() {

        def password = 'Passw0rd!2@3#'
        def firstName = 'John'
        def lastName = 'Suspend'
        def email = "john-${uniqueTestName}@example.com"

        // 1. Create a user
        User user = UserBuilder.instance()
                .setEmail(email)
                .setFirstName(firstName)
                .setLastName(lastName)
                .setPassword(password.toCharArray())
                .setActive(true)
                .buildAndCreate(client)
        registerForCleanup(user)
        validateUser(user, firstName, lastName, email)

        // 2. Suspend the user and verify user in list of suspended users
        user.suspend()

        // fix flakiness seen in PDV tests
        Thread.sleep(getTestOperationDelay())

        assertPresent(client.listUsers(null, 'status eq \"SUSPENDED\"', null, null, null), user)

        // 3. Unsuspend the user and verify user in list of active users
        user.unsuspend()
        assertPresent(client.listUsers(null, 'status eq \"ACTIVE\"', null, null, null), user)
    }

    @Test
    void getUserInvalidUserId() {
        try {
            def userId = "invalid-user-id-${uniqueTestName}@example.com"
            getClient().getUser(userId)
            Assert.fail("Expected ResourceException")
        } catch(ResourceException e) {
            assertThat e.getStatus(), equalTo(404)
        }
    }

    @Test
    void getUserNullUserId() {
        try {
            getClient().getUser(null)
            Assert.fail("Expected IllegalArgumentException")
        } catch(IllegalArgumentException e) {
            assertThat e.getMessage(), allOf(containsString("userId"), containsString("required"))
        }
    }

    @Test
    void getUserEmptyUserId() {
        try {
            getClient().getUser("")
            Assert.fail("Expected IllegalArgumentException")
        } catch(IllegalArgumentException e) {
            assertThat e.getMessage(), allOf(containsString("userId"), containsString("required"))
        }
    }

    @Test
    void createUserWithGroups() {

        def groupName = "Group-to-Assign-${uniqueTestName}"
        def password = 'Passw0rd!2@3#'
        def firstName = 'John'
        def lastName = 'Create-User-With-Group'
        def email = "john-${uniqueTestName}@example.com"

        // 1. Create group
        Group group = GroupBuilder.instance()
            .setName(groupName)
            .buildAndCreate(client)
        registerForCleanup(group)
        validateGroup(group, groupName)

        // 2. Create a user
        User createUser = UserBuilder.instance()
                .setEmail(email)
                .setFirstName(firstName)
                .setLastName(lastName)
                .setPassword(password.toCharArray())
                .setActive(false)
                .addGroup(group.getId())
                .buildAndCreate(client)
        registerForCleanup(createUser)
        validateUser(createUser, firstName, lastName, email)

        List<Group> groups = createUser.listGroups().stream().collect(Collectors.toList())
        assertThat groups, allOf(hasSize(2))
        assertThat groups.get(0).getProfile().name, equalTo("Everyone")
        assertThat groups.get(1).getId(), equalTo(group.id)
    }

    @Test
    void setUserPasswordWithoutReset() {

        def user = randomUser()
        def userId = user.getId()

        Resource userPasswordRequest = client.instantiate(ExtensibleResource)
        userPasswordRequest.put("credentials", client.instantiate(ExtensibleResource)
                               .put("password", client.instantiate(ExtensibleResource)
                                   .put("value", "aPassword1!".toCharArray())))

        Resource result = client.getDataStore().http()
                                    .setBody(userPasswordRequest)
                                    .addQueryParameter("key1", "value1")
                                    .addQueryParameter("key2", "value2")
                                    .post("/api/v1/users/"+ userId, User.class)

        assertThat(result, instanceOf(User))
    }

    @Test
    void importUserWithSha512Password() {

        def salt = "aSalt"
        def hashedPassword = hashPassword("aPassword", salt, PasswordCredentialHashAlgorithm.SHA_512)

        def email = "joe.coder+${uniqueTestName}@example.com"
        User user = UserBuilder.instance()
                .setEmail(email)
                .setFirstName("Joe")
                .setLastName("Code")
                .setSha512PasswordHash(hashedPassword, salt, "PREFIX")
                .buildAndCreate(getClient())
        registerForCleanup(user)

        assertThat user.getCredentials(), notNullValue()
        assertThat user.getCredentials().getProvider().getType(), is(AuthenticationProviderType.IMPORT)
    }

    @Test
    void importUserWithMd5Password() {
        def salt = "aSalt"

        User user = UserBuilder.instance()
                .setEmail("joe.coder+${uniqueTestName}@example.com")
                .setFirstName("Joe")
                .setLastName("Code")
                .setMd5PasswordHash(hashPassword("aPassword", salt, PasswordCredentialHashAlgorithm.MD5), salt, "PREFIX")
                .buildAndCreate(getClient())
        registerForCleanup(user)

        assertThat user.getCredentials(), notNullValue()
        assertThat user.getCredentials().getProvider().getType(), is(AuthenticationProviderType.IMPORT)
    }

    @Test(enabled = false)      // TODO: Enable it back when 379446 is resolved.
    void importCreateAndLoginUserWithMd5Password() {
        def salt = "aSalt"
        def password = "aPassword"
        def hashPassword = hashPassword(password, salt, PasswordCredentialHashAlgorithm.MD5)
        def login = "joe.coder+${uniqueTestName}@example.com"
        def client = getClient()

        User user = UserBuilder.instance()
            .setEmail(login)
            .setFirstName("Joe")
            .setLastName("Code")
            .setMd5PasswordHash(hashPassword, salt, "PREFIX")
            .buildAndCreate(client)
        registerForCleanup(user)

        URL url = new URL(new URL(new URL(user.getResourceHref()), "/").toString() + "api/v1/authn")
        HttpURLConnection con = (HttpURLConnection) url.openConnection()
        con.setRequestMethod("POST")
        con.setRequestProperty("Content-Type", "application/json; utf-8")
        con.setRequestProperty("Accept", "application/json")
        con.setDoOutput(true)
        byte[] input = "{\"password\":\"${password}\",\"username\":\"${login}\"}".getBytes("utf-8")
        con.getOutputStream().write(input, 0, input.length)

        BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"))
        StringBuilder response = new StringBuilder()
        String responseLine
        while ((responseLine = br.readLine()) != null) {
            response.append(responseLine.trim())
        }

        assertThat response.toString().contains("\"status\":\"SUCCESS\""), is(true)
    }

    private void ensureCustomProperties() {
        def userSchemaUri = "/api/v1/meta/schemas/user/default"

        ExtensibleResource userSchema = getClient().http().get(userSchemaUri, ExtensibleResource)
        Map customProperties = userSchema.get("definitions").get("custom").get("properties")

        boolean needsUpdate =
            ensureCustomProperty("customNumber", [type: "number"], customProperties) &&
            ensureCustomProperty("customBoolean", [type: "boolean"], customProperties) &&
            ensureCustomProperty("customInteger", [type: "integer"], customProperties) &&
            ensureCustomProperty("customStringArray", [type: "array", items: [type: "string"]], customProperties) &&
            ensureCustomProperty("customNumberArray", [type: "array", items: [type: "number"]], customProperties) &&
            ensureCustomProperty("customIntegerArray", [type: "array", items: [type: "integer"]], customProperties)

        if (needsUpdate)  {
            getClient().http()
                .setBody(userSchema)
                .post(userSchemaUri, ExtensibleResource)
        }
    }

     private static boolean ensureCustomProperty(String name, Map body, Map<String, Object> customProperties) {
        boolean addProperty = !customProperties.containsKey(name)
        if (addProperty) {
            body.putAll([
                title: name
            ])
            customProperties.put(name, body)
        }
        return addProperty
    }

    private static String hashPassword(String password, String salt, PasswordCredentialHashAlgorithm algorithm) {
        def messageDigest = MessageDigest.getInstance(algorithm.toString())
        messageDigest.update(salt.getBytes(StandardCharsets.UTF_8))
        def bytes = messageDigest.digest(password.getBytes(StandardCharsets.UTF_8))
        return Base64.getEncoder().encodeToString(bytes)
    }
}
