package com.churnguard.backend;

import com.churnguard.backend.exception.MlServiceException;
import com.churnguard.backend.ml.MlApi;
import com.churnguard.backend.repository.PredictionRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PredictionTest extends IntegrationTestBase {

    private static final String HEADER =
            "customerID,gender,SeniorCitizen,Partner,Dependents,tenure,PhoneService,MultipleLines,"
                    + "InternetService,OnlineSecurity,OnlineBackup,DeviceProtection,TechSupport,"
                    + "StreamingTV,StreamingMovies,Contract,PaperlessBilling,PaymentMethod,"
                    + "MonthlyCharges,TotalCharges,Churn\n";

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

    private UUID organizationIdOf(String token) throws Exception {
        String json = getWithToken(token, "/api/me").andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.organizationId"));
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
    void runStoresPredictionsAndListIsSortedByRisk() throws Exception {
        String token = registerAndLogin();
        importCustomers(token, row("C-1", "Two year"), row("C-2", "Month-to-month"),
                row("C-3", "One year"));

        run(token).andExpect(status().isOk())
                .andExpect(jsonPath("$.customers").value(3))
                .andExpect(jsonPath("$.created").value(3))
                .andExpect(jsonPath("$.atRisk").value(1));

        getWithToken(token, "/api/predictions").andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].externalId").value("C-2"))
                .andExpect(jsonPath("$.content[0].atRisk").value(true))
                .andExpect(jsonPath("$.content[1].atRisk").value(false));
    }

    @Test
    void rerunUpdatesWithoutCreatingDuplicates() throws Exception {
        String token = registerAndLogin();
        importCustomers(token, row("C-1", "Two year"), row("C-2", "Month-to-month"));

        run(token).andExpect(jsonPath("$.created").value(2));
        run(token).andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(0))
                .andExpect(jsonPath("$.updated").value(2));

        assertThat(predictionRepository.countByOrganizationId(organizationIdOf(token))).isEqualTo(2);
    }

    @Test
    void atRiskFilterReturnsOnlyAtRiskCustomers() throws Exception {
        String token = registerAndLogin();
        importCustomers(token, row("C-1", "Two year"), row("C-2", "Month-to-month"),
                row("C-3", "Month-to-month"));
        run(token).andExpect(status().isOk());

        getWithToken(token, "/api/predictions?atRiskOnly=true").andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void predictionDetailContainsTheReasons() throws Exception {
        String token = registerAndLogin();
        importCustomers(token, row("C-1", "Month-to-month"));
        run(token).andExpect(status().isOk());

        String list = getWithToken(token, "/api/predictions").andReturn().getResponse().getContentAsString();
        String customerId = JsonPath.read(list, "$.content[0].customerId");

        getWithToken(token, "/api/predictions/customers/" + customerId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.factors.length()").value(1))
                .andExpect(jsonPath("$.factors[0].theme").value("Contrat"));
    }

    @Test
    void organizationsAreIsolated() throws Exception {
        String tokenA = registerAndLogin();
        String tokenB = registerAndLogin();
        importCustomers(tokenA, row("A-1", "Month-to-month"), row("A-2", "Two year"),
                row("A-3", "One year"));
        importCustomers(tokenB, row("B-1", "Month-to-month"), row("B-2", "Two year"));

        run(tokenA).andExpect(status().isOk());

        getWithToken(tokenB, "/api/predictions").andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        String listA = getWithToken(tokenA, "/api/predictions").andReturn()
                .getResponse().getContentAsString();
        String customerOfA = JsonPath.read(listA, "$.content[0].customerId");
        getWithToken(tokenB, "/api/predictions/customers/" + customerOfA)
                .andExpect(status().isNotFound());

        run(tokenB).andExpect(jsonPath("$.customers").value(2));

        assertThat(predictionRepository.countByOrganizationId(organizationIdOf(tokenA))).isEqualTo(3);
        assertThat(predictionRepository.countByOrganizationId(organizationIdOf(tokenB))).isEqualTo(2);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MlApi.MlCustomer>> captor = ArgumentCaptor.forClass(List.class);
        verify(mlClient, org.mockito.Mockito.times(2)).score(captor.capture());
        assertThat(captor.getAllValues().get(0)).hasSize(3);
        assertThat(captor.getAllValues().get(1)).hasSize(2);
    }

    @Test
    void viewerCanReadButCannotRun() throws Exception {
        String adminToken = registerAndLogin();
        importCustomers(adminToken, row("C-1", "Month-to-month"));
        run(adminToken).andExpect(status().isOk());
        String viewerToken = createViewerToken(adminToken);

        getWithToken(viewerToken, "/api/predictions").andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        run(viewerToken).andExpect(status().isForbidden());
    }

    @Test
    void predictionsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/predictions")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/predictions/run")).andExpect(status().isUnauthorized());
    }

    @Test
    void mlServiceFailureReturns503AndStoresNothing() throws Exception {
        String token = registerAndLogin();
        importCustomers(token, row("C-1", "Month-to-month"));
        when(mlClient.score(anyList())).thenThrow(new MlServiceException("down"));

        run(token).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("Prediction service unavailable"));

        assertThat(predictionRepository.countByOrganizationId(organizationIdOf(token))).isZero();
    }

    @Test
    void organizationWithoutCustomersMakesNoCallToMlService() throws Exception {
        String token = registerAndLogin();

        run(token).andExpect(status().isOk())
                .andExpect(jsonPath("$.customers").value(0));

        verify(mlClient, never()).score(anyList());
    }
}