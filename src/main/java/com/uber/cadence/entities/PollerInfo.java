package com.uber.cadence.entities;

import lombok.Data;

@Data
public class PollerInfo {
    private Long lastAccessTime;
    private String identity;
    private double ratePerSecond;
}
