package com.churnguard.backend;

import com.churnguard.backend.exception.InvalidImportFileException;
import com.churnguard.backend.service.CustomerCsvParser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerCsvParserTest {

    private static final String HEADER =
            "customerID,gender,SeniorCitizen,Partner,Dependents,tenure,PhoneService,MultipleLines,"
                    + "InternetService,OnlineSecurity,OnlineBackup,DeviceProtection,TechSupport,"
                    + "StreamingTV,StreamingMovies,Contract,PaperlessBilling,PaymentMethod,"
                    + "MonthlyCharges,TotalCharges,Churn\n";

    private final CustomerCsvParser parser = new CustomerCsvParser();

    private static String row(String id, String tenure, String contract, String totalCharges) {
        return id + ",Female,0,Yes,No," + tenure
                + ",No,No phone service,DSL,No,Yes,No,No,No,No,"
                + contract + ",Yes,Electronic check,29.85," + totalCharges + ",No\n";
    }

    private CustomerCsvParser.ParseResult parse(String csv) {
        return parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void validRowIsParsed() {
        var result = parse(HEADER + row("TEST-0001", "12", "Month-to-month", "350.50"));

        assertThat(result.totalRows()).isEqualTo(1);
        assertThat(result.rejected()).isZero();

        var customer = result.customers().get(0);
        assertThat(customer.externalId()).isEqualTo("TEST-0001");
        assertThat(customer.tenureMonths()).isEqualTo(12);
        assertThat(customer.monthlyCharges()).isEqualByComparingTo("29.85");
        assertThat(customer.totalCharges()).isEqualByComparingTo("350.50");
        assertThat(customer.churned()).isFalse();
    }

    @Test
    void blankTotalChargesBecomesNull() {
        var result = parse(HEADER + row("TEST-0002", "0", "Month-to-month", " "));

        assertThat(result.rejected()).isZero();
        assertThat(result.customers().get(0).totalCharges()).isNull();
    }

    @Test
    void missingRequiredColumnRejectsTheWholeFile() {
        String header = HEADER.replace(",Contract", "");

        assertThatThrownBy(() -> parse(header + "TEST-0001\n"))
                .isInstanceOf(InvalidImportFileException.class)
                .hasMessageContaining("Contract");
    }

    @Test
    void invalidRowIsRejectedAndOthersAreKept() {
        String csv = HEADER
                + row("TEST-0001", "12", "Month-to-month", "100.00")
                + row("TEST-0002", "12", "Bad-Contract-Value", "100.00")
                + row("TEST-0003", "12", "Two year", "100.00");

        var result = parse(csv);

        assertThat(result.customers()).hasSize(2);
        assertThat(result.rejected()).isEqualTo(1);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).line()).isEqualTo(3);
        assertThat(result.errors().get(0).message())
                .contains("Contract")
                .doesNotContain("Bad-Contract-Value");
    }

    @Test
    void duplicateIdInFileIsRejected() {
        String csv = HEADER
                + row("TEST-0001", "12", "Two year", "100.00")
                + row("TEST-0001", "24", "One year", "200.00");

        var result = parse(csv);

        assertThat(result.customers()).hasSize(1);
        assertThat(result.rejected()).isEqualTo(1);
    }

    @Test
    void negativeTenureIsRejected() {
        var result = parse(HEADER + row("TEST-0001", "-5", "Two year", "100.00"));

        assertThat(result.customers()).isEmpty();
        assertThat(result.rejected()).isEqualTo(1);
    }

    @Test
    void booby_trappedNumberIsRejected() {
        var result = parse(HEADER + row("TEST-0001", "1", "Two year", "1E999999"));

        assertThat(result.customers()).isEmpty();
        assertThat(result.rejected()).isEqualTo(1);
    }
}