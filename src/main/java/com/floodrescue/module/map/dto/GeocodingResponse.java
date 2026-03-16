package com.floodrescue.module.map.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class GeocodingResponse {
    private String address;
    private Double latitude;
    private Double longitude;
}
