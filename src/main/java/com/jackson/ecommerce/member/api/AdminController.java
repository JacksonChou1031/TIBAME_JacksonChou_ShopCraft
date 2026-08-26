package com.jackson.ecommerce.member.api;

import com.jackson.ecommerce.security.MemberPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal MemberPrincipal principal) {
        return Map.of(
                "memberId", principal.memberId(),
                "username", principal.getUsername(),
                "role", "ADMIN");
    }
}
