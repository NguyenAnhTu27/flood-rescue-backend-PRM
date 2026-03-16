package com.floodrescue.module.relief.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReliefRequestLineRequest {

    @NotNull(message = "ID loại hàng không được để trống")
    private Integer itemCategoryId;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 0, message = "Số lượng phải lớn hơn 0")
    private Double qty;

    @NotBlank(message = "Đơn vị tính không được để trống")
    private String unit;
}

