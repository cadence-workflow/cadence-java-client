package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MarkerRecordedEventAttributes {
  private String markerName;
  private byte[] details;
  private long decisionTaskCompletedEventId;
  private Header header;
}
