package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DescribeHistoryHostResponse {
  private int numberOfShards;
  private List<Integer> shardIDs = new ArrayList<>();;
  private DomainCacheInfo domainCache;
  private String shardControllerStatus;
  private String address;
}
