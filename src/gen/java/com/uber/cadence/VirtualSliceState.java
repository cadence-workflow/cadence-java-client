package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class VirtualSliceState {
  private TaskRange taskRange;
  private Predicate predicate;
}
