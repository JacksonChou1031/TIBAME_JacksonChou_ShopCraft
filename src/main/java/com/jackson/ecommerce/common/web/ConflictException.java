package com.jackson.ecommerce.common.web;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
