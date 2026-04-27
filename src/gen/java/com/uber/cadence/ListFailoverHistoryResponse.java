package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ListFailoverHistoryResponse {
  private List<FailoverEvent> failoverEvents = new ArrayList<>();;
  private byte[] nextPageToken;
}
