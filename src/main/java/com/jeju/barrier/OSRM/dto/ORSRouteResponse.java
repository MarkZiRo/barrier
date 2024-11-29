package com.jeju.barrier.OSRM.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ORSRouteResponse {
    private List<Feature> features;
    private List<Double> bbox;
    private Metadata metadata;
    private String type;
}
