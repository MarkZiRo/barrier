package com.jeju.barrier.OSRM.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class Step {
    private double distance;
    private double duration;
    private int type;
    private String instruction;
    private String name;
    private List<Integer> way_points;
}
