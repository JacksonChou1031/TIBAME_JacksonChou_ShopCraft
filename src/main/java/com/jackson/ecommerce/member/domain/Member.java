package com.jackson.ecommerce.member.domain;

public record Member(
        long id,
        String email,
        String username,
        String passwordHash,
        String displayName,
        String phone,
        MemberRole role,
        AccountStatus accountStatus,
        boolean mustChangePassword
) {
    public boolean isActive() {
        return accountStatus == AccountStatus.ACTIVE;
    }
}
