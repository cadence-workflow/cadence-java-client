package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DescribeDomainResponse {
  private DomainInfo domainInfo;
  private DomainConfiguration configuration;
  private DomainReplicationConfiguration replicationConfiguration;
  private long failoverVersion;
  private boolean isGlobalDomain;
  private FailoverInfo failoverInfo;
}
