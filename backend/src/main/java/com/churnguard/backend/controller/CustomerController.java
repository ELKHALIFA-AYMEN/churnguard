package com.churnguard.backend.controller;

import com.churnguard.backend.dto.ImportReport;
import com.churnguard.backend.exception.InvalidImportFileException;
import com.churnguard.backend.service.CustomerImportService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerImportService importService;

    public CustomerController(CustomerImportService importService) {
        this.importService = importService;
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ImportReport importCustomers(@AuthenticationPrincipal Jwt jwt,
                                        @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new InvalidImportFileException("The file is empty");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new InvalidImportFileException("Only .csv files are accepted");
        }

        UUID organizationId = UUID.fromString(jwt.getClaimAsString("org"));

        try (InputStream input = file.getInputStream()) {
            return importService.importCustomers(organizationId, input);
        }
    }
}