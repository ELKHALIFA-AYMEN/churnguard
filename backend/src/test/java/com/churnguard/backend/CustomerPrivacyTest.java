package com.churnguard.backend;

import com.churnguard.backend.ml.MlApi;
import com.churnguard.backend.repository.CustomerRepository;
import com.churnguard.backend.repository.PredictionRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CustomerPrivacyTest extends IntegrationTestBase {

    private static final String HEADER =
            "customerID,gender,SeniorCitizen,Partner,Dependents,tenure,PhoneService,MultipleLines,"
                    + "InternetService,OnlineSecurity,OnlineBackup,DeviceProtection,TechSupport,"
                    + "StreamingTV,StreamingMovies,Contract,PaperlessBilling,PaymentMethod,"
                    + "MonthlyCharges,TotalCharges,Churn\n";

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PredictionRepository predictionRepository;

    @BeforeEach
    void stubMlService() {
        when(mlClient.score(anyList())).thenAnswer(invocation -> {
            List<MlApi.MlCustomer> input = invocation.getArgument(0);
            List<MlApi.Prediction> predictions = input.stream().map(customer -> {
                boolean monthly = "Month-to-month".equals(customer.contract());
                return new MlApi.Prediction(
                        new BigDecimal(monthly ? "0.8000" : "0.1000"),
                        monthly,
                        List.of(new MlApi.Factor("Contrat", new BigDecimal("0.50"),
                                monthly ? "increases" : "decreases")));
            }).toList();
            return new MlApi.Response(new BigDecimal("0.2000"), predictions);
        });
    }

    // ---------- Fonctions d'aide ----------

    private static String row(String id, String contract) {
        return id + ",Female,0,Yes,No,12,No,No phone service,DSL,No,Yes,No,No,No,No,"
                + contract + ",Yes,Electronic check,29.85,350.50,No\n";
    }

    private String registerAndLogin() throws Exception {
        String email = uniqueEmail();
        register("Org-" + UUID.randomUUID(), email, PASSWORD).andExpect(status().isCreated());
        return loginAndGetToken(email, PASSWORD);
    }

    private void importCustomers(String token, String... rows) throws Exception {
        StringBuilder csv = new StringBuilder(HEADER);
        for (String row : rows) {
            csv.append(row);
        }
        MockMultipartFile file = new MockMultipartFile(
                "file", "customers.csv", "text/csv", csv.toString().getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/customers/import").file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private ResultActions run(String token) throws Exception {
        return mockMvc.perform(post("/api/predictions/run")
                .header("Authorization", "Bearer " + token));
    }

    private ResultActions getWithToken(String token, String url) throws Exception {
        return mockMvc.perform(get(url).header("Authorization", "Bearer " + token));
    }

    private ResultActions deleteWithToken(String token, String url) throws Exception {
        return mockMvc.perform(delete(url).header("Authorization", "Bearer " + token));
    }

    private UUID organizationIdOf(String token) throws Exception {
        String json = getWithToken(token, "/api/me").andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.organizationId"));
    }

    private UUID customerIdOf(String token, String externalId) throws Exception {
        return customerRepository
                .findByOrganizationIdAndExternalId(organizationIdOf(token), externalId)
                .orElseThrow()
                .getId();
    }

    private String createUserToken(String adminToken, String role) throws Exception {
        String email = uniqueEmail();
        String body = """
                {"email":"%s","password":"%s","role":"%s"}
                """.formatted(email, PASSWORD, role);
        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
        return loginAndGetToken(email, PASSWORD);
    }

    // ---------- Droit d'accès (export) ----------

    @Test
    void adminCanExportCustomerWithItsPrediction() throws Exception {
        String token = registerAndLogin();
        importCustomers(token, row("C-1", "Month-to-month"));
        run(token).andExpect(status().isOk());
        UUID id = customerIdOf(token, "C-1");

        String body = getWithToken(token, "/api/customers/" + id + "/export")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportedAt").isNotEmpty())
                .andExpect(jsonPath("$.customer").exists())
                .andExpect(jsonPath("$.prediction.externalId").value("C-1"))
                .andExpect(jsonPath("$.prediction.factors[0].theme").value("Contrat"))
                .andReturn().getResponse().getContentAsString();

        // L'identifiant de l'organisation ne doit jamais sortir dans l'export.
        assertThat(body).doesNotContain(organizationIdOf(token).toString());
    }

    @Test
    void exportWorksForCustomerWithoutPrediction() throws Exception {
        String token = registerAndLogin();
        importCustomers(token, row("C-1", "Two year"));
        UUID id = customerIdOf(token, "C-1");

        getWithToken(token, "/api/customers/" + id + "/export")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer").exists())
                .andExpect(jsonPath("$.prediction").doesNotExist());
    }

    // ---------- Droit à l'effacement ----------

    @Test
    void deleteRemovesTheCustomerAndItsPredictionOnly() throws Exception {
        String token = registerAndLogin();
        importCustomers(token, row("C-1", "Month-to-month"), row("C-2", "Two year"));
        run(token).andExpect(status().isOk());
        UUID id = customerIdOf(token, "C-1");

        deleteWithToken(token, "/api/customers/" + id).andExpect(status().isNoContent());

        getWithToken(token, "/api/customers/" + id).andExpect(status().isNotFound());
        getWithToken(token, "/api/predictions/customers/" + id).andExpect(status().isNotFound());
        assertThat(predictionRepository.countByOrganizationId(organizationIdOf(token))).isEqualTo(1);
        getWithToken(token, "/api/customers").andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ---------- Cloisonnement et droits ----------

    @Test
    void anotherOrganizationCannotExportOrDelete() throws Exception {
        String tokenA = registerAndLogin();
        String tokenB = registerAndLogin();
        importCustomers(tokenA, row("A-1", "Month-to-month"));
        UUID idOfA = customerIdOf(tokenA, "A-1");

        getWithToken(tokenB, "/api/customers/" + idOfA + "/export").andExpect(status().isNotFound());
        deleteWithToken(tokenB, "/api/customers/" + idOfA).andExpect(status().isNotFound());

        getWithToken(tokenA, "/api/customers/" + idOfA).andExpect(status().isOk());
    }

    @Test
    void onlyAdminsCanExportOrDelete() throws Exception {
        String adminToken = registerAndLogin();
        importCustomers(adminToken, row("C-1", "Month-to-month"));
        UUID id = customerIdOf(adminToken, "C-1");

        for (String role : List.of("VIEWER", "ANALYST")) {
            String token = createUserToken(adminToken, role);
            getWithToken(token, "/api/customers/" + id + "/export").andExpect(status().isForbidden());
            deleteWithToken(token, "/api/customers/" + id).andExpect(status().isForbidden());
        }

        getWithToken(adminToken, "/api/customers/" + id).andExpect(status().isOk());
    }

    @Test
    void exportAndDeleteRequireAuthentication() throws Exception {
        UUID anyId = UUID.randomUUID();

        mockMvc.perform(get("/api/customers/" + anyId + "/export")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/customers/" + anyId)).andExpect(status().isUnauthorized());
    }

    @Test
    void unknownCustomerIsNotFound() throws Exception {
        String token = registerAndLogin();
        UUID unknown = UUID.randomUUID();

        getWithToken(token, "/api/customers/" + unknown + "/export").andExpect(status().isNotFound());
        deleteWithToken(token, "/api/customers/" + unknown).andExpect(status().isNotFound());
    }
}