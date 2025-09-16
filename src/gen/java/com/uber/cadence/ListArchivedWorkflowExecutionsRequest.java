package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ListArchivedWorkflowExecutionsRequest {
  private String domain;
  private int pageSize;
  private byte[] nextPageToken;
  private String query;
}
