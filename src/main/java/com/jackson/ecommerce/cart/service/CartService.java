package com.jackson.ecommerce.cart.service;

import com.jackson.ecommerce.cart.api.AddCartItemRequest;
import com.jackson.ecommerce.cart.api.CartResponse;
import com.jackson.ecommerce.cart.api.UpdateCartItemRequest;
import com.jackson.ecommerce.cart.domain.CartItem;
import com.jackson.ecommerce.cart.repository.CartRepository;
import com.jackson.ecommerce.common.web.ConflictException;
import com.jackson.ecommerce.common.web.NotFoundException;
import com.jackson.ecommerce.member.service.MemberService;
import com.jackson.ecommerce.product.domain.Product;
import com.jackson.ecommerce.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartService {
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final MemberService memberService;

    public CartService(CartRepository cartRepository, ProductRepository productRepository,
                       MemberService memberService) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.memberService = memberService;
    }

    public CartResponse get(long memberId) {
        memberService.requireActive(memberId);
        CartRepository.CartRow cart = cartRepository.findCart(memberId).orElse(null);
        List<CartItem> items = cartRepository.findItems(memberId);
        return CartResponse.from(cart == null ? null : cart.id(), cart == null ? null : cart.sellerId(), items);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CartResponse add(long memberId, AddCartItemRequest request) {
        memberService.requireActive(memberId);
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new NotFoundException("Product was not found"));
        ensureAddable(product, request.quantity());

        CartRepository.CartRow cart = cartRepository.findCart(memberId).orElse(null);
        if (cart != null && cart.sellerId() != null && cart.sellerId() != product.sellerId()) {
            throw new ConflictException("A cart can contain products from only one seller");
        }
        if (cart == null) {
            long cartId = cartRepository.insertCart(memberId, product.sellerId());
            cart = new CartRepository.CartRow(cartId, memberId, product.sellerId());
        } else if (cart.sellerId() == null) {
            cartRepository.updateSeller(cart.id(), product.sellerId());
        }

        CartRepository.ItemRow existing = cartRepository.findItem(cart.id(), product.id()).orElse(null);
        int newQuantity = existing == null ? request.quantity() : existing.quantity() + request.quantity();
        if (newQuantity > product.stock()) {
            throw new ConflictException("Requested quantity exceeds current stock");
        }
        if (existing == null) {
            cartRepository.insertItem(cart.id(), product.id(), newQuantity);
        } else {
            cartRepository.updateItem(cart.id(), product.id(), newQuantity);
        }
        return get(memberId);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CartResponse update(long memberId, long productId, UpdateCartItemRequest request) {
        memberService.requireActive(memberId);
        CartRepository.CartRow cart = cartRepository.findCart(memberId)
                .orElseThrow(() -> new NotFoundException("Cart item was not found"));
        if (cartRepository.findItem(cart.id(), productId).isEmpty()) {
            throw new NotFoundException("Cart item was not found");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product was not found"));
        if (product.isPubliclyVisible() && request.quantity() > product.stock()) {
            throw new ConflictException("Requested quantity exceeds current stock");
        }
        cartRepository.updateItem(cart.id(), productId, request.quantity());
        return get(memberId);
    }

    @Transactional
    public void remove(long memberId, long productId) {
        memberService.requireActive(memberId);
        CartRepository.CartRow cart = cartRepository.findCart(memberId)
                .orElseThrow(() -> new NotFoundException("Cart item was not found"));
        if (cartRepository.deleteItem(cart.id(), productId) != 1) {
            throw new NotFoundException("Cart item was not found");
        }
        if (cartRepository.countItems(cart.id()) == 0) {
            cartRepository.updateSeller(cart.id(), null);
        }
    }

    @Transactional
    public void clear(long memberId) {
        memberService.requireActive(memberId);
        cartRepository.findCart(memberId).ifPresent(cart -> {
            cartRepository.deleteAllItems(cart.id());
            cartRepository.updateSeller(cart.id(), null);
        });
    }

    private void ensureAddable(Product product, int quantity) {
        if (!product.isPubliclyVisible()) {
            throw new ConflictException("Product is not currently available");
        }
        if (quantity > product.stock()) {
            throw new ConflictException("Requested quantity exceeds current stock");
        }
    }
}
