package com.floodrescue.module.auth.dto.request;

import com.floodrescue.shared.validation.ValidPhoneNumber;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RegisterCitizenRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 120, message = "Họ và tên không được vượt quá 120 ký tự")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @ValidPhoneNumber
    @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự")
    private String phone;

    @Email(message = "Email không hợp lệ")
    @Size(max = 120, message = "Email không được vượt quá 120 ký tự")
    private String email; // optional

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, max = 72, message = "Mật khẩu phải có độ dài từ 6 đến 72 ký tự")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", 
             message = "Mật khẩu phải chứa ít nhất một chữ hoa, một chữ thường và một chữ số")
    private String password;
}