package com.jeju.barrier.data.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
@NoArgsConstructor
public class AccessibilityFilter {
    private List<Category> categories;    // 다중 카테고리 지원
    private List<UserType> userTypes;     // 다중 사용자 유형 지원 (userType → userTypes로 변경)
    private String title;                 // 제목 검색

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

    // 단일 카테고리와 단일 유저타입을 받는 생성자
    public AccessibilityFilter(Category category, UserType userType, String title) {
        this.categories = category != null ? List.of(category) : null;
        this.userTypes = userType != null ? List.of(userType) : null;
        this.title = title;
    }

    // 다중 카테고리와 다중 유저타입을 받는 생성자
    public AccessibilityFilter(List<Category> categories, List<UserType> userTypes, String title) {
        this.categories = categories;
        this.userTypes = userTypes;
        this.title = title;
    }
}