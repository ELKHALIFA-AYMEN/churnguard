package com.churnguard.backend.controller;

import com.churnguard.backend.dto.CustomerResponse;
import com.churnguard.backend.dto.ImportReport;
import com.churnguard.backend.dto.PageResponse;
import com.churnguard.backend.exception.InvalidImportFileException;
import com.churnguard.backend.service.CustomerImportService;
import com.churnguard.backend.service.CustomerQueryService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final CustomerQueryService queryService;

    public CustomerController(CustomerImportService importService,
                              CustomerQueryService queryService) {
        this.importService = importService;
        this.queryService = queryService;
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

        try (InputStream input = file.getInputStream()) {
            return importService.importCustomers(organizationId(jwt), input);
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'VIEWER')")
    public PageResponse<CustomerResponse> list(@AuthenticationPrincipal Jwt jwt,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "50") int size) {
        return queryService.list(organizationId(jwt), page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'VIEWER')")
    public CustomerResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return queryService.get(organizationId(jwt), id);
    }

    private static UUID organizationId(Jwt jwt) {
        return UUID.fromString(jwt.getClaimAsString("org"));
    }
}