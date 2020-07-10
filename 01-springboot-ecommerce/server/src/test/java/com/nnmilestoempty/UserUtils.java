package com.nnmilestoempty;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nnmilestoempty.model.dao.auth.Role;
import com.nnmilestoempty.model.dao.auth.User;
import com.nnmilestoempty.model.request.LoginRequest;
import com.nnmilestoempty.repository.auth.UserRepository;
import com.nnmilestoempty.security.CustomUserDetails;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Component
public class UserUtils {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Qualifier("customUserDetailsManager")
    @Autowired
    private UserDetailsManager userDetailsManager;

    @Autowired
    private GoogleAuthenticator googleAuthenticator;

    private ObjectMapper mapper = new ObjectMapper();

    public String createUserRegistrationPayload(String firstName, String lastName, String username, String password,
                                                String emailAddress) {
        return null;
    }

    public User createUser(String firstName, String lastName, String email, String username, String password,
                           boolean enabled, boolean multiFactorAuth) {
        Role role = new Role("ROLE_USER");
        User user = new User(firstName, lastName, email, username, password, enabled);
        if (multiFactorAuth) {
            user.setUsing2FA(true);
            user.setSecret2FA(googleAuthenticator.createCredentials().getKey());
        }
        role.setUser(user);
        user.setRoles(Collections.singleton(role));
        UserDetails details = new CustomUserDetails(user);
        if (!userDetailsManager.userExists(username)) {
            userDetailsManager.createUser(details);
        }

        return userRepository.findByUsername(username);
    }

    public String loginUser(String username, String password) throws Exception {
        return loginUser(createLoginRequest(username, password));
    }

    public void loginUserExpectFail(String username, String password) throws Exception {
        LoginRequest request = createLoginRequest(username, password);

        String payload = mapper.writeValueAsString(request);

        mockMvc.perform(post("/login").content(payload).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.UNAUTHORIZED.value()));
    }

    private LoginRequest createLoginRequest(String username, String password) {
        User user = userRepository.findByUsername(username);
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        if (user.isUsing2FA()) {
            request.setVerificationCode(Integer.toString(googleAuthenticator.getTotpPassword(user.getSecret2FA())));
        }

        return request;
    }

    private String loginUser(LoginRequest request) throws Exception {
        String payload = mapper.writeValueAsString(request);

        String content =
                mockMvc.perform(post("/login").content(payload).contentType(MediaType.APPLICATION_JSON))
                        .andReturn().getResponse().getContentAsString();

        Map<String, Object> jsonMap = jsonToMap(content);
        String accessToken = (String) jsonMap.get("accessToken");

        return accessToken;
    }

    public Map<String, Object> jsonToMap(String json) throws IOException {
        TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
        };

        Map<String, Object> jsonMap;
        try {
            jsonMap = mapper.readValue(json, typeRef);
        } catch (Exception e) {
            throw new IOException();
        }

        return jsonMap;
    }
}
