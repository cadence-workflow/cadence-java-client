package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FailoverDomainRequest {
  private String domainName;
  private String domainActiveClusterName;
  private ActiveClusters activeClusters;
  private String reason;
}
