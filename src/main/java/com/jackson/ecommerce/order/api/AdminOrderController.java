package com.jackson.ecommerce.order.api;

import com.jackson.ecommerce.order.service.OrderService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/orders")
@Tag(name = "Admin Orders", description = "Administrator order viewing APIs")
@SecurityRequirement(name = "cookieAuth", scopes = {"ADMIN"})
public class AdminOrderController {
    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public OrderListResponse list() {
        return OrderListResponse.from(orderService.listForAdmin());
    }

    @GetMapping("/{orderId}")
    public OrderResponse get(@PathVariable long orderId) {
        return orderService.getForAdmin(orderId);
    }
}
