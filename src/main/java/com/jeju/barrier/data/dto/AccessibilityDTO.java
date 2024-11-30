package com.jeju.barrier.data.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class AccessibilityDTO {
    private String id;              // 고유 ID
    private String description;     // 설명
    private String address;         // 주소
    private String phone;           // 전화번호
    private String schedule;        // 운영시간
    private String thumbnails;      // 썸네일 URL
    private String thumb;           // 썸네일 관련
    private String lat;            // 위도
    private String lon;            // 경도
    private String hints;          // 힌트/메모
    private String cat;            // 카테고리

    // Barrier Free 관련 항목들
    private String barrierFree_1;
    private String barrierFree_2;
    private String barrierFree_3;
    private String barrierFree_4;
    private String barrierFree_5;
    private String barrierFree_6;
    private String barrierFree_7;
    private String barrierFree_8;
    private String barrierFree_9;
    private String barrierFree_10;
    private String barrierFree_11;
    private String barrierFree_12;
    private String barrierFree_13;
    private String barrierFree_14;
    private String barrierFree_15;
    private String barrierFree_16;

    // 경사로 관련
    private String slope;          // 경사로
    private String slopeScale;     // 경사로 규모

    // 시설 관련
    private String elevator;       // 엘리베이터
    private String toilet;         // 화장실
    private String parking;        // 주차장
    private String table;          // 테이블

    // 평가 관련
    private String total;          // 총점
    private String accessibility;  // 접근성 평가
}
