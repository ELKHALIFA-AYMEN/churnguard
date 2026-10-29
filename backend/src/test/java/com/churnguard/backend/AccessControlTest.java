package com.churnguard.backend;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccessControlTest extends IntegrationTestBase {

    private ResultActions me(String token) throws Exception {
        return mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + token));
    }

    private ResultActions createUser(String token, String email, String role, String extraJson)
            throws Exception {
        String body = """
                {"email":"%s","password":"%s","role":"%s"%s}
                """.formatted(email, PASSWORD, role, extraJson);

        return mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void meWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void meWithFakeTokenIsUnauthorized() throws Exception {
        me("abc.def.ghi").andExpect(status().isUnauthorized());
    }

    @Test
    void tamperedTokenIsRejected() throws Exception {
        String email = uniqueEmail();
        register("Acme", email, PASSWORD).andExpect(status().isCreated());
        String token = loginAndGetToken(email, PASSWORD);

        String[] parts = token.split("\\.");
        char first = parts[2].charAt(0);
        char replacement = first == 'A' ? 'B' : 'A';
        String tampered = parts[0] + "." + parts[1] + "." + replacement + parts[2].substring(1);

        me(tampered).andExpect(status().isUnauthorized());
    }

    @Test
    void meWithValidTokenReturnsRoleAndAuthorities() throws Exception {
        String email = uniqueEmail();
        register("Acme", email, PASSWORD).andExpect(status().isCreated());
        String token = loginAndGetToken(email, PASSWORD);

        me(token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.authorities", hasItem("ROLE_ADMIN")));
    }

    @Test
    void adminCanCreateViewerInOwnOrganization() throws Exception {
        String orgName = "Org-" + UUID.randomUUID();
        String adminEmail = uniqueEmail();
        register(orgName, adminEmail, PASSWORD).andExpect(status().isCreated());
        String adminToken = loginAndGetToken(adminEmail, PASSWORD);

        createUser(adminToken, uniqueEmail(), "VIEWER", "")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("VIEWER"))
                .andExpect(jsonPath("$.organizationName").value(orgName));
    }

    @Test
    void viewerCannotCreateUsers() throws Exception {
        String adminEmail = uniqueEmail();
        register("Acme", adminEmail, PASSWORD).andExpect(status().isCreated());
        String adminToken = loginAndGetToken(adminEmail, PASSWORD);

        String viewerEmail = uniqueEmail();
        createUser(adminToken, viewerEmail, "VIEWER", "").andExpect(status().isCreated());
        String viewerToken = loginAndGetToken(viewerEmail, PASSWORD);

        String intruderEmail = uniqueEmail();
        createUser(viewerToken, intruderEmail, "ADMIN", "").andExpect(status().isForbidden());

        login(intruderEmail, PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void adminCannotCreateUserInAnotherOrganization() throws Exception {
        String emailA = uniqueEmail();
        String emailB = uniqueEmail();
        String orgNameB = "Org-B-" + UUID.randomUUID();

        register("Org-A-" + UUID.randomUUID(), emailA, PASSWORD).andExpect(status().isCreated());
        register(orgNameB, emailB, PASSWORD).andExpect(status().isCreated());

        String tokenA = loginAndGetToken(emailA, PASSWORD);
        String tokenB = loginAndGetToken(emailB, PASSWORD);

        String orgIdA = JsonPath.read(
                me(tokenA).andReturn().getResponse().getContentAsString(), "$.organizationId");

        createUser(tokenB, uniqueEmail(), "VIEWER", ",\"organizationId\":\"" + orgIdA + "\"")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.organizationName").value(orgNameB));
    }
}