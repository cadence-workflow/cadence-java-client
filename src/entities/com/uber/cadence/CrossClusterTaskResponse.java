package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class CrossClusterTaskResponse {
  private long taskID;
  private CrossClusterTaskType taskType;
  private int taskState;
  private CrossClusterTaskFailedCause failedCause;
  private CrossClusterStartChildExecutionResponseAttributes startChildExecutionAttributes;
  private CrossClusterCancelExecutionResponseAttributes cancelExecutionAttributes;
  private CrossClusterSignalExecutionResponseAttributes signalExecutionAttributes;
  private CrossClusterRecordChildWorkflowExecutionCompleteResponseAttributes
      recordChildWorkflowExecutionCompleteAttributes;
  private CrossClusterApplyParentClosePolicyResponseAttributes applyParentClosePolicyAttributes;
}
