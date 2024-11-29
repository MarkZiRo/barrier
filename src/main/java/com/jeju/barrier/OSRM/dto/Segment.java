package com.jeju.barrier.OSRM.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class Segment {
    private double distance;
    private double duration;
    private List<Step> steps;
}
