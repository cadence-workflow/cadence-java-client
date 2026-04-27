package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Predicate {
  private PredicateType predicateType;
  private UniversalPredicateAttributes universalPredicateAttributes;
  private EmptyPredicateAttributes emptyPredicateAttributes;
  private DomainIDPredicateAttributes domainIDPredicateAttributes;
}
