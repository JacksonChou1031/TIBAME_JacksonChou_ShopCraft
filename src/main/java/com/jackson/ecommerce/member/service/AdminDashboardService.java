package com.jackson.ecommerce.member.service;

import com.jackson.ecommerce.member.api.AdminDashboardResponse;
import com.jackson.ecommerce.member.repository.MemberRepository;
import com.jackson.ecommerce.order.repository.OrderRepository;
import com.jackson.ecommerce.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDashboardService {
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public AdminDashboardService(MemberRepository memberRepository, ProductRepository productRepository,
                                 OrderRepository orderRepository) {
        this.memberRepository = memberRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse getSummary() {
        return new AdminDashboardResponse(
                memberRepository.countAll(), productRepository.countAll(), orderRepository.countAll());
    }
}
