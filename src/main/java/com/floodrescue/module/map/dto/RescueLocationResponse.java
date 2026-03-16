package com.floodrescue.module.map.dto;

import com.floodrescue.shared.enums.RescuePriority;
import com.floodrescue.shared.enums.RescueRequestStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RescueLocationResponse {
    private Long id;
    private String code;
    private RescueRequestStatus status;
    private RescuePriority priority;
    private Double latitude;
    private Double longitude;
    private String addressText;
    private Integer affectedPeopleCount;
    private String citizenName;
}
