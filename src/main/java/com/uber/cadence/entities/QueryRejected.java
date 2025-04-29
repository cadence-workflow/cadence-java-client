package com.uber.cadence.entities;

import lombok.Data;

@Data
public class QueryRejected {
    private WorkflowExecutionCloseStatus closeStatus;
}
