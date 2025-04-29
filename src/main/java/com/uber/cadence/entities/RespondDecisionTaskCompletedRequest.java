package com.uber.cadence.entities;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class RespondDecisionTaskCompletedRequest {
    private byte[] taskToken;
    private List<Decision> decisions;
    private StickyExecutionAttributes stickyAttributes;
    private boolean returnNewDecisionTask;
    private boolean forceCreateNewDecisionTask;
    private Map<String, WorkflowQueryResult> queryResults;
    private byte[] executionContext;
    private String binaryChecksum;
    private String identity;
}
