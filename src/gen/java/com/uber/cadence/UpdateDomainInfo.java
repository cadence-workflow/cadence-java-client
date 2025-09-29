package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class UpdateDomainInfo {
  private String description;
  private String ownerEmail;
  private Map<String, String> data = new HashMap<>();;
}
