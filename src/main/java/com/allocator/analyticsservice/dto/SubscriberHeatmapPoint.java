package com.allocator.analyticsservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubscriberHeatmapPoint {
    private String date;
    private long count;
}
