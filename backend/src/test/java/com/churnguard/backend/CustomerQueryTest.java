package com.churnguard.backend;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CustomerQueryTest extends IntegrationTestBase {

    private static final String HEADER =
            "customerID,gender,SeniorCitizen,Partner,Dependents,tenure,PhoneService,MultipleLines,"
                    + "InternetService,OnlineSecurity,OnlineBackup,DeviceProtection,TechSupport,"
                    + "StreamingTV,StreamingMovies,Contract,PaperlessBilling,PaymentMethod,"
                    + "MonthlyCharges,TotalCharges,Churn\n";

    private static String row(String id) {
        return id + ",Female,0,Yes,No,12,No,No phone service,DSL,No,Yes,No,No,No,No,"
                + "Two year,Yes,Electronic check,29.85,350.50,No\n";
    }

    private String registerAndLogin() throws Exception {
        String email = uniqueEmail();
        register("Org-" + UUID.randomUUID(), email, PASSWORD).andExpect(status().isCreated());
        return loginAndGetToken(email, PASSWORD);
    }

    private void importCustomers(String token, String... ids) throws Exception {
        StringBuilder csv = new StringBuilder(HEADER);
        for (String id : ids) {
            csv.append(row(id));
        }
        MockMultipartFile file = new MockMultipartFile(
                "file", "customers.csv", "text/csv",
                csv.toString().getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/customers/import").file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private ResultActions getWithToken(String token, String url) throws Exception {
        return mockMvc.perform(get(url).header("Authorization", "Bearer " + token));
    }

    private String firstCustomerId(String token) throws Exception {
        String json = getWithToken(token, "/api/customers?size=1")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.content[0].id");
    }

    private String createViewerToken(String adminToken) throws Exception {
        String viewerEmail = uniqueEmail();
        String body = """
                {"email":"%s","password":"%s","role":"VIEWER"}
                """.formatted(viewerEmail, PASSWORD);
        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
        return loginAndGetToken(viewerEmail, PASSWORD);
    }

    @Test
    void eachOrganizationSeesOnlyItsOwnCustomers() throws Exception {
        String tokenA = registerAndLogin();
        String tokenB = registerAndLogin();
        importCustomers(tokenA, "A-1", "A-2", "A-3");
        importCustomers(tokenB, "B-1", "B-2");

        getWithToken(tokenA, "/api/customers")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[*].externalId")
                        .value(org.hamcrest.Matchers.everyItem(
                                org.hamcrest.Matchers.startsWith("A-"))));

        getWithToken(tokenB, "/api/customers")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].externalId")
                        .value(org.hamcrest.Matchers.everyItem(
                                org.hamcrest.Matchers.startsWith("B-"))));
    }

    @Test
    void customerOfAnotherOrganizationIsNotFound() throws Exception {
        String tokenA = registerAndLogin();
        String tokenB = registerAndLogin();
        importCustomers(tokenA, "A-1");
        String idOfA = firstCustomerId(tokenA);

        getWithToken(tokenA, "/api/customers/" + idOfA).andExpect(status().isOk());

        getWithToken(tokenB, "/api/customers/" + idOfA).andExpect(status().isNotFound());
        getWithToken(tokenB, "/api/customers/" + UUID.randomUUID())
                .andExpect(status().isNotFound());
    }

    @Test
    void paginationRespectsSizeAndIsCapped() throws Exception {
        String token = registerAndLogin();
        importCustomers(token, "C-1", "C-2", "C-3");

        getWithToken(token, "/api/customers?page=0&size=2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));

        getWithToken(token, "/api/customers?size=1000")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void viewerCanReadCustomers() throws Exception {
        String adminToken = registerAndLogin();
        importCustomers(adminToken, "V-1");
        String viewerToken = createViewerToken(adminToken);

        getWithToken(viewerToken, "/api/customers")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/customers")).andExpect(status().isUnauthorized());
    }

    @Test
    void malformedIdIsRejected() throws Exception {
        String token = registerAndLogin();

        getWithToken(token, "/api/customers/pas-un-uuid").andExpect(status().isBadRequest());
    }

    @Test
    void responseDoesNotExposeOrganization() throws Exception {
        String token = registerAndLogin();
        importCustomers(token, "R-1");
        String id = firstCustomerId(token);

        getWithToken(token, "/api/customers/" + id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalId").value("R-1"))
                .andExpect(jsonPath("$.organization").doesNotExist())
                .andExpect(jsonPath("$.organizationId").doesNotExist());
    }
}