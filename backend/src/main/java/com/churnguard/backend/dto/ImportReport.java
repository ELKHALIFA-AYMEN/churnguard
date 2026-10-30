package com.churnguard.backend.dto;

import java.util.List;

public record ImportReport(
        int totalRows,
        int created,
        int updated,
        int rejected,
        List<RowError> errors
) {

    public record RowError(long line, String message) {
    }
}