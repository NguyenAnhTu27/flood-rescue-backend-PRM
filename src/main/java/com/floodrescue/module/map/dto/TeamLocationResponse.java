package com.floodrescue.module.map.dto;

import com.floodrescue.shared.enums.TeamType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class TeamLocationResponse {
    private Long teamId;
    private String name;
    private Byte status;
    private TeamType teamType;
    private Double latitude;
    private Double longitude;
    private Double distanceKm;
    private LocalDateTime lastLocationUpdate;
}
