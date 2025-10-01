package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RespondCrossClusterTasksCompletedResponse {
  private List<CrossClusterTaskRequest> tasks = new ArrayList<>();;
}
