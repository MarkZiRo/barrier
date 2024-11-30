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
import java.util.Comparator;
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

    @GetMapping("/filter")
    public ResponseEntity<List<AccessibilityDTO>> getFilteredData(
            @RequestParam(required = false) List<String> categories,
            @RequestParam(required = false) List<String> userTypes,  // 단일 userType → List<String> userTypes
            @RequestParam(required = false) String title) {
        try {
            // 카테고리 문자열 리스트를 enum 리스트로 변환
            List<AccessibilityFilter.Category> categoryEnums = null;
            if (categories != null && !categories.isEmpty()) {
                categoryEnums = categories.stream()
                        .map(cat -> AccessibilityFilter.Category.valueOf(cat.toUpperCase()))
                        .collect(Collectors.toList());
            }

            // 유저타입 문자열 리스트를 enum 리스트로 변환
            List<AccessibilityFilter.UserType> userTypeEnums = null;
            if (userTypes != null && !userTypes.isEmpty()) {
                userTypeEnums = userTypes.stream()
                        .map(type -> AccessibilityFilter.UserType.valueOf(type.toUpperCase()))
                        .collect(Collectors.toList());
            }

            // 필터 객체 생성
            AccessibilityFilter filter = new AccessibilityFilter(categoryEnums, userTypeEnums, title);

            return accessibilityService.getFilteredData(filter);
        } catch (IllegalArgumentException e) {
            log.error("잘못된 카테고리 또는 유저타입 값: categories={}, userTypes={}, title={}",
                    categories, userTypes, title);
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
                        if (dto.getLat() == null || dto.getLon() == null ||
                                dto.getLat().trim().isEmpty() || dto.getLon().trim().isEmpty()) {
                            return false;
                        }

                        try {
                            double dtoLat = Double.parseDouble(dto.getLat().trim());
                            double dtoLon = Double.parseDouble(dto.getLon().trim());
                            double distance = calculateDistance(dtoLat, dtoLon, lat, lon);
                            // 거리 정보 설정
                            dto.setDistance(Math.round(distance * 100.0) / 100.0); // 소수점 둘째자리까지 반올림
                            return distance <= radius;
                        } catch (NumberFormatException e) {
                            log.debug("Skipping invalid coordinates for id: {}", dto.getId());
                            return false;
                        }
                    })
                    .sorted(Comparator.comparing(AccessibilityDTO::getDistance)) // 거리순 정렬
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

    @GetMapping("/search/{title}/distance")
    public ResponseEntity<List<AccessibilityDTO>> searchByTitleWithDistance(
            @PathVariable String title,
            @RequestParam double lat,
            @RequestParam double lon) {
        try {
            List<AccessibilityDTO> allData = googleSheetService.getSheetData();

            List<AccessibilityDTO> searchResults = allData.stream()
                    .filter(dto -> dto.getTitle() != null &&
                            dto.getTitle().toLowerCase().contains(title.toLowerCase().trim()))
                    .filter(dto -> {
                        // 좌표가 유효한 데이터만 필터링
                        if (dto.getLat() == null || dto.getLon() == null ||
                                dto.getLat().trim().isEmpty() || dto.getLon().trim().isEmpty()) {
                            return false;
                        }

                        try {
                            double dtoLat = Double.parseDouble(dto.getLat().trim());
                            double dtoLon = Double.parseDouble(dto.getLon().trim());
                            double distance = calculateDistance(dtoLat, dtoLon, lat, lon);
                            // 거리 정보 설정 (소수점 둘째자리까지 반올림)
                            dto.setDistance(Math.round(distance * 100.0) / 100.0);
                            return true;
                        } catch (NumberFormatException e) {
                            log.debug("Skipping invalid coordinates for id: {}", dto.getId());
                            return false;
                        }
                    })
                    .sorted(Comparator.comparing(AccessibilityDTO::getDistance)) // 거리순 정렬
                    .collect(Collectors.toList());

            return ResponseEntity.ok(searchResults);
        } catch (Exception e) {
            log.error("제목 검색 및 거리 계산 중 오류 발생: {}", e.getMessage());
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