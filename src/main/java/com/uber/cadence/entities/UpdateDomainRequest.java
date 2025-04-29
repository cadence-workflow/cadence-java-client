package com.uber.cadence.entities;

import lombok.Data;

@Data
public class UpdateDomainRequest {
    private String name;
    private String securityToken;
    private UpdateDomainInfo updatedInfo;
    private DomainConfiguration configuration;
    private String deleteBadBinary;
    private int failoverTimeoutInSeconds;
}
