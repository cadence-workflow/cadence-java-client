package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class GetTaskListsByDomainResponse {
  private Map<String, DescribeTaskListResponse> decisionTaskListMap;
  private Map<String, DescribeTaskListResponse> activityTaskListMap;
}
