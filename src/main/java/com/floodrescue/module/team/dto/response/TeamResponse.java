package com.floodrescue.module.team.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class TeamResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String status;
    private String workloadStatus;
    private Double currentLatitude;
    private Double currentLongitude;
    private String currentLocationText;
    private LocalDateTime currentLocationUpdatedAt;
    private Long leaderId;
    private String leaderName;
    private Integer memberCount;
    private List<TeamMemberResponse> members;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
