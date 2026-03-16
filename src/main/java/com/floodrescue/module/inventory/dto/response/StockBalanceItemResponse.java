package com.floodrescue.module.inventory.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class StockBalanceItemResponse {
    private Integer itemCategoryId;
    private String code;
    private String name;
    private String unit;

    private BigDecimal donationQty;
    private BigDecimal purchaseQty;
    private BigDecimal totalQty;
}

