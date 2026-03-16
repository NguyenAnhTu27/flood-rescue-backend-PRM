package com.floodrescue.module.asset.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.floodrescue.shared.enums.AssetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAssetRequest {

    @NotBlank(message = "Mã phương tiện không được để trống")
    private String code;

    @NotBlank(message = "Tên phương tiện không được để trống")
    private String name;

    @NotNull(message = "Loại phương tiện không được để trống")
    @JsonAlias({"asset_type", "type"})
    private AssetType assetType;

    private Integer capacity;

    // Có thể null nếu chưa gán cho đội cụ thể
    @JsonAlias({"assigned_team_id", "teamId", "team_id"})
    private Long assignedTeamId;

    private String note;
}