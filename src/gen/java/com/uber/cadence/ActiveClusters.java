package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ActiveClusters {
  private Map<String, ActiveClusterInfo> activeClustersByRegion = new HashMap<>();;
  private Map<String, ClusterAttributeScope> activeClustersByClusterAttribute = new HashMap<>();;
}
