package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ClusterFailover {
  private ActiveClusterInfo fromCluster;
  private ActiveClusterInfo toCluster;
  private ClusterAttribute clusterAttribute;
}
