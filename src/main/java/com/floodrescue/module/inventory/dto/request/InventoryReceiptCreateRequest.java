package com.floodrescue.module.inventory.dto.request;

import com.floodrescue.shared.enums.StockSourceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class InventoryReceiptCreateRequest {

    @NotNull(message = "Nguồn hàng không được để trống")
    private StockSourceType sourceType;

    private String note;

    @NotEmpty(message = "Danh sách dòng phiếu nhập không được để trống")
    @Valid
    private List<InventoryReceiptLineRequest> lines;
}

