package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ListDomainsResponse {
  private List<DescribeDomainResponse> domains = new ArrayList<>();;
  private byte[] nextPageToken;
}
