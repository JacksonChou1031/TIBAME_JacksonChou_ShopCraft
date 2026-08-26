package com.jackson.ecommerce.order.api;

import com.jackson.ecommerce.order.service.OrderService;
import com.jackson.ecommerce.security.MemberPrincipal;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Buyer Orders", description = "Buyer order history and confirmation APIs")
@SecurityRequirement(name = "cookieAuth")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public OrderListResponse list(@AuthenticationPrincipal MemberPrincipal principal) {
        return OrderListResponse.from(orderService.listForBuyer(principal.memberId()));
    }

    @GetMapping("/{orderId}")
    public OrderResponse get(@AuthenticationPrincipal MemberPrincipal principal, @PathVariable long orderId) {
        return orderService.getForBuyer(principal.memberId(), orderId);
    }

    @PostMapping("/{orderId}/confirm")
    public OrderResponse confirm(@AuthenticationPrincipal MemberPrincipal principal, @PathVariable long orderId) {
        return orderService.confirmReceived(principal.memberId(), orderId);
    }

    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancel(@AuthenticationPrincipal MemberPrincipal principal, @PathVariable long orderId) {
        return orderService.cancel(principal.memberId(), orderId);
    }
}
