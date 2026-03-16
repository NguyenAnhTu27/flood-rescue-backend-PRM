package com.floodrescue.module.user.service;

import com.floodrescue.module.user.dto.request.UpdateMyProfileRequest;
import com.floodrescue.module.user.dto.response.UserProfileResponse;

public interface UserService {
    UserProfileResponse getMyProfile(Long userId);

    UserProfileResponse updateMyProfile(Long userId, UpdateMyProfileRequest request);
}
