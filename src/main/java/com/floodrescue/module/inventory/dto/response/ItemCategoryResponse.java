package com.floodrescue.module.inventory.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ItemCategoryResponse {
    private Integer id;
    private String code;
    private String name;
    private String unit;
    private Integer classificationId;
    private String classificationCode;
    private String classificationName;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
