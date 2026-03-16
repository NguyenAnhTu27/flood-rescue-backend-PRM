package com.floodrescue.module.rescue.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlockCitizenRequest {
    @NotNull
    private Boolean blocked;

    @Size(max = 1000)
    private String reason;
}

