package com.churnguard.backend.ml;

import com.churnguard.backend.domain.Customer;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/** Contrat d'échange avec le service ML : les noms JSON sont ceux de l'API Python. */
public final class MlApi {

    private MlApi() {
    }

    public record Request(List<MlCustomer> customers) {
    }

    public record MlCustomer(
            @JsonProperty("tenure") int tenure,
            @JsonProperty("MonthlyCharges") BigDecimal monthlyCharges,
            @JsonProperty("TotalCharges") BigDecimal totalCharges,
            @JsonProperty("PhoneService") String phoneService,
            @JsonProperty("MultipleLines") String multipleLines,
            @JsonProperty("InternetService") String internetService,
            @JsonProperty("OnlineSecurity") String onlineSecurity,
            @JsonProperty("OnlineBackup") String onlineBackup,
            @JsonProperty("DeviceProtection") String deviceProtection,
            @JsonProperty("TechSupport") String techSupport,
            @JsonProperty("StreamingTV") String streamingTv,
            @JsonProperty("StreamingMovies") String streamingMovies,
            @JsonProperty("Contract") String contract,
            @JsonProperty("PaperlessBilling") String paperlessBilling,
            @JsonProperty("PaymentMethod") String paymentMethod) {

        public static MlCustomer from(Customer c) {
            return new MlCustomer(
                    c.getTenureMonths(), c.getMonthlyCharges(), c.getTotalCharges(),
                    yesNo(c.isPhoneService()), c.getMultipleLines(), c.getInternetService(),
                    c.getOnlineSecurity(), c.getOnlineBackup(), c.getDeviceProtection(),
                    c.getTechSupport(), c.getStreamingTv(), c.getStreamingMovies(),
                    c.getContract(), yesNo(c.isPaperlessBilling()), c.getPaymentMethod());
        }

        private static String yesNo(boolean value) {
            return value ? "Yes" : "No";
        }
    }

    public record Response(
            @JsonProperty("threshold") BigDecimal threshold,
            @JsonProperty("predictions") List<Prediction> predictions) {
    }

    public record Prediction(
            @JsonProperty("probability") BigDecimal probability,
            @JsonProperty("at_risk") boolean atRisk,
            @JsonProperty("factors") List<Factor> factors) {
    }

    public record Factor(
            @JsonProperty("theme") String theme,
            @JsonProperty("impact") BigDecimal impact,
            @JsonProperty("direction") String direction) {
    }
}