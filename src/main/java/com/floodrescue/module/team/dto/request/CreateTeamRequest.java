package com.floodrescue.module.team.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateTeamRequest {

    @NotBlank(message = "Tên đội cứu hộ không được để trống")
    private String name;

    private String code;
    private String description;
    private Long leaderId;
    private List<Long> memberIds;
    private Boolean forceReassignMembers;
}
