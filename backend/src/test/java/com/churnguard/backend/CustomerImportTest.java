package com.churnguard.backend;

import com.churnguard.backend.repository.CustomerRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CustomerImportTest extends IntegrationTestBase {

    private static final String HEADER =
            "customerID,gender,SeniorCitizen,Partner,Dependents,tenure,PhoneService,MultipleLines,"
                    + "InternetService,OnlineSecurity,OnlineBackup,DeviceProtection,TechSupport,"
                    + "StreamingTV,StreamingMovies,Contract,PaperlessBilling,PaymentMethod,"
                    + "MonthlyCharges,TotalCharges,Churn\n";

    @Autowired
    private CustomerRepository customerRepository;

    private static String row(String id, String contract) {
        return id + ",Female,0,Yes,No,12,No,No phone service,DSL,No,Yes,No,No,No,No,"
                + contract + ",Yes,Electronic check,29.85,350.50,No\n";
    }

    private ResultActions importCsv(String token, String filename, String content)
            throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", filename, "text/csv", content.getBytes(StandardCharsets.UTF_8));
        var request = multipart("/api/customers/import").file(file);
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(request);
    }

    private String registerAndLogin() throws Exception {
        String email = uniqueEmail();
        register("Org-" + UUID.randomUUID(), email, PASSWORD).andExpect(status().isCreated());
        return loginAndGetToken(email, PASSWORD);
    }

    private UUID organizationIdOf(String token) throws Exception {
        String json = mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
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
    void importThenReimportDoesNotCreateDuplicates() throws Exception {
        String token = registerAndLogin();
        String csv = HEADER + row("C-1", "Two year") + row("C-2", "One year");

        importCsv(token, "customers.csv", csv)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(2))
                .andExpect(jsonPath("$.created").value(2))
                .andExpect(jsonPath("$.updated").value(0))
                .andExpect(jsonPath("$.rejected").value(0));

        importCsv(token, "customers.csv", csv)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(0))
                .andExpect(jsonPath("$.updated").value(2));

        assertThat(customerRepository.countByOrganizationId(organizationIdOf(token))).isEqualTo(2);
    }

    @Test
    void viewerCannotImport() throws Exception {
        String adminToken = registerAndLogin();
        String viewerToken = createViewerToken(adminToken);

        importCsv(viewerToken, "customers.csv", HEADER + row("C-1", "Two year"))
                .andExpect(status().isForbidden());

        assertThat(customerRepository.countByOrganizationId(organizationIdOf(adminToken))).isZero();
    }

    @Test
    void importWithoutTokenIsUnauthorized() throws Exception {
        importCsv(null, "customers.csv", HEADER + row("C-1", "Two year"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonCsvFileIsRejected() throws Exception {
        String token = registerAndLogin();

        importCsv(token, "customers.txt", HEADER + row("C-1", "Two year"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fileWithMissingColumnIsRejectedAndNamesTheColumn() throws Exception {
        String token = registerAndLogin();
        String csv = HEADER.replace(",Contract", "") + "C-1\n";

        importCsv(token, "customers.csv", csv)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("Contract")));
    }

    @Test
    void invalidRowIsReportedAndValidRowIsImported() throws Exception {
        String token = registerAndLogin();
        String csv = HEADER + row("C-1", "Two year") + row("C-2", "Bad-Contract-Value");

        importCsv(token, "customers.csv", csv)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(1))
                .andExpect(jsonPath("$.rejected").value(1))
                .andExpect(jsonPath("$.errors[0].line").value(3));

        assertThat(customerRepository.countByOrganizationId(organizationIdOf(token))).isEqualTo(1);
    }

    @Test
    void importsAreIsolatedBetweenOrganizations() throws Exception {
        String tokenA = registerAndLogin();
        String tokenB = registerAndLogin();
        String csv = HEADER + row("SAME-1", "Two year") + row("SAME-2", "One year");

        importCsv(tokenA, "customers.csv", csv).andExpect(jsonPath("$.created").value(2));
        importCsv(tokenB, "customers.csv", csv)
                .andExpect(jsonPath("$.created").value(2))
                .andExpect(jsonPath("$.updated").value(0));

        assertThat(customerRepository.countByOrganizationId(organizationIdOf(tokenA))).isEqualTo(2);
        assertThat(customerRepository.countByOrganizationId(organizationIdOf(tokenB))).isEqualTo(2);
    }
}