package com.example.devsecops.order.exception;

/**
 * Exception applicative (pas metier) : la ressource demandee n'existe pas.
 * Traduite en 404 par le GlobalExceptionHandler.
 */
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String message) {
        super(message);
    }
}
