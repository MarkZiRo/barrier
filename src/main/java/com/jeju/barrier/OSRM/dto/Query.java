package com.jeju.barrier.OSRM.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class Query {
    private List<List<Double>> coordinates;
    private String profile;
    private String format;
}
