package com.jackson.ecommerce.security;

import com.jackson.ecommerce.member.domain.Member;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class MemberPrincipal implements UserDetails {
    private final Member member;

    public MemberPrincipal(Member member) {
        this.member = member;
    }

    public long memberId() {
        return member.id();
    }

    public Member member() {
        return member;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + member.role().name()));
    }

    @Override
    public String getPassword() {
        return member.passwordHash();
    }

    @Override
    public String getUsername() {
        return member.username();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return member.isActive();
    }
}
