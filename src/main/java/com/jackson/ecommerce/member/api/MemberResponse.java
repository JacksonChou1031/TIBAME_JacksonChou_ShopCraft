package com.jackson.ecommerce.member.api;

import com.jackson.ecommerce.member.domain.Member;
import com.jackson.ecommerce.member.domain.MemberRole;

public record MemberResponse(
        long id,
        String email,
        String username,
        String displayName,
        String phone,
        MemberRole role,
        boolean mustChangePassword
) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.id(),
                member.email(),
                member.username(),
                member.displayName(),
                member.phone(),
                member.role(),
                member.mustChangePassword());
    }
}
