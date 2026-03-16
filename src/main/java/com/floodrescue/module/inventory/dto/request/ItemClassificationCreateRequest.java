package com.floodrescue.module.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemClassificationCreateRequest {

    @NotBlank(message = "Mã phân loại không được để trống")
    private String code;

    @NotBlank(message = "Tên phân loại không được để trống")
    private String name;
}
