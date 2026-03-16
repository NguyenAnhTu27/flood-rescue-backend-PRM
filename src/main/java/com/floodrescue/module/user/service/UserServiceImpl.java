package com.floodrescue.module.user.service;

import com.floodrescue.module.user.dto.request.UpdateMyProfileRequest;
import com.floodrescue.module.user.dto.response.UserProfileResponse;
import com.floodrescue.module.user.entity.UserEntity;
import com.floodrescue.module.user.repository.UserRepository;
import com.floodrescue.shared.exception.BusinessException;
import com.floodrescue.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(Long userId) {
        UserEntity user = userRepository.findByIdWithRole(userId)
                .orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));
        return toProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateMyProfile(Long userId, UpdateMyProfileRequest request) {
        UserEntity user = userRepository.findByIdWithRole(userId)
                .orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));

        String fullName = normalizeRequired(request.getFullName(), "Họ và tên không được để trống");
        String phone = normalizeRequired(request.getPhone(), "Số điện thoại không được để trống");
        String email = normalizeOptional(request.getEmail());

        if (userRepository.existsByPhoneAndIdNot(phone, userId)) {
            throw new BusinessException("Số điện thoại đã tồn tại");
        }
        if (email != null && userRepository.existsByEmailAndIdNot(email, userId)) {
            throw new BusinessException("Email đã tồn tại");
        }

        user.setFullName(fullName);
        user.setPhone(phone);
        user.setEmail(email);
        user.setUpdatedAt(LocalDateTime.now());

        return toProfileResponse(userRepository.save(user));
    }

    private UserProfileResponse toProfileResponse(UserEntity user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole().getCode() : null)
                .build();
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new BusinessException(message);
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
