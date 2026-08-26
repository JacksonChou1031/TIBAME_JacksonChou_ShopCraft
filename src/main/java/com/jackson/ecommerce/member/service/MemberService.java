package com.jackson.ecommerce.member.service;

import com.jackson.ecommerce.common.web.ConflictException;
import com.jackson.ecommerce.common.web.UnauthorizedException;
import com.jackson.ecommerce.member.api.ChangePasswordRequest;
import com.jackson.ecommerce.member.api.LoginRequest;
import com.jackson.ecommerce.member.api.RegisterRequest;
import com.jackson.ecommerce.member.api.UpdateProfileRequest;
import com.jackson.ecommerce.member.domain.Member;
import com.jackson.ecommerce.member.domain.MemberRole;
import com.jackson.ecommerce.member.repository.MemberRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Member register(RegisterRequest request) {
        String email = MemberRepository.normalize(request.email());
        String username = MemberRepository.normalize(request.username());
        try {
            long memberId = memberRepository.insert(
                    email,
                    username,
                    passwordEncoder.encode(request.password()),
                    request.displayName().trim(),
                    request.phone().trim(),
                    MemberRole.MEMBER,
                    false);
            return memberRepository.findById(memberId).orElseThrow(
                    () -> new IllegalStateException("Registered member could not be loaded"));
        } catch (DuplicateKeyException exception) {
            throw new ConflictException("Email or username is already registered");
        }
    }

    public Member authenticate(LoginRequest request) {
        Member member = memberRepository.findByLogin(request.identifier())
                .filter(Member::isActive)
                .orElseThrow(() -> new UnauthorizedException("Invalid login credentials"));
        if (!passwordEncoder.matches(request.password(), member.passwordHash())) {
            throw new UnauthorizedException("Invalid login credentials");
        }
        return member;
    }

    public Member requireActive(long memberId) {
        return memberRepository.findById(memberId)
                .filter(Member::isActive)
                .orElseThrow(() -> new UnauthorizedException("Member is not authenticated"));
    }

    @Transactional
    public void changePassword(long memberId, ChangePasswordRequest request) {
        Member member = requireActive(memberId);
        if (!passwordEncoder.matches(request.currentPassword(), member.passwordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw new ConflictException("New password must be different from current password");
        }
        memberRepository.updatePassword(memberId, passwordEncoder.encode(request.newPassword()));
    }

    @Transactional
    public Member updateProfile(long memberId, UpdateProfileRequest request) {
        requireActive(memberId);
        if (memberRepository.updateProfile(memberId, request.displayName().trim(), request.phone().trim()) != 1) {
            throw new UnauthorizedException("Member is not authenticated");
        }
        return requireActive(memberId);
    }
}
