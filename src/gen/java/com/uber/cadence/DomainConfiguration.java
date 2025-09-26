package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DomainConfiguration {
  private int workflowExecutionRetentionPeriodInDays;
  private boolean emitMetric;
  private IsolationGroupConfiguration isolationgroups;
  private BadBinaries badBinaries;
  private ArchivalStatus historyArchivalStatus;
  private String historyArchivalURI;
  private ArchivalStatus visibilityArchivalStatus;
  private String visibilityArchivalURI;
  private AsyncWorkflowConfiguration AsyncWorkflowConfiguration;
}
