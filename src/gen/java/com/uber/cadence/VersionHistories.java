package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class VersionHistories {
  private int currentVersionHistoryIndex;
  private List<VersionHistory> histories = new ArrayList<>();;
}
