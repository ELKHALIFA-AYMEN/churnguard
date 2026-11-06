package com.churnguard.backend.exception;

public class MlServiceException extends RuntimeException {

    public MlServiceException(String message) {
        super(message);
    }
}