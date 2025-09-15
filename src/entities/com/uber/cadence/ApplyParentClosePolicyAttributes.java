package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ApplyParentClosePolicyAttributes {
  private String childDomainID;
  private String childWorkflowID;
  private String childRunID;
  private ParentClosePolicy parentClosePolicy;
}
