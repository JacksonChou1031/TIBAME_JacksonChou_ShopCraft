package com.jackson.ecommerce.product.service;

import com.jackson.ecommerce.common.web.BadRequestException;
import com.jackson.ecommerce.common.web.ForbiddenException;
import com.jackson.ecommerce.common.web.NotFoundException;
import com.jackson.ecommerce.common.web.UnauthorizedException;
import com.jackson.ecommerce.member.domain.Member;
import com.jackson.ecommerce.member.service.MemberService;
import com.jackson.ecommerce.product.api.ProductRequest;
import com.jackson.ecommerce.product.domain.Product;
import com.jackson.ecommerce.product.domain.ProductImage;
import com.jackson.ecommerce.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Service
public class ProductService {
    private static final int DEFAULT_PAGE_SIZE = 12;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_IMAGES = 5;

    private final ProductRepository productRepository;
    private final MemberService memberService;
    private final ProductImageStorage imageStorage;

    public ProductService(ProductRepository productRepository, MemberService memberService,
                           ProductImageStorage imageStorage) {
        this.productRepository = productRepository;
        this.memberService = memberService;
        this.imageStorage = imageStorage;
    }

    @Transactional
    public Product create(long sellerId, ProductRequest request) {
        memberService.requireActive(sellerId);
        long productId = productRepository.insert(
                sellerId, request.name().trim(), request.description().trim(), request.price(), request.stock(),
                request.category().trim());
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalStateException("Created product could not be loaded"));
    }

    @Transactional
    public Product update(long memberId, long productId, ProductRequest request) {
        requireOwner(memberId, productId);
        if (productRepository.update(productId, memberId, request.name().trim(), request.description().trim(),
                request.price(), request.stock(), request.category().trim()) != 1) {
            throw new NotFoundException("Product was not found");
        }
        return productRepository.findById(productId).orElseThrow(() -> new NotFoundException("Product was not found"));
    }

    @Transactional
    public void delete(long memberId, long productId) {
        requireOwner(memberId, productId);
        if (productRepository.softDelete(productId, memberId) != 1) {
            throw new NotFoundException("Product was not found");
        }
    }

    @Transactional
    public void adminDelete(long productId) {
        if (productRepository.softDeleteById(productId) != 1) {
            throw new NotFoundException("Product was not found");
        }
    }

    public Product getPublic(long productId) {
        return productRepository.findPublicById(productId)
                .orElseThrow(() -> new NotFoundException("Product was not found"));
    }

    public ProductRepository.ProductPage listPublic(String keyword, String category, String sort, int page, int size) {
        validatePage(page, size);
        if (!"newest".equals(sort) && !"price_asc".equals(sort) && !"price_desc".equals(sort)) {
            throw new BadRequestException("sort must be newest, price_asc or price_desc");
        }
        return productRepository.findPublic(keyword, category, sort, page, size);
    }

    public ProductRepository.ProductPage listMine(long memberId, int page, int size) {
        validatePage(page, size);
        return productRepository.findBySeller(memberId, page, size);
    }

    public ProductRepository.ProductPage listAll(int page, int size) {
        validatePage(page, size);
        return productRepository.findAll(page, size);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ProductImage addImage(long memberId, long productId, MultipartFile file) {
        requireOwner(memberId, productId);
        if (productRepository.countImages(productId) >= MAX_IMAGES) {
            throw new BadRequestException("A product can have at most 5 images");
        }
        ProductImageStorage.StoredImage stored = imageStorage.save(file);
        try {
            long imageId = productRepository.insertImage(productId, stored.storageKey(), stored.originalFilename(),
                    stored.mediaType(), stored.fileSize(), productRepository.countImages(productId));
            return productRepository.findImage(productId, imageId)
                    .orElseThrow(() -> new IllegalStateException("Uploaded image could not be loaded"));
        } catch (RuntimeException exception) {
            imageStorage.delete(stored.storageKey());
            throw exception;
        }
    }

    @Transactional
    public void deleteImage(long memberId, long productId, long imageId) {
        requireOwner(memberId, productId);
        ProductImage image = productRepository.findImage(productId, imageId)
                .orElseThrow(() -> new NotFoundException("Image was not found"));
        if (productRepository.deleteImage(productId, imageId) != 1) {
            throw new NotFoundException("Image was not found");
        }
        imageStorage.delete(image.storageKey());
    }

    public ProductImage imageForViewer(long productId, long imageId,
                                       com.jackson.ecommerce.security.MemberPrincipal principal) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product was not found"));
        boolean admin = principal != null && principal.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        boolean owner = principal != null && principal.memberId() == product.sellerId();
        if (!product.isPubliclyVisible() && !admin && !owner) {
            if (principal == null) {
                throw new UnauthorizedException("Login required to view this image");
            }
            throw new ForbiddenException("You cannot view this product image");
        }
        return productRepository.findImage(productId, imageId)
                .orElseThrow(() -> new NotFoundException("Image was not found"));
    }

    public InputStream openImage(ProductImage image) {
        return imageStorage.open(image.storageKey());
    }

    private Product requireOwner(long memberId, long productId) {
        Member member = memberService.requireActive(memberId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product was not found"));
        if (product.sellerId() != member.id()) {
            throw new ForbiddenException("You can only manage your own products");
        }
        if (product.deleted()) {
            throw new NotFoundException("Product was not found");
        }
        return product;
    }

    private void validatePage(int page, int size) {
        if (page < 1 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("page must be >= 1 and size must be between 1 and 50");
        }
    }

    public static int defaultPageSize() {
        return DEFAULT_PAGE_SIZE;
    }
}
