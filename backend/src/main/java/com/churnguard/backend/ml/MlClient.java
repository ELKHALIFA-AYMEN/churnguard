package com.churnguard.backend.ml;

import com.churnguard.backend.exception.MlServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class MlClient {

    public static final int MAX_BATCH = 1000;

    private static final Logger log = LoggerFactory.getLogger(MlClient.class);

    private final RestClient restClient;
    private final String token;

    public MlClient(RestClient mlRestClient, @Value("${app.ml.token}") String token) {
        if (token == null || token.length() < 32) {
            throw new IllegalStateException("app.ml.token must be at least 32 characters long");
        }
        this.restClient = mlRestClient;
        this.token = token;
    }

    public List<MlApi.Prediction> predict(List<MlApi.MlCustomer> customers) {
        return score(customers).predictions();
    }

    public MlApi.Response score(List<MlApi.MlCustomer> customers) {
        if (customers.isEmpty()) {
            return new MlApi.Response(null, List.of());
        }
        if (customers.size() > MAX_BATCH) {
            throw new IllegalArgumentException("Batch too large (maximum " + MAX_BATCH + ")");
        }

        MlApi.Response response;
        try {
            response = restClient.post()
                    .uri("/predict")
                    .header("X-Internal-Token", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new MlApi.Request(customers))
                    .retrieve()
                    .body(MlApi.Response.class);
        } catch (RestClientException e) {
            log.error("ML service call failed: {}", e.getClass().getSimpleName());
            throw new MlServiceException("ML service call failed");
        }

        if (response == null || response.predictions() == null
                || response.predictions().size() != customers.size()) {
            log.error("ML service returned an unexpected number of predictions");
            throw new MlServiceException("Unexpected ML service response");
        }
        return response;
    }
}