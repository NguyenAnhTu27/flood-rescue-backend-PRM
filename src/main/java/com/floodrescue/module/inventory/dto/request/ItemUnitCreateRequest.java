package com.floodrescue.module.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemUnitCreateRequest {

    @NotBlank(message = "Mã đơn vị không được để trống")
    private String code;

    @NotBlank(message = "Tên đơn vị không được để trống")
    private String name;
}
