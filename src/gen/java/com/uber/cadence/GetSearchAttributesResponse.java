package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class GetSearchAttributesResponse {
  private Map<String, IndexedValueType> keys = new HashMap<>();;
}
