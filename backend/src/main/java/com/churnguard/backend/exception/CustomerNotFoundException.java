package com.churnguard.backend.exception;

public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException() {
        super("Customer not found");
    }
}