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
import java.util.function.Function;
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
            @RequestParam(name = "categories", value = "categories", required = false) List<String> categories,
            @RequestParam(required = false) List<String> userTypes,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            @RequestParam(required = false) Double radius) {


        try {
            // 카테고리 문자열 리스트를 enum 리스트로 변환
            List<AccessibilityFilter.Category> categoryEnums = null;
            if (categories != null && !categories.isEmpty()) {
                categoryEnums = categories.stream()
                        .map(cat -> {
                            log.debug("카테고리 변환: {}", cat);
                            return AccessibilityFilter.Category.valueOf(cat.toUpperCase());
                        })
                        .collect(Collectors.toList());
                log.debug("변환된 카테고리: {}", categoryEnums);
            }

            // 유저타입 문자열 리스트를 enum 리스트로 변환
            List<AccessibilityFilter.UserType> userTypeEnums = null;
            if (userTypes != null && !userTypes.isEmpty()) {
                userTypeEnums = userTypes.stream()
                        .map(type -> {
                            log.debug("유저타입 변환: {}", type);
                            return AccessibilityFilter.UserType.valueOf(type.toUpperCase());
                        })
                        .collect(Collectors.toList());
                log.debug("변환된 유저타입: {}", userTypeEnums);
            }

            // 필터 객체 생성ㅌ
            AccessibilityFilter filter = AccessibilityFilter.builder()
                    .categories(categoryEnums)
                    .userTypes(userTypeEnums)
                    .title(title)
                    .lat(lat)
                    .lon(lon)
                    .radius(radius)
                    .build();
            log.debug("생성된 필터 객체: {}", filter);

            // 거리 계산을 위한 함수 정의
            Function<double[], Double> distanceCalculator = coordinates -> {
                double lat1 = coordinates[0];
                double lon1 = coordinates[1];
                double lat2 = coordinates[2];
                double lon2 = coordinates[3];

                // Haversine 공식을 사용한 거리 계산
                final int R = 6371;
                double latDistance = Math.toRadians(lat2 - lat1);
                double lonDistance = Math.toRadians(lon2 - lon1);
                double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                        + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
                double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
                double distance = R * c;

                log.trace("거리 계산 - 좌표({}, {}) -> ({}, {}): {}km",
                        lat1, lon1, lat2, lon2, distance);
                return distance;
            };

            ResponseEntity<List<AccessibilityDTO>> response = accessibilityService.getFilteredData(filter, distanceCalculator);
            log.info("=== 필터 API 호출 완료 - 상태코드: {} ===", response.getStatusCode());
            return response;

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
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