package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DescribeShardDistributionResponse {
  private int numberOfShards;
  private Map<Integer, String> shards = new HashMap<>();;
}
