package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Memo {
  private Map<String, byte[]> fields = new HashMap<>();;
}
