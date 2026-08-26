package com.jackson.ecommerce.product.api;

import com.jackson.ecommerce.product.domain.ProductImage;
import com.jackson.ecommerce.product.service.ProductService;
import com.jackson.ecommerce.common.web.ApiErrorResponse;
import com.jackson.ecommerce.security.MemberPrincipal;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Public product catalog and member product APIs")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ProductPageResponse list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        return pageResponse(productService.listPublic(keyword, category, sort, page, size), page, size);
    }

    @GetMapping("/{productId}")
    public ProductResponse get(@PathVariable long productId) {
        return ProductResponse.from(productService.getPublic(productId));
    }

    @PostMapping
    @SecurityRequirement(name = "cookieAuth")
    @ApiResponse(responseCode = "201", description = "Product created")
    @ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<ProductResponse> create(@AuthenticationPrincipal MemberPrincipal principal,
                                                   @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(201).body(ProductResponse.from(productService.create(principal.memberId(), request)));
    }

    @PutMapping("/{productId}")
    @SecurityRequirement(name = "cookieAuth")
    public ProductResponse update(@AuthenticationPrincipal MemberPrincipal principal,
                                  @PathVariable long productId,
                                  @Valid @RequestBody ProductRequest request) {
        return ProductResponse.from(productService.update(principal.memberId(), productId, request));
    }

    @DeleteMapping("/{productId}")
    @SecurityRequirement(name = "cookieAuth")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal MemberPrincipal principal,
                                       @PathVariable long productId) {
        productService.delete(principal.memberId(), productId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{productId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SecurityRequirement(name = "cookieAuth")
    public ResponseEntity<ProductImageUploadResponse> uploadImage(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable long productId,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(201).body(
                ProductImageUploadResponse.from(productService.addImage(principal.memberId(), productId, file)));
    }

    @DeleteMapping("/{productId}/images/{imageId}")
    @SecurityRequirement(name = "cookieAuth")
    public ResponseEntity<Void> deleteImage(@AuthenticationPrincipal MemberPrincipal principal,
                                            @PathVariable long productId,
                                            @PathVariable long imageId) {
        productService.deleteImage(principal.memberId(), productId, imageId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{productId}/images/{imageId}")
    public ResponseEntity<Resource> image(@PathVariable long productId, @PathVariable long imageId,
                                          @AuthenticationPrincipal MemberPrincipal principal) {
        ProductImage image = productService.imageForViewer(productId, imageId, principal);
        InputStream inputStream = productService.openImage(image);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.mediaType()))
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + image.originalFilename() + "\"")
                .body(new InputStreamResource(inputStream));
    }

    static ProductPageResponse pageResponse(com.jackson.ecommerce.product.repository.ProductRepository.ProductPage page,
                                            int pageNumber, int size) {
        int totalPages = page.totalItems() == 0 ? 0 : (int) ((page.totalItems() + size - 1) / size);
        return new ProductPageResponse(page.items().stream().map(ProductResponse::from).toList(), pageNumber, size,
                page.totalItems(), totalPages);
    }
}
