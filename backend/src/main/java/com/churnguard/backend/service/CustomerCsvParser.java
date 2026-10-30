package com.churnguard.backend.service;

import com.churnguard.backend.dto.ImportReport.RowError;
import com.churnguard.backend.exception.InvalidImportFileException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class CustomerCsvParser {

    public static final int MAX_ROWS = 100_000;
    private static final int MAX_REPORTED_ERRORS = 100;

    private static final List<String> REQUIRED_COLUMNS = List.of(
            "customerID", "tenure", "PhoneService", "MultipleLines", "InternetService",
            "OnlineSecurity", "OnlineBackup", "DeviceProtection", "TechSupport",
            "StreamingTV", "StreamingMovies", "Contract", "PaperlessBilling",
            "PaymentMethod", "MonthlyCharges");

    private static final Set<String> YES_NO = Set.of("Yes", "No");
    private static final Set<String> MULTIPLE_LINES = Set.of("Yes", "No", "No phone service");
    private static final Set<String> INTERNET_SERVICE = Set.of("DSL", "Fiber optic", "No");
    private static final Set<String> INTERNET_ADDON = Set.of("Yes", "No", "No internet service");
    private static final Set<String> CONTRACT = Set.of("Month-to-month", "One year", "Two year");
    private static final Set<String> PAYMENT_METHOD = Set.of(
            "Electronic check", "Mailed check", "Bank transfer (automatic)", "Credit card (automatic)");

    private static final Pattern MONEY = Pattern.compile("\\d{1,10}(\\.\\d{1,6})?");
    private static final BigDecimal MAX_MONTHLY = new BigDecimal("99999999.99");
    private static final BigDecimal MAX_TOTAL = new BigDecimal("9999999999.99");

    public record ParsedCustomer(
            String externalId,
            int tenureMonths,
            boolean phoneService,
            String multipleLines,
            String internetService,
            String onlineSecurity,
            String onlineBackup,
            String deviceProtection,
            String techSupport,
            String streamingTv,
            String streamingMovies,
            String contract,
            boolean paperlessBilling,
            String paymentMethod,
            BigDecimal monthlyCharges,
            BigDecimal totalCharges,
            Boolean churned) {
    }

    public record ParseResult(
            int totalRows,
            List<ParsedCustomer> customers,
            int rejected,
            List<RowError> errors) {
    }

    public ParseResult parse(InputStream input) {
        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            skipBom(reader);

            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setTrim(true)
                    .setIgnoreEmptyLines(true)
                    .get();

            try (CSVParser parser = format.parse(reader)) {
                Map<String, Integer> headers = parser.getHeaderMap();
                if (headers == null || headers.isEmpty()) {
                    throw new InvalidImportFileException("The file is empty or has no header");
                }

                List<String> missing = REQUIRED_COLUMNS.stream()
                        .filter(column -> !headers.containsKey(column))
                        .toList();
                if (!missing.isEmpty()) {
                    throw new InvalidImportFileException(
                            "Missing required columns: " + String.join(", ", missing));
                }

                List<ParsedCustomer> customers = new ArrayList<>();
                List<RowError> errors = new ArrayList<>();
                Set<String> seenIds = new HashSet<>();
                int total = 0;
                int rejected = 0;

                for (CSVRecord record : parser) {
                    total++;
                    if (total > MAX_ROWS) {
                        throw new InvalidImportFileException(
                                "Too many rows (maximum " + MAX_ROWS + ")");
                    }
                    try {
                        ParsedCustomer customer = parseRow(record);
                        if (!seenIds.add(customer.externalId())) {
                            throw new IllegalArgumentException("Duplicate customerID in file");
                        }
                        customers.add(customer);
                    } catch (IllegalArgumentException e) {
                        rejected++;
                        if (errors.size() < MAX_REPORTED_ERRORS) {
                            errors.add(new RowError(total + 1L, e.getMessage()));
                        }
                    }
                }

                return new ParseResult(total, customers, rejected, errors);
            }
        } catch (IOException | UncheckedIOException e) {
            throw new InvalidImportFileException("The file is unreadable or malformed");
        }
    }

    private ParsedCustomer parseRow(CSVRecord r) {
        if (!r.isConsistent()) {
            throw new IllegalArgumentException(
                    "Row has a different number of columns than the header");
        }

        String externalId = value(r, "customerID");
        if (externalId.isEmpty() || externalId.length() > 100) {
            throw invalid("customerID");
        }

        int tenure = parseTenure(value(r, "tenure"));

        String totalRaw = value(r, "TotalCharges");
        BigDecimal totalCharges =
                totalRaw.isEmpty() ? null : money(totalRaw, "TotalCharges", MAX_TOTAL);

        String churnRaw = value(r, "Churn");
        Boolean churned = null;
        if (!churnRaw.isEmpty()) {
            if (!YES_NO.contains(churnRaw)) {
                throw invalid("Churn");
            }
            churned = churnRaw.equals("Yes");
        }

        return new ParsedCustomer(
                externalId,
                tenure,
                yesNo(r, "PhoneService"),
                oneOf(r, "MultipleLines", MULTIPLE_LINES),
                oneOf(r, "InternetService", INTERNET_SERVICE),
                oneOf(r, "OnlineSecurity", INTERNET_ADDON),
                oneOf(r, "OnlineBackup", INTERNET_ADDON),
                oneOf(r, "DeviceProtection", INTERNET_ADDON),
                oneOf(r, "TechSupport", INTERNET_ADDON),
                oneOf(r, "StreamingTV", INTERNET_ADDON),
                oneOf(r, "StreamingMovies", INTERNET_ADDON),
                oneOf(r, "Contract", CONTRACT),
                yesNo(r, "PaperlessBilling"),
                oneOf(r, "PaymentMethod", PAYMENT_METHOD),
                money(value(r, "MonthlyCharges"), "MonthlyCharges", MAX_MONTHLY),
                totalCharges,
                churned);
    }

    private static void skipBom(BufferedReader reader) throws IOException {
        reader.mark(1);
        if (reader.read() != 0xFEFF) {
            reader.reset();
        }
    }

    private static String value(CSVRecord r, String column) {
        if (!r.isMapped(column) || !r.isSet(column)) {
            return "";
        }
        String v = r.get(column);
        return v == null ? "" : v.trim();
    }

    private static String oneOf(CSVRecord r, String column, Set<String> allowed) {
        String v = value(r, column);
        if (!allowed.contains(v)) {
            throw invalid(column);
        }
        return v;
    }

    private static boolean yesNo(CSVRecord r, String column) {
        return oneOf(r, column, YES_NO).equals("Yes");
    }

    private static int parseTenure(String raw) {
        int tenure;
        try {
            tenure = Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw invalid("tenure");
        }
        if (tenure < 0 || tenure > 1200) {
            throw invalid("tenure");
        }
        return tenure;
    }

    private static BigDecimal money(String raw, String column, BigDecimal max) {
        if (!MONEY.matcher(raw).matches()) {
            throw invalid(column);
        }
        BigDecimal amount = new BigDecimal(raw).setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(max) > 0) {
            throw invalid(column);
        }
        return amount;
    }

    private static IllegalArgumentException invalid(String column) {
        return new IllegalArgumentException("Invalid or missing value for column " + column);
    }
}