package com.uber.cadence.entities;

import lombok.Data;

@Data
public class StatusFilter {
    private WorkflowExecutionCloseStatus status;
}
