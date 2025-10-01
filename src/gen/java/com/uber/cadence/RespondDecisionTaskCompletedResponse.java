package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RespondDecisionTaskCompletedResponse {
  private PollForDecisionTaskResponse decisionTask;
  private Map<String, ActivityLocalDispatchInfo> activitiesToDispatchLocally = new HashMap<>();;
}
