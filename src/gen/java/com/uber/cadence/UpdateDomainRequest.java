package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class UpdateDomainRequest {
  private String name;
  private UpdateDomainInfo updatedInfo;
  private DomainConfiguration configuration;
  private DomainReplicationConfiguration replicationConfiguration;
  private String securityToken;
  private String deleteBadBinary;
  private int failoverTimeoutInSeconds;
}
