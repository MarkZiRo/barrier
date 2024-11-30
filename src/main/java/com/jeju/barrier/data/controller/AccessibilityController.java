package com.jeju.barrier.data.controller;

import com.jeju.barrier.data.dto.AccessibilityDTO;
import com.jeju.barrier.data.service.GoogleSheetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/accessibility")
@RequiredArgsConstructor
@Slf4j
public class AccessibilityController {

    private final GoogleSheetService googleSheetService;

    @GetMapping
    public ResponseEntity<List<AccessibilityDTO>> getAllData() {
        try {
            List<AccessibilityDTO> data = googleSheetService.getSheetData();
            return ResponseEntity.ok(data);
        } catch (IOException e) {
            log.error("Error fetching data from Google Sheets: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 특정 ID로 데이터 조회
    @GetMapping("/{id}")
    public ResponseEntity<AccessibilityDTO> getDataById(@PathVariable String id) {
        try {
            List<AccessibilityDTO> allData = googleSheetService.getSheetData();
            AccessibilityDTO result = allData.stream()
                    .filter(dto -> dto.getId().equals(id))
                    .findFirst()
                    .orElse(null);

            if (result != null) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            log.error("Error fetching data for ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 카테고리별 데이터 조회
    @GetMapping("/category/{category}")
    public ResponseEntity<List<AccessibilityDTO>> getDataByCategory(@PathVariable String category) {
        try {
            List<AccessibilityDTO> allData = googleSheetService.getSheetData();
            List<AccessibilityDTO> filteredData = allData.stream()
                    .filter(dto -> dto.getCat() != null && dto.getCat().contains(category))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(filteredData);
        } catch (IOException e) {
            log.error("Error fetching data for category {}: {}", category, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 위치 기반 검색 (위도/경도 범위 내)
    @GetMapping("/nearby")
    public ResponseEntity<List<AccessibilityDTO>> getNearbyLocations(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "1.0") double radius) {
        try {
            List<AccessibilityDTO> allData = googleSheetService.getSheetData();
            List<AccessibilityDTO> nearbyLocations = allData.stream()
                    .filter(dto -> {
                        // null이거나 빈 문자열인 경우 필터링
                        if (dto.getLat() == null || dto.getLon() == null ||
                                dto.getLat().trim().isEmpty() || dto.getLon().trim().isEmpty()) {
                            return false;
                        }

                        try {
                            double dtoLat = Double.parseDouble(dto.getLat().trim());
                            double dtoLon = Double.parseDouble(dto.getLon().trim());
                            return isWithinRadius(dtoLat, dtoLon, lat, lon, radius);
                        } catch (NumberFormatException e) {
                            log.debug("Skipping invalid coordinates for id: {}", dto.getId());
                            return false;
                        }
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(nearbyLocations);
        } catch (Exception e) {
            log.error("Error fetching nearby locations: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 거리 계산 헬퍼 메서드 (Haversine 공식)
    private boolean isWithinRadius(double lat1, double lon1, double lat2, double lon2, double radius) {
        double R = 6371; // 지구 반지름 (km)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double distance = R * c;

        return distance <= radius;
    }
}