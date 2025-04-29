package com.uber.cadence.entities;

import lombok.Data;

@Data
public class WorkerVersionInfo {
    private String impl;
    private String featureVersion;
}
