package com.jeju.barrier.OSRM.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Properties;

@Getter
@Setter
@NoArgsConstructor
public class Feature {
    private ORSGeometry geometry;
    private Properties properties;
    private String type;
}
