package com.nnmilestoempty.controller;

import com.nnmilestoempty.UserUtils;
import com.nnmilestoempty.model.dao.auth.RegistrationToken;
import com.nnmilestoempty.model.dao.auth.User;
import com.nnmilestoempty.repository.auth.RegistrationKeyRepository;
import com.nnmilestoempty.repository.auth.UserRepository;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.AssertionErrors;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserUtils userUtils;

    @Autowired
    private RegistrationKeyRepository registrationKeyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GoogleAuthenticator googleAuthenticator;

    @Test
    public void testLoginDisabledUser() throws Exception {
        String username = "disableduser";
        String password = "abcdef123456";

        User disabledUser = userUtils.createUser("Disabled", "User", "disabled.user@99milestoempty.com", username,
                password, false, false);

        userUtils.loginUserExpectFail(username, password);
    }

    @Test
    public void testRegisterVerifyAndLoginUser() throws Exception {
        String username = "jsample";

        // Create a new user to register.
        String payload = "{" +
                "  \"firstName\": \"Joe\"," +
                "  \"lastName\": \"Sample\"," +
                "  \"username\": \"" + username + "\"," +
                "  \"email\": \"jsample@99milestoempty.org\"," +
                "  \"password\": \"testtesttesttesttest\"" +
                " }";
        mockMvc.perform(post("/register").content(payload).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.OK.value()));

        // Verify that they can not log in yet since they have not verified the registration.
        payload =
                "{ \"username\": \"" + username +
                        "\", \"password\": \"testtesttesttesttest\" }";
        mockMvc.perform(post("/login").content(payload).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.UNAUTHORIZED.value()));

        // Get the registration token and verify that we can call the endpoint to verify this user.
        RegistrationToken registrationToken = registrationKeyRepository.findFirstByUsername(username);
        mockMvc.perform(get("/register/" + registrationToken.getToken()))
                .andExpect(status().is(HttpStatus.OK.value()));

        // Check to make sure the user can not use the same registration token twice.
        mockMvc.perform(get("/register/" + registrationToken.getToken()))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));

        // Verify that the user can now log in after being verified without 2-factor auth since it has not yet been
        // enabled.
        payload =
                "{ \"username\": \"" + username +
                        "\", \"password\": \"testtesttesttesttest\" }";
        String jsonResponse = mockMvc.perform(post("/login").content(payload).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.OK.value())).andExpect(jsonPath("accessToken", is(notNullValue())))
                .andReturn().getResponse().getContentAsString();
        // Get the secret
        Map<String, Object> jsonMap = userUtils.jsonToMap(jsonResponse);
        String accessToken = (String) jsonMap.get("accessToken");

        // Enable 2-factor auth.
        payload = "{ \"username\": \"" + username +
                "\", \"password\": \"testtesttesttesttest\", \"enable2FA\": \"true\" }";
        jsonResponse =
                mockMvc.perform(post("/user/multifactorauth").content(payload).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)).andExpect(status().is(HttpStatus.OK.value()))
                        .andReturn().getResponse().getContentAsString();
        // Get the secret
        jsonMap = userUtils.jsonToMap(jsonResponse);
        String mfaSecret = (String) jsonMap.get("secret");

        // Verify enabling of 2-factor auth by passing in verification code.
        payload = "{ \"username\": \"" + username +
                "\", \"password\": \"testtesttesttesttest\", \"enable2FA\": \"true\", \"verificationCode\": \"" +
                googleAuthenticator.getTotpPassword(mfaSecret) + "\" }";
        mockMvc.perform(post("/user/multifactorauth").content(payload).contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)).andExpect(status().is(HttpStatus.OK.value()));

        // Verify login now requires 2-factor auth.
        payload =
                "{ \"username\": \"" + username +
                        "\", \"password\": \"testtesttesttesttest\" }";
        mockMvc.perform(post("/login").content(payload).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.UNAUTHORIZED.value()));

        payload =
                "{ \"username\": \"" + username +
                        "\", \"password\": \"testtesttesttesttest\", \"verificationCode\": \"" +
                        googleAuthenticator.getTotpPassword(mfaSecret) + "\" }";
        mockMvc.perform(post("/login").content(payload).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.OK.value())).andExpect(jsonPath("accessToken", is(notNullValue())));

    }

    @Test
    public void testInvalidRegistrationToken() throws Exception {
        mockMvc.perform(get("/register/abcdefghijklmnop"))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));

        // Try a payload that is possibly malicious
        // Try passing possibly malicious data
        boolean caught = false;
        try {
            mockMvc.perform(get("/register/abcdef;stealpasswords"));
        } catch (RequestRejectedException e) {
            caught = true;
        }
        AssertionErrors
                .assertTrue("Expected a RequestRejectedException due to possibly malicious string \";\"", caught);

    }
}
