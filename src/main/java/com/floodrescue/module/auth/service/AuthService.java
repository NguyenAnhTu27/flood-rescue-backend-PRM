package com.floodrescue.module.auth.service;

import com.floodrescue.module.auth.dto.request.LoginRequest;
import com.floodrescue.module.auth.dto.request.RegisterCitizenRequest;
import com.floodrescue.module.auth.dto.response.LoginResponse;

public interface AuthService {
    void registerCitizen(RegisterCitizenRequest req);
    LoginResponse login(LoginRequest req);
}