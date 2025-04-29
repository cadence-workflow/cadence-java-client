package com.uber.cadence.entities;

import lombok.Data;

@Data
public class PollForDecisionTaskRequest {
    private String domain;
    private TaskList taskList;
    private String binaryChecksum;
    private String identity;
}
