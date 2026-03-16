    package com.floodrescue.module.auth.controller;

import com.floodrescue.module.auth.dto.request.LoginRequest;
import com.floodrescue.module.auth.dto.request.RegisterCitizenRequest;
import com.floodrescue.module.auth.dto.response.LoginResponse;
import com.floodrescue.module.auth.service.AuthService;
import com.floodrescue.module.user.entity.UserEntity;
import com.floodrescue.module.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return Long.parseLong(userDetails.getUsername());
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerCitizen(@Valid @RequestBody RegisterCitizenRequest req) {
        authService.registerCitizen(req);
        return ResponseEntity.ok(Map.of("message", "Đăng ký Citizen thành công"));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }
        UserEntity user = userRepository.findById(userId)
                .orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        }

        String role = user.getRole() != null ? user.getRole().getCode() : null;
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "fullName", user.getFullName() == null ? "" : user.getFullName(),
                "phone", user.getPhone() == null ? "" : user.getPhone(),
                "email", user.getEmail() == null ? "" : user.getEmail(),
                "role", role == null ? "" : role,
                "rescueRequestBlocked", Boolean.TRUE.equals(user.getRescueRequestBlocked()),
                "rescueRequestBlockedReason", user.getRescueRequestBlockedReason() == null ? "" : user.getRescueRequestBlockedReason()
        ));
    }
}
