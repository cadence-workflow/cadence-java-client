package com.uber.cadence.entities;

import lombok.Data;

@Data
public class DomainConfiguration {
    private Integer workflowExecutionRetentionPeriodInDays;
    private Boolean emitMetric;
    private IsolationGroupConfiguration isolationGroups;
    private AsyncWorkflowConfiguration asyncWorkflowConfiguration;
    private BadBinaries badBinaries;
    private ArchivalStatus historyArchivalStatus;
    private String historyArchivalURI;
    private ArchivalStatus visibilityArchivalStatus;
    private String visibilityArchivalURI;
}
