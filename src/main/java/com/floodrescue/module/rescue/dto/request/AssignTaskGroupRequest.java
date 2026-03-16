package com.floodrescue.module.rescue.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignTaskGroupRequest {

    @NotNull(message = "ID nhóm nhiệm vụ không được để trống")
    private Long taskGroupId;

    @NotNull(message = "ID đội cứu hộ không được để trống")
    @JsonAlias({"assignedTeamId", "assigned_team_id"})
    private Long teamId;

    // Tài sản có thể null
    private Long assetId;

    private String note;
}
