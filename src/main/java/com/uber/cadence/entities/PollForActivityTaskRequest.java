package com.uber.cadence.entities;

import lombok.Data;

@Data
public class PollForActivityTaskRequest {
    private String domain;
    private TaskList taskList;
    private TaskListMetadata taskListMetadata;
    private String identity;
}
