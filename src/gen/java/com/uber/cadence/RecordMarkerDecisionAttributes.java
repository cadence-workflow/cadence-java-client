package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RecordMarkerDecisionAttributes {
  private String markerName;
  private byte[] details;
  private Header header;
}
