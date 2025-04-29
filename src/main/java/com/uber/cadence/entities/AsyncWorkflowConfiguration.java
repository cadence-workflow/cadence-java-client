package com.uber.cadence.entities;

import lombok.Data;

@Data
public class AsyncWorkflowConfiguration {
    private boolean enabled;
    private Integer prefetchTaskListSize;
    private Integer maxConcurrentTaskExecutionSize;
}
