package com.floodrescue.module.auth.service;

import com.floodrescue.config.security.JwtTokenProvider;
import com.floodrescue.module.auth.dto.request.LoginRequest;
import com.floodrescue.module.auth.dto.request.RegisterCitizenRequest;
import com.floodrescue.module.auth.dto.response.LoginResponse;
import com.floodrescue.module.user.entity.RoleEntity;
import com.floodrescue.module.user.entity.UserEntity;
import com.floodrescue.module.user.repository.RoleRepository;
import com.floodrescue.module.user.repository.UserRepository;
import com.floodrescue.shared.exception.BusinessException;
import com.floodrescue.shared.exception.UnauthorizedException;
import com.floodrescue.shared.util.PhoneUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void registerCitizen(RegisterCitizenRequest req) {
        // Normalize and sanitize phone number
        String normalizedPhone = PhoneUtil.normalize(req.getPhone());
        if (normalizedPhone == null) {
            throw new BusinessException("Số điện thoại không hợp lệ");
        }

        // Check for duplicate phone (using normalized format)
        if (userRepo.existsByPhone(normalizedPhone)) {
            throw new BusinessException("Số điện thoại đã tồn tại");
        }

        // Normalize email if provided
        String normalizedEmail = null;
        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            normalizedEmail = req.getEmail().trim().toLowerCase();
            if (userRepo.existsByEmail(normalizedEmail)) {
                throw new BusinessException("Email đã tồn tại");
            }
        }

        RoleEntity citizenRole = roleRepo.findByCode("CITIZEN")
                .orElseThrow(() -> new BusinessException("Chưa có role CITIZEN trong bảng roles"));

        LocalDateTime now = LocalDateTime.now();

        UserEntity user = UserEntity.builder()
                .role(citizenRole)
                .teamId(null)
                .fullName(req.getFullName().trim())
                .phone(normalizedPhone) // Store normalized phone
                .email(normalizedEmail) // Store normalized email
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .status((byte) 1)
                .createdAt(now)
                .updatedAt(now)
                .build();

        userRepo.save(user);
    }

    @Override
    public LoginResponse login(LoginRequest req) {
        String identifier = req.getIdentifier().trim();

        UserEntity user = null;

        // Try to normalize as phone number first
        String normalizedPhone = PhoneUtil.normalize(identifier);
        if (normalizedPhone != null) {
            // Search by normalized phone
            user = userRepo.findByPhone(normalizedPhone).orElse(null);
        }

        // If not found by phone, try email (normalized to lowercase)
        if (user == null) {
            String normalizedEmail = identifier.toLowerCase();
            user = userRepo.findByEmail(normalizedEmail).orElse(null);
        }

        if (user == null) {
            throw new UnauthorizedException("Tài khoản không tồn tại");
        }

        if (user.getStatus() == 0) {
            throw new UnauthorizedException("Tài khoản đã bị vô hiệu hóa");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Mật khẩu không đúng");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepo.save(user);

        String roleCode = user.getRole().getCode();
        String token = jwtTokenProvider.generateToken(user.getId(), roleCode);

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .fullName(user.getFullName())
                .role(roleCode)
                .build();
    }
}