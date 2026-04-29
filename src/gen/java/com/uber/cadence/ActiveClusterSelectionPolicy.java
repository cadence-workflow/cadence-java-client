package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ActiveClusterSelectionPolicy {
  private ClusterAttribute clusterAttribute;
  private ActiveClusterSelectionStrategy strategy;
  private String stickyRegion;
  private String externalEntityType;
  private String externalEntityKey;
}
