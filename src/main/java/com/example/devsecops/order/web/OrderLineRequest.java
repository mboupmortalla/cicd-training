package com.example.devsecops.order.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * productId + quantity, et RIEN d'autre.
 * Ni nom ni prix : ils viennent du catalogue, cote serveur.
 */
public record OrderLineRequest(
        @NotNull(message = "productId is required")
        UUID productId,

        @Min(value = 1, message = "quantity must be greater than 0")
        int quantity
) {
}
