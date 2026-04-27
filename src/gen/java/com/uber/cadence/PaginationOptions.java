package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class PaginationOptions {
  private int pageSize;
  private byte[] nextPageToken;
}
