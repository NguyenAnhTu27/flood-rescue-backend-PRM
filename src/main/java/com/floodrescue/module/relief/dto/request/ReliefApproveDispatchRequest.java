package com.floodrescue.module.relief.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReliefApproveDispatchRequest {

    @NotNull(message = "assignedTeamId không được để trống")
    private Long assignedTeamId;

    private String note;
}
