package com.example.devsecops.order.web;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderLineResponse(
        UUID productId,
        String productName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal
) {
}
