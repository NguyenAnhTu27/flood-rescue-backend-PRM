package com.floodrescue.module.rescue.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EscalateTaskGroupRequest {

    @NotBlank(message = "Mức khẩn cấp không được để trống")
    @Size(max = 20, message = "Mức khẩn cấp không hợp lệ")
    private String severity;

    @NotBlank(message = "Lý do khẩn cấp không được để trống")
    @Size(max = 2000, message = "Lý do khẩn cấp không được vượt quá 2000 ký tự")
    private String reason;
}
