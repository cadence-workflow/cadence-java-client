package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class GetWorkflowExecutionHistoryResponse {
  private History history;
  private List<DataBlob> rawHistory = new ArrayList<>();;
  private byte[] nextPageToken;
  private boolean archived;
}
