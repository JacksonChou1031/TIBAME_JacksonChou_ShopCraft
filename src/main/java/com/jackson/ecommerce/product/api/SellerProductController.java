package com.jackson.ecommerce.product.api;

import com.jackson.ecommerce.product.service.ProductService;
import com.jackson.ecommerce.security.MemberPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seller/products")
public class SellerProductController {
    private final ProductService productService;

    public SellerProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ProductPageResponse listMine(@AuthenticationPrincipal MemberPrincipal principal,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "12") int size) {
        return ProductController.pageResponse(productService.listMine(principal.memberId(), page, size), page, size);
    }
}
