package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RegisterDomainRequest {
  private String name;
  private String description;
  private String ownerEmail;
  private int workflowExecutionRetentionPeriodInDays;
  private boolean emitMetric;
  private List<ClusterReplicationConfiguration> clusters = new ArrayList<>();;
  private String activeClusterName;
  private Map<String, String> data = new HashMap<>();;
  private String securityToken;
  private boolean isGlobalDomain;
  private ArchivalStatus historyArchivalStatus;
  private String historyArchivalURI;
  private ArchivalStatus visibilityArchivalStatus;
  private String visibilityArchivalURI;
}
