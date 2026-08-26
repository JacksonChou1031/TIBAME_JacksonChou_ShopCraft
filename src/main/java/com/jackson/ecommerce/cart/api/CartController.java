package com.jackson.ecommerce.cart.api;

import com.jackson.ecommerce.cart.service.CartService;
import com.jackson.ecommerce.security.MemberPrincipal;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
@Tag(name = "Cart", description = "Member shopping cart APIs")
@SecurityRequirement(name = "cookieAuth")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse get(@AuthenticationPrincipal MemberPrincipal principal) {
        return cartService.get(principal.memberId());
    }

    @PostMapping("/items")
    public CartResponse add(@AuthenticationPrincipal MemberPrincipal principal,
                            @Valid @RequestBody AddCartItemRequest request) {
        return cartService.add(principal.memberId(), request);
    }

    @PatchMapping("/items/{productId}")
    public CartResponse update(@AuthenticationPrincipal MemberPrincipal principal,
                               @PathVariable long productId,
                               @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.update(principal.memberId(), productId, request);
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal MemberPrincipal principal,
                                       @PathVariable long productId) {
        cartService.remove(principal.memberId(), productId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clear(@AuthenticationPrincipal MemberPrincipal principal) {
        cartService.clear(principal.memberId());
        return ResponseEntity.noContent().build();
    }
}
