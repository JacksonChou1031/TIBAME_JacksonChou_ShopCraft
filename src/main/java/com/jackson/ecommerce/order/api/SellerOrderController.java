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
@RequestMapping("/api/v1/seller/orders")
@Tag(name = "Seller Orders", description = "Seller order preparation and shipment APIs")
@SecurityRequirement(name = "cookieAuth")
public class SellerOrderController {
    private final OrderService orderService;

    public SellerOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public OrderListResponse list(@AuthenticationPrincipal MemberPrincipal principal) {
        return OrderListResponse.from(orderService.listForSeller(principal.memberId()));
    }

    @GetMapping("/{orderId}")
    public OrderResponse get(@AuthenticationPrincipal MemberPrincipal principal, @PathVariable long orderId) {
        return orderService.getForSeller(principal.memberId(), orderId);
    }

    @PostMapping("/{orderId}/prepare-shipment")
    public ResponseEntity<Void> prepare(@AuthenticationPrincipal MemberPrincipal principal,
                                        @PathVariable long orderId) {
        orderService.prepareShipment(principal.memberId(), orderId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{orderId}/ship")
    public OrderResponse ship(@AuthenticationPrincipal MemberPrincipal principal, @PathVariable long orderId,
                              @Valid @RequestBody ShipmentRequest request) {
        return orderService.ship(principal.memberId(), orderId, request);
    }
}
