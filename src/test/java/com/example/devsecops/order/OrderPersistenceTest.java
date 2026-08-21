package com.example.devsecops.order;

import com.example.devsecops.order.domain.OrderStatus;
import com.example.devsecops.order.web.CreateOrderRequest;
import com.example.devsecops.order.web.OrderLineRequest;
import com.example.devsecops.order.web.OrderResponse;
import com.example.devsecops.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class OrderPersistenceTest extends AbstractIntegrationTest {



    @Autowired
    private OrderService orderService;

    private static final UUID USER    = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID CLAVIER = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void verify_order_is_created() {
        OrderResponse cree = orderService.create(
                new CreateOrderRequest(USER, List.of(new OrderLineRequest(CLAVIER, 2)))
        );

        OrderResponse relu= orderService.findById(cree.orderId());

        assertThat(relu.orderId()).isEqualTo(cree.orderId());
        assertThat(relu.items()).hasSize(1);
        assertThat(relu.totalAmount()).isEqualByComparingTo("179.80");
    }

    @Test
    void change_status_order_to_confirmed() {
        OrderResponse cree = orderService.create(
                new CreateOrderRequest(USER, List.of(new OrderLineRequest(CLAVIER, 2)))
        );

        orderService.confirm(cree.orderId());

        OrderResponse relu= orderService.findById(cree.orderId());

        assertThat(relu.status()).isEqualTo(OrderStatus.CONFIRMED);
    }
}
