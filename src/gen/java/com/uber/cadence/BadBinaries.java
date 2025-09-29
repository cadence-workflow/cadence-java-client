package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class BadBinaries {
  private Map<String, BadBinaryInfo> binaries = new HashMap<>();;
}
