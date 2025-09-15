package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class CrossClusterTaskRequest {
  private CrossClusterTaskInfo taskInfo;
  private CrossClusterStartChildExecutionRequestAttributes startChildExecutionAttributes;
  private CrossClusterCancelExecutionRequestAttributes cancelExecutionAttributes;
  private CrossClusterSignalExecutionRequestAttributes signalExecutionAttributes;
  private CrossClusterRecordChildWorkflowExecutionCompleteRequestAttributes
      recordChildWorkflowExecutionCompleteAttributes;
  private CrossClusterApplyParentClosePolicyRequestAttributes applyParentClosePolicyAttributes;
}
