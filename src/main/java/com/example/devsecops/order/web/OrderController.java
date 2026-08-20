package com.example.devsecops.order.web;

import com.example.devsecops.order.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody @Valid CreateOrderRequest request,
                                                     UriComponentsBuilder uriBuilder) {
        OrderResponse created = orderService.create(request);

        URI location = uriBuilder.path("/api/v1/orders/{id}")
                .buildAndExpand(created.orderId())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    // TODO SECURITY: verifier que l'utilisateur authentifie est bien
    // le proprietaire de la commande (OWASP API1:2023 - BOLA)
    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable UUID id) {
        return orderService.findById(id);
    }

    @PostMapping("/{id}/confirm")
    @ResponseStatus(HttpStatus.OK)
    public OrderResponse confirmOrder(@PathVariable UUID id) {
        return orderService.confirm(id);
    }

    @PostMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.OK)
    public OrderResponse cancelOrder(@PathVariable UUID id) {
        return orderService.cancel(id);
    }
}
