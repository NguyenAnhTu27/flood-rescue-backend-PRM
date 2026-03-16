package com.floodrescue.module.relief.controller;

import com.floodrescue.module.relief.dto.response.AreaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AreaController {

    /**
     * Lấy danh sách khu vực cứu trợ.
     * FE có thể gọi các endpoint:
     * - GET /api/areas
     * - GET /api/relief/areas
     * - GET /api/manager/areas
     * - GET /api/locations
     */
    @GetMapping({"/areas", "/relief/areas", "/manager/areas", "/locations"})
    public ResponseEntity<List<AreaResponse>> getAreas() {
        List<AreaResponse> areas = Arrays.asList(
                AreaResponse.builder().id(1).name("Huyện Lệ Thủy, Quảng Bình").build(),
                AreaResponse.builder().id(2).name("Thị xã Ba Đồn, Quảng Bình").build(),
                AreaResponse.builder().id(3).name("Huyện Cam Lộ, Quảng Trị").build(),
                AreaResponse.builder().id(4).name("Huyện Hương Khê, Hà Tĩnh").build(),
                AreaResponse.builder().id(5).name("Huyện Kỳ Anh, Hà Tĩnh").build()
        );
        return ResponseEntity.ok(areas);
    }
}