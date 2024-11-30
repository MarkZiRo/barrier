package com.jeju.barrier.data.dto;

import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
public class AccessibilityFilter {
    private List<Category> categories;
    private List<UserType> userTypes;
    private String title;
    private Double lat;
    private Double lon;
    private Double radius;

    public AccessibilityFilter(List<Category> categories, List<UserType> userTypes, String title,
                               Double lat, Double lon, Double radius) {
        this.categories = categories;
        this.userTypes = userTypes;
        this.title = title;
        this.lat = lat;
        this.lon = lon;
        this.radius = radius;
    }

    public enum Category {
        TOUR("관광"),
        ACCOMMODATION("숙박"),
        RESTAURANT("음식점");

        @Getter
        private final String value;

        Category(String value) {
            this.value = value;
        }
    }

    public enum UserType {
        MOBILITY_IMPAIRED,
        VISUALLY_IMPAIRED,
        HEARING_IMPAIRED,
        INFANT_ACCOMPANIED
    }
}