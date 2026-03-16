package com.floodrescue.module.rescue.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MarkDuplicateRequest {

    @NotNull(message = "ID yêu cầu chính không được để trống")
    private Long masterRequestId;

    private String note;
}
