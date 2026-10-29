package com.churnguard.backend;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthLoginTest extends IntegrationTestBase {

    @Test
    void loginWithCorrectPasswordReturnsBearerToken() throws Exception {
        String email = uniqueEmail();
        register("Acme", email, PASSWORD).andExpect(status().isCreated());

        login(email, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void loginWithWrongPasswordIsUnauthorized() throws Exception {
        String email = uniqueEmail();
        register("Acme", email, PASSWORD).andExpect(status().isCreated());

        login(email, "MauvaisMotDePasse-123").andExpect(status().isUnauthorized());
    }

    @Test
    void loginIgnoresEmailCase() throws Exception {
        String email = uniqueEmail();
        register("Acme", email, PASSWORD).andExpect(status().isCreated());

        login(email.toUpperCase(), PASSWORD).andExpect(status().isOk());
    }

    @Test
    void unknownEmailAndWrongPasswordGiveIdenticalResponses() throws Exception {
        String email = uniqueEmail();
        register("Acme", email, PASSWORD).andExpect(status().isCreated());

        String wrongPasswordResponse = login(email, "MauvaisMotDePasse-123")
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        String unknownEmailResponse = login(uniqueEmail(), PASSWORD)
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(unknownEmailResponse).isEqualTo(wrongPasswordResponse);
    }
}