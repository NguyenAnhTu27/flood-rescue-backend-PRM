package com.floodrescue.module.asset.dto.request;

import com.floodrescue.shared.enums.AssetStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssetStatusUpdateRequest {

    @NotNull
    private AssetStatus status;
}
