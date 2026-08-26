package com.jackson.ecommerce.member.api;

import com.jackson.ecommerce.security.MemberPrincipal;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.jackson.ecommerce.member.service.AdminDashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin", description = "Administrator dashboard and administration APIs")
@SecurityRequirement(name = "cookieAuth", scopes = {"ADMIN"})
public class AdminController {
    private final AdminDashboardService dashboardService;

    public AdminController(AdminDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public AdminDashboardResponse dashboard() {
        return dashboardService.getSummary();
    }

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal MemberPrincipal principal) {
        return Map.of(
                "memberId", principal.memberId(),
                "username", principal.getUsername(),
                "role", "ADMIN");
    }
}
