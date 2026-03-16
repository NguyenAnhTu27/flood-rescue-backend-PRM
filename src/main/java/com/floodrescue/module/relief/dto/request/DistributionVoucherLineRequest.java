package com.floodrescue.module.relief.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DistributionVoucherLineRequest {

    @NotNull(message = "itemCategoryId không được để trống")
    private Integer itemCategoryId;

    @NotNull(message = "qty không được để trống")
    @Min(value = 1, message = "qty phải lớn hơn 0")
    private Double qty;

    private String unit;
}
