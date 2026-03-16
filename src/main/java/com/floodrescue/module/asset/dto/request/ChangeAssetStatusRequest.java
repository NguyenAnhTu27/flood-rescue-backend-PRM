package com.floodrescue.module.asset.dto.request;

import com.floodrescue.shared.enums.AssetStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeAssetStatusRequest {

    @NotNull(message = "Trạng thái mới không được để trống")
    private AssetStatus newStatus;

    private String reason;
}
