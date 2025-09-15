package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ListDomainsRequest {
  private int pageSize;
  private byte[] nextPageToken;
}
