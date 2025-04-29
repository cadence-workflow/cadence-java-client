package com.uber.cadence.entities;

import lombok.Data;

@Data
public class StartTimeFilter {
    private Long earliestTime;
    private Long latestTime;
}
