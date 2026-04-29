package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FailoverEvent {
  private String id;
  private long createdTime;
  private FailoverType failoverType;
  private List<ClusterFailover> clusterFailovers = new ArrayList<>();;
}
