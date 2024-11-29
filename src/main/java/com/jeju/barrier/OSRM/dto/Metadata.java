package com.jeju.barrier.OSRM.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Metadata {
    private String attribution;
    private String service;
    private long timestamp;
    private Query query;
}
