package com.jeju.barrier.data.service;

import com.jeju.barrier.data.dto.AccessibilityDTO;
import com.jeju.barrier.data.dto.AccessibilityFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccessibilityService {
    private final GoogleSheetService googleSheetService;

    // 공통 편의시설 정의
    private final Set<String> commonFeatures = Set.of(
            "보장구 대여", "안내데스크", "장애인 화장실", "장애인 객실",
            "주차장", "키오스크 접근 가능"
    );

    // 사용자 유형별 특정 편의시설 정의
    private final Map<AccessibilityFilter.UserType, Set<String>> specificFeatures = Map.of(
            // 보행장애인을 위한 특정 편의시설
            AccessibilityFilter.UserType.MOBILITY_IMPAIRED, Set.of(
                    "단독접근가능", "도움필요", "단차", "경사로", "테이블 비치",
                    "매표소 접근 가능", "장애인 리프트", "승강기", "장애인 관람석",
                    "장애인 전용 주차구역", "전동휠체어 급속충전기"
            ),
            // 시각장애인을 위한 특정 편의시설
            AccessibilityFilter.UserType.VISUALLY_IMPAIRED, Set.of(
                    "점자블록", "점자안내판", "안내견 출입 가능", "점자책 대여", "음성안내해설"
            ),
            // 청각장애인을 위한 특정 편의시설
            AccessibilityFilter.UserType.HEARING_IMPAIRED, Set.of(
                    "수어안내해설"
            ),
            // 영유아동반을 위한 특정 편의시설
            AccessibilityFilter.UserType.INFANT_ACCOMPANIED, Set.of(
                    "아기의자", "수유실", "유아차 대여", "가족화장실"
            )
    );

    public ResponseEntity<List<AccessibilityDTO>> getFilteredData(AccessibilityFilter filter) {
        try {
            // 전체 데이터를 가져옴
            List<AccessibilityDTO> allData = googleSheetService.getSheetData();

            // 스트림을 사용하여 필터링 수행
            List<AccessibilityDTO> filteredData = allData.stream()
                    .filter(dto -> matchesCategories(dto, filter.getCategories()) &&
                            matchesAccessibilityFeatures(dto, filter.getUserType()))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(filteredData);
        } catch (IOException e) {
            log.error("데이터 필터링 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 카테고리 매칭 확인

    private boolean matchesCategories(AccessibilityDTO dto, List<AccessibilityFilter.Category> categories) {
        // 카테고리가 null이거나 비어있으면 모든 데이터 포함
        if (categories == null || categories.isEmpty()) {
            return true;
        }

        // dto의 카테고리가 null이면 false
        if (dto.getCat() == null) {
            return false;
        }

        // 주어진 카테고리 목록 중 하나라도 일치하면 true
        return categories.stream()
                .map(AccessibilityFilter.Category::getValue)  // enum의 value 값을 가져옴
                .anyMatch(categoryValue -> dto.getCat().equals(categoryValue));
    }
    // 접근성 기능 매칭 확인
    private boolean matchesAccessibilityFeatures(AccessibilityDTO dto, AccessibilityFilter.UserType userType) {
        if (userType == null) {
            return true; // 유저타입 필터가 없으면 모든 데이터 통과
        }

        Set<String> features = specificFeatures.get(userType);
        if (features == null || features.isEmpty()) {
            return true;
        }

        // 특정 기능과 공통 편의시설을 모두 만족해야 함
        boolean hasSpecificFeatures = features.stream()
                .anyMatch(feature -> matchesFeature(dto, feature));
        boolean hasCommonFeatures = commonFeatures.stream()
                .anyMatch(commonFeature -> matchesFeature(dto, commonFeature));

        return hasSpecificFeatures || hasCommonFeatures;
    }

    // 특정 기능 매칭 확인
    private boolean matchesFeature(AccessibilityDTO dto, String feature) {
        // hints 필드 확인
        if (dto.getHints() != null && dto.getHints().contains(feature)) {
            return true;
        }

        // barrierFree 필드 확인
        return hasFeatureInBarrierFreeFields(dto, feature);
    }

    // barrierFree 필드 값 가져오기
    private String getBarrierFreeValue(AccessibilityDTO dto, int index) {
        try {
            Field field = AccessibilityDTO.class.getDeclaredField("barrierFree_" + index);
            field.setAccessible(true);
            return (String) field.get(dto);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }

    public ResponseEntity<List<AccessibilityDTO>> searchByTitle(String title) {
        try {
            List<AccessibilityDTO> allData = googleSheetService.getSheetData();

            List<AccessibilityDTO> searchResults = allData.stream()
                    .filter(dto -> dto.getTitle() != null &&
                            dto.getTitle().toLowerCase().contains(title.toLowerCase().trim()))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(searchResults);
        } catch (IOException e) {
            log.error("제목 검색 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // barrierFree 필드들에서 특정 기능 검색
    private boolean hasFeatureInBarrierFreeFields(AccessibilityDTO dto, String feature) {
        for (int i = 1; i <= 16; i++) {
            String value = getBarrierFreeValue(dto, i);
            if (value != null && value.contains(feature)) {
                return true;
            }
        }
        return false;
    }
}