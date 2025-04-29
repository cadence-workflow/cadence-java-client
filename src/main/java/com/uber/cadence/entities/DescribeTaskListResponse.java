package com.uber.cadence.entities;

import java.util.List;
import lombok.Data;

@Data
public class DescribeTaskListResponse {
    private List<PollerInfo> pollers;
    private TaskListStatus taskListStatus;
}
