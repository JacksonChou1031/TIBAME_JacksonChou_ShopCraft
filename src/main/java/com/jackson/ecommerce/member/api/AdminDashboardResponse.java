package com.jackson.ecommerce.member.api;

public record AdminDashboardResponse(long memberCount, long productCount, long orderCount) {
}
