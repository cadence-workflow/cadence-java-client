package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DomainIDPredicateAttributes {
  private List<String> domainIDs = new ArrayList<>();;
  private boolean isExclusive;
}
