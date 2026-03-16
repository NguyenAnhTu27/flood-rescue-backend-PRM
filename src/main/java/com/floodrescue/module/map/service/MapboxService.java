package com.floodrescue.module.map.service;

import com.floodrescue.module.map.dto.GeocodingResponse;

public interface MapboxService {
    GeocodingResponse geocode(String address);
    GeocodingResponse reverseGeocode(double latitude, double longitude);
    double calculateDistance(double lat1, double lng1, double lat2, double lng2);
}
