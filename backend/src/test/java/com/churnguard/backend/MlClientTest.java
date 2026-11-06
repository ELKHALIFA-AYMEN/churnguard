package com.churnguard.backend;

import com.churnguard.backend.exception.MlServiceException;
import com.churnguard.backend.ml.MlApi;
import com.churnguard.backend.ml.MlClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MlClientTest {

    private static final String TOKEN = "t".repeat(40);

    private static final String ONE_PREDICTION = """
            {"threshold":0.2,"predictions":[{"probability":0.6,"at_risk":true,
            "factors":[{"theme":"Contrat","impact":0.5,"direction":"increases"}]}]}
            """;

    private MockRestServiceServer server;
    private MlClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://ml.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new MlClient(builder.build(), TOKEN);
    }

    private static MlApi.MlCustomer customer() {
        return new MlApi.MlCustomer(12, new BigDecimal("70.50"), null, "Yes", "No",
                "Fiber optic", "No", "No", "No", "No", "No", "No",
                "Month-to-month", "Yes", "Electronic check");
    }

    @Test
    void sendsTokenAndOnlyTheModelFields() {
        server.expect(requestTo("http://ml.test/predict"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Internal-Token", TOKEN))
                .andExpect(jsonPath("$.customers[0].Contract").value("Month-to-month"))
                .andExpect(jsonPath("$.customers[0].gender").doesNotExist())
                .andExpect(jsonPath("$.customers[0].customerID").doesNotExist())
                .andRespond(withSuccess(ONE_PREDICTION, MediaType.APPLICATION_JSON));

        List<MlApi.Prediction> result = client.predict(List.of(customer()));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).atRisk()).isTrue();
        assertThat(result.get(0).probability()).isEqualByComparingTo("0.6");
        server.verify();
    }

    @Test
    void serverErrorBecomesMlServiceException() {
        server.expect(requestTo("http://ml.test/predict")).andRespond(withServerError());

        assertThatThrownBy(() -> client.predict(List.of(customer())))
                .isInstanceOf(MlServiceException.class);
    }

    @Test
    void rejectedTokenBecomesMlServiceException() {
        server.expect(requestTo("http://ml.test/predict"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.predict(List.of(customer())))
                .isInstanceOf(MlServiceException.class);
    }

    @Test
    void mismatchedNumberOfPredictionsIsRejected() {
        server.expect(requestTo("http://ml.test/predict"))
                .andRespond(withSuccess(ONE_PREDICTION, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.predict(List.of(customer(), customer())))
                .isInstanceOf(MlServiceException.class);
    }

    @Test
    void emptyListMakesNoCall() {
        assertThat(client.predict(List.of())).isEmpty();
        server.verify();
    }

    @Test
    void oversizedBatchIsRefusedBeforeAnyCall() {
        List<MlApi.MlCustomer> tooMany = Collections.nCopies(MlClient.MAX_BATCH + 1, customer());

        assertThatThrownBy(() -> client.predict(tooMany))
                .isInstanceOf(IllegalArgumentException.class);
        server.verify();
    }

    @Test
    void shortTokenIsRefusedAtStartup() {
        assertThatThrownBy(() -> new MlClient(RestClient.builder().build(), "court"))
                .isInstanceOf(IllegalStateException.class);
    }
}