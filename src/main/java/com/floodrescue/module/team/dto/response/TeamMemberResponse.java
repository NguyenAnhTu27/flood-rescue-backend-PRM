package com.floodrescue.module.team.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TeamMemberResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private Long teamId;
    private String teamCode;
    private String teamName;
    private Boolean isLeader;
}

