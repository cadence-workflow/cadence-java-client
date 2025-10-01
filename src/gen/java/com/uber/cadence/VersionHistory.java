package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class VersionHistory {
  private byte[] branchToken;
  private List<VersionHistoryItem> items = new ArrayList<>();;
}
