package com.churnguard.backend;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthRegistrationTest extends IntegrationTestBase {

    @Test
    void registrationCreatesAdminAndNeverReturnsPassword() throws Exception {
        String email = uniqueEmail();

        register("Acme", email, PASSWORD)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void registrationWithExistingEmailIsRejectedWithConflict() throws Exception {
        String email = uniqueEmail();

        register("Acme", email, PASSWORD).andExpect(status().isCreated());
        register("Autre", email, PASSWORD).andExpect(status().isConflict());
    }

    @Test
    void emailsAreCaseInsensitive() throws Exception {
        String email = uniqueEmail();

        register("Acme", email, PASSWORD).andExpect(status().isCreated());
        register("Autre", email.toUpperCase(), PASSWORD).andExpect(status().isConflict());
    }

    @Test
    void shortPasswordIsRejected() throws Exception {
        register("Acme", uniqueEmail(), "court").andExpect(status().isBadRequest());
    }

    @Test
    void invalidEmailIsRejected() throws Exception {
        register("Acme", "pas-un-email", PASSWORD).andExpect(status().isBadRequest());
    }
}