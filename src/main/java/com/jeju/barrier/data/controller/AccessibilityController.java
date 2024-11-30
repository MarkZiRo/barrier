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
                            double distance = calculateDistance(dtoLat, dtoLon, lat, lon);
                            // radius 킬로미터 이내의 장소만 필터링
                            return distance <= radius;
                        } catch (NumberFormatException e) {
                            log.debug("Skipping invalid coordinates for id: {}", dto.getId());
                            return false;
                        }
                    })
                    .sorted((loc1, loc2) -> {
                        // 각 위치의 거리 계산
                        double dist1 = calculateDistance(
                                Double.parseDouble(loc1.getLat().trim()),
                                Double.parseDouble(loc1.getLon().trim()),
                                lat,
                                lon
                        );
                        double dist2 = calculateDistance(
                                Double.parseDouble(loc2.getLat().trim()),
                                Double.parseDouble(loc2.getLon().trim()),
                                lat,
                                lon
                        );
                        // 거리를 기준으로 오름차순 정렬
                        return Double.compare(dist1, dist2);
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(nearbyLocations);
        } catch (Exception e) {
            log.error("Error fetching nearby locations: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 두 지점 간의 거리를 계산하는 메서드 (Haversine 공식 사용)
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 지구의 반지름 (km)

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c; // 킬로미터 단위의 거리 반환
    }
}