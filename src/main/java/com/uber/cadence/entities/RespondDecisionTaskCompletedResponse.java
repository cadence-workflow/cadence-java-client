package com.uber.cadence.entities;

import lombok.Data;
import java.util.Map;

@Data
public class RespondDecisionTaskCompletedResponse {
    private PollForDecisionTaskResponse decisionTask;
    private Map<String, ActivityLocalDispatchInfo> activitiesToDispatchLocally;
}
