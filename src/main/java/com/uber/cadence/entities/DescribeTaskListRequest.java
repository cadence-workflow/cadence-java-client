package com.uber.cadence.entities;

import lombok.Data;

@Data
public class DescribeTaskListRequest {
    private String domain;
    private TaskList taskList;
    private TaskListType taskListType;
    private boolean includeTaskListStatus;
}
