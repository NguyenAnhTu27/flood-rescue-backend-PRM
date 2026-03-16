package com.floodrescue.module.asset.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignTeamRequest {

    @NotNull(message = "ID đội cứu hộ không được để trống")
    @JsonAlias({"team_id", "teamId"})
    private Long teamId;
}
