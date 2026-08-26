package com.jackson.ecommerce.member.service;

import com.jackson.ecommerce.member.repository.MemberRepository;
import com.jackson.ecommerce.member.domain.MemberRole;
import com.jackson.ecommerce.security.InitialAdminProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class InitialAdminInitializer implements ApplicationRunner {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final InitialAdminProperties properties;

    public InitialAdminInitializer(MemberRepository memberRepository, PasswordEncoder passwordEncoder,
                                    InitialAdminProperties properties) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        var existingAdmin = memberRepository.findByLogin(properties.getEmail());
        if (existingAdmin.isPresent()) {
            if (existingAdmin.get().role() != MemberRole.ADMIN) {
                throw new IllegalStateException("Configured initial admin email belongs to a non-admin member");
            }
            return;
        }
        try {
            memberRepository.insert(
                    MemberRepository.normalize(properties.getEmail()),
                    MemberRepository.normalize(properties.getUsername()),
                    passwordEncoder.encode(properties.getPassword()),
                    properties.getDisplayName().trim(),
                    properties.getPhone().trim(),
                    MemberRole.ADMIN,
                    false);
        } catch (DuplicateKeyException exception) {
            // A concurrent startup may have created the configured admin already.
            var concurrentAdmin = memberRepository.findByLogin(properties.getEmail());
            if (concurrentAdmin.isEmpty() || concurrentAdmin.get().role() != MemberRole.ADMIN) {
                throw new IllegalStateException("Could not create the configured initial admin", exception);
            }
        }
    }
}
