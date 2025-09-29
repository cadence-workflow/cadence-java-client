package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DomainInfo {
  private String name;
  private DomainStatus status;
  private String description;
  private String ownerEmail;
  private Map<String, String> data = new HashMap<>();;
  private String uuid;
}
