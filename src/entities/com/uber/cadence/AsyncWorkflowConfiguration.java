package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AsyncWorkflowConfiguration {
  private boolean enabled;
  private String predefinedQueueName;
  private String queueType;
  private DataBlob queueConfig;
}
