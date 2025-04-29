package com.uber.cadence.entities;

import lombok.Data;

@Data
public class BadBinaryInfo {
    private String reason;
    private String operator;
    private Long createdTimeNano;
}
