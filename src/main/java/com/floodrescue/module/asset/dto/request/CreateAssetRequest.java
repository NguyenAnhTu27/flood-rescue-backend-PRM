package com.floodrescue.module.asset.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAssetRequest {

    @NotBlank(message = "Mã phương tiện không được để trống")
    private String code;

    @NotBlank(message = "Tên phương tiện không được để trống")
    private String name;

    @NotBlank(message = "Loại phương tiện không được để trống")
    @JsonAlias({"asset_type", "type"})
    private String assetType;

    private Integer capacity;

    // Có thể null nếu chưa gán cho đội cụ thể
    @JsonAlias({"assigned_team_id", "teamId", "team_id"})
    private Long assignedTeamId;

    private String note;
}