package com.floodrescue.module.rescue.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class CoordinatorDashboardResponse {

    private List<QueueItem> requests;
    private List<TeamItem> teams;
    private List<VehicleItem> vehicles;

    @Getter
    @Setter
    @Builder
    public static class QueueItem {
        private Long id;
        private String code;
        private String priority;     // HIGH/MEDIUM/LOW
        private Integer peopleCount; // affectedPeopleCount
        private String timeAgo;      // "1p trước"
        private String status;       // PENDING/VERIFIED/...
        private Boolean waitingForTeam;
        // Map sẽ làm sau => để null
        private Double lat;
        private Double lng;
    }

    @Getter
    @Setter
    @Builder
    public static class TeamItem {
        private Long id;
        private String name;
        private String area;       // có thể null
        private String status;     // AVAILABLE/BUSY
        private Double distance;   // map sau => null
        private String lastUpdate; // có thể null
        private Double lat;
        private Double lng;
        private Boolean online;
    }

    @Getter
    @Setter
    @Builder
    public static class VehicleItem {
        private Long id;
        private String code;
        private String name;
        private String type;      // "boat" | "cano" | "helicopter" | ...
        private Integer capacity;
        private String status;    // AVAILABLE/IN_USE/...
        private Double distance;  // map sau => null
        private String location;  // có thể null
        private Boolean online;
    }
}
