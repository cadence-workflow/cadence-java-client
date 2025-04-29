package com.uber.cadence.entities;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class RegisterDomainRequest {
    private String name;
    private String description;
    private String ownerEmail;
    private int workflowExecutionRetentionPeriodInDays;
    private List<ClusterReplicationConfiguration> clusters;
    private String activeClusterName;
    private Map<String, String> data;
    private String securityToken;
    private boolean isGlobalDomain;
    private ArchivalStatus historyArchivalStatus;
    private String historyArchivalURI;
    private ArchivalStatus visibilityArchivalStatus;
    private String visibilityArchivalURI;
}
