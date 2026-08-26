package com.jackson.ecommerce.member.api;

import com.jackson.ecommerce.member.domain.Member;
import com.jackson.ecommerce.member.service.MemberService;
import com.jackson.ecommerce.security.JwtService;
import com.jackson.ecommerce.security.MemberPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final MemberService memberService;
    private final JwtService jwtService;

    public AuthController(MemberService memberService, JwtService jwtService) {
        this.memberService = memberService;
        this.jwtService = jwtService;
    }

    @GetMapping("/csrf")
    public ResponseEntity<CsrfResponse> csrf(HttpServletRequest request) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token == null) {
            token = (CsrfToken) request.getAttribute("_csrf");
        }
        if (token == null) {
            throw new IllegalStateException("CSRF token was not initialized");
        }
        ResponseCookie cookie = ResponseCookie.from("XSRF-TOKEN", token.getToken())
                .httpOnly(false)
                .secure(jwtService.cookieSecure())
                .sameSite("Lax")
                .path("/")
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new CsrfResponse(token.getToken()));
    }

    @PostMapping("/register")
    public ResponseEntity<MemberResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(201).body(MemberResponse.from(memberService.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<MemberResponse> login(@Valid @RequestBody LoginRequest request) {
        Member member = memberService.authenticate(request);
        ResponseCookie cookie = authCookie(jwtService.issue(member));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(MemberResponse.from(member));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearAuthCookie().toString())
                .build();
    }

    @GetMapping("/me")
    public MemberResponse me(@AuthenticationPrincipal MemberPrincipal principal) {
        return MemberResponse.from(memberService.requireActive(principal.memberId()));
    }

    @PutMapping("/me")
    public MemberResponse updateProfile(@AuthenticationPrincipal MemberPrincipal principal,
                                        @Valid @RequestBody UpdateProfileRequest request) {
        return MemberResponse.from(memberService.updateProfile(principal.memberId(), request));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal MemberPrincipal principal,
                                                @Valid @RequestBody ChangePasswordRequest request) {
        memberService.changePassword(principal.memberId(), request);
        return ResponseEntity.noContent().build();
    }

    private ResponseCookie authCookie(String token) {
        return ResponseCookie.from(JwtService.AUTH_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(jwtService.cookieSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofSeconds(jwtService.maxAgeSeconds()))
                .build();
    }

    private ResponseCookie clearAuthCookie() {
        return ResponseCookie.from(JwtService.AUTH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(jwtService.cookieSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }

    public record CsrfResponse(String token) {
    }
}
