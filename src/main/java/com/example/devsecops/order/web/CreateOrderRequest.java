package com.example.devsecops.order.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        @NotNull(message = "userRef is required")
        UUID userRef,

        // @NotEmpty et pas @Positive : @Positive ne s'applique qu'aux nombres,
        // sur une List il etait silencieusement ignore.
        // @Valid fait descendre la validation dans chaque OrderLineRequest ;
        // sans lui, les contraintes des lignes ne sont jamais evaluees.
        @NotEmpty(message = "An order must contain at least one line")
        List<@Valid OrderLineRequest> items
) {
}
