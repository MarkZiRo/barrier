package com.jeju.barrier.data.controller;

import com.jeju.barrier.data.dto.AccessibilityDTO;
import com.jeju.barrier.data.dto.AccessibilityFilter;
import com.jeju.barrier.data.service.AccessibilityService;
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
    private final AccessibilityService accessibilityService;


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
            // 문자열을 enum으로 변환
            AccessibilityFilter.Category categoryEnum = AccessibilityFilter.Category.valueOf(category.toUpperCase());

            List<AccessibilityDTO> allData = googleSheetService.getSheetData();
            List<AccessibilityDTO> filteredData = allData.stream()
                    .filter(dto -> dto.getCat() != null &&
                            dto.getCat().equals(categoryEnum.getValue()))  // enum의 value와 정확히 비교
                    .collect(Collectors.toList());

            return ResponseEntity.ok(filteredData);
        } catch (IllegalArgumentException e) {
            log.error("잘못된 카테고리 값: {}", category);
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            log.error("카테고리 {} 데이터 조회 중 오류 발생: {}", category, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/filter")
    public ResponseEntity<List<AccessibilityDTO>> getFilteredData(
            @RequestParam(required = false) List<String> categories,  // 다중 카테고리
            @RequestParam(required = false) String userType) {        // 단일 유저타입
        try {
            // 카테고리 문자열 리스트를 enum 리스트로 변환
            List<AccessibilityFilter.Category> categoryEnums = null;
            if (categories != null && !categories.isEmpty()) {
                categoryEnums = categories.stream()
                        .map(cat -> AccessibilityFilter.Category.valueOf(cat.toUpperCase()))
                        .collect(Collectors.toList());
            }

            // 유저타입 문자열을 enum으로 변환
            AccessibilityFilter.UserType userTypeEnum = userType != null ?
                    AccessibilityFilter.UserType.valueOf(userType.toUpperCase()) : null;

            // 필터 객체 생성
            AccessibilityFilter filter = new AccessibilityFilter(categoryEnums, userTypeEnum);

            return accessibilityService.getFilteredData(filter);
        } catch (IllegalArgumentException e) {
            log.error("잘못된 카테고리 또는 유저타입 값: categories={}, userType={}", categories, userType);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("데이터 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @GetMapping
    public ResponseEntity<List<AccessibilityDTO>> getPlaces(
            @RequestParam(required = false) String category,  // String으로 받아서
            @RequestParam(required = false) String userType) {  // String으로 받아서
        try {
            // null 체크 후 enum으로 변환
            AccessibilityFilter.Category categoryEnum = category != null ?
                    AccessibilityFilter.Category.valueOf(category.toUpperCase()) : null;
            AccessibilityFilter.UserType userTypeEnum = userType != null ?
                    AccessibilityFilter.UserType.valueOf(userType.toUpperCase()) : null;

            AccessibilityFilter filter = new AccessibilityFilter(categoryEnum, userTypeEnum);
            return accessibilityService.getFilteredData(filter);
        } catch (IllegalArgumentException e) {
            log.error("잘못된 category 또는 userType 값: category={}, userType={}", category, userType);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("데이터 조회 중 오류 발생: {}", e.getMessage());
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

    @GetMapping("/search/{title}")
    public ResponseEntity<List<AccessibilityDTO>> searchByTitle(
            @PathVariable String title) {
        try {
            return accessibilityService.searchByTitle(title);
        } catch (Exception e) {
            log.error("제목 검색 중 오류 발생: {}", e.getMessage());
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