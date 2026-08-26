package com.jackson.ecommerce.product.api;

import com.jackson.ecommerce.product.service.ProductService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/products")
@Tag(name = "Admin Products", description = "Administrator product moderation APIs")
@SecurityRequirement(name = "cookieAuth", scopes = {"ADMIN"})
public class AdminProductController {
    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ProductPageResponse listAll(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "12") int size) {
        return ProductController.pageResponse(productService.listAll(page, size), page, size);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> delete(@PathVariable long productId) {
        productService.adminDelete(productId);
        return ResponseEntity.noContent().build();
    }
}
