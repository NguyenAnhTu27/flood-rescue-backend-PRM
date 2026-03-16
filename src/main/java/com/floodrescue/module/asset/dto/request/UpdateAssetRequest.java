package com.floodrescue.module.asset.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.floodrescue.shared.enums.AssetType;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateAssetRequest {

    @Size(max = 120, message = "Tên phương tiện không được vượt quá 120 ký tự")
    private String name;

    @JsonAlias({"asset_type", "type"})
    private AssetType assetType;

    private Integer capacity;

    private String note;
}
