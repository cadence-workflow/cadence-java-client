package com.uber.cadence.entities;

import lombok.Data;

@Data
public class DescribeDomainResponse {
    private DomainInfo domainInfo;
    private DomainConfiguration configuration;
    private DomainReplicationConfiguration replicationConfiguration;
    private Long failoverVersion;
    private Boolean isGlobalDomain;
}
