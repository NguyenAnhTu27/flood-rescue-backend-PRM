package com.floodrescue.module.relief.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReliefRequestRejectRequest {

    @NotBlank(message = "Lý do từ chối không được để trống")
    private String reason;
}
