package com.floodrescue.module.inventory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventoryReceiptLineRequest {

    @NotNull(message = "ID loại hàng không được để trống")
    private Integer itemCategoryId;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 0, message = "Số lượng phải lớn hơn 0")
    private Double qty;

    @NotNull(message = "Đơn vị tính không được để trống")
    private String unit;

    /**
     * Tên mặt hàng cụ thể (optional - để FE hiển thị/validate).
     * Nếu không có, BE sẽ lấy từ ItemCategory theo itemCategoryId.
     */
    private String itemName;
}

