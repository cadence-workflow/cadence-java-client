package com.uber.cadence.entities;

import lombok.Data;

@Data
public class ListTaskListPartitionsRequest {
    private String domain;
    private TaskList taskList;
}
