package com.floodrescue.module.user.dto.request;

import com.floodrescue.shared.validation.ValidPhoneNumber;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMyProfileRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 120, message = "Họ và tên không được vượt quá 120 ký tự")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @ValidPhoneNumber
    @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự")
    private String phone;

    @Email(message = "Email không hợp lệ")
    @Size(max = 120, message = "Email không được vượt quá 120 ký tự")
    private String email;
}
