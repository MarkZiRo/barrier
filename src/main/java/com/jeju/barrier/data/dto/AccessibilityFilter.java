package com.jeju.barrier.data.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
public class AccessibilityFilter {
    private List<Category> categories; // 다중 카테고리 지원
    private UserType userType;         // 사용자 유형

    // 카테고리 정의
    public enum Category {
        TOUR("tour"),             // 관광지
        RESTAURANT("restaurant"), // 음식점
        ACCOMMODATION("accommodation"); // 숙박시설

        private final String value;

        Category(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    // 사용자 유형 정의
    public enum UserType {
        MOBILITY_IMPAIRED,    // 보행장애인
        VISUALLY_IMPAIRED,   // 시각장애인
        HEARING_IMPAIRED,    // 청각장애인
        INFANT_ACCOMPANIED   // 영유아동반
    }

    // 단일 카테고리와 유저타입을 받는 생성자
    public AccessibilityFilter(Category category, UserType userType) {
        this.categories = category != null ? List.of(category) : null;
        this.userType = userType;
    }

    // 다중 카테고리와 유저타입을 받는 생성자
    public AccessibilityFilter(List<Category> categories, UserType userType) {
        this.categories = categories;
        this.userType = userType;
    }
}