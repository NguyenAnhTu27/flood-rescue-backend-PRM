package com.floodrescue.module.auth.dto.request;

import com.floodrescue.shared.validation.ValidIdentifier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class LoginRequest {

    // Cho phép login bằng email hoặc phone
    @NotBlank(message = "Định danh không được để trống")
    @ValidIdentifier
    @Size(max = 120, message = "Định danh không được vượt quá 120 ký tự")
    private String identifier;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 1, max = 72, message = "Mật khẩu không được vượt quá 72 ký tự")
    private String password;
}