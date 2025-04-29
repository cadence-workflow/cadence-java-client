package com.uber.cadence.entities;

import lombok.Data;

@Data
public class SupportedClientVersions {
    private String goSdk;
    private String javaSdk;
    private String cli;
    private String ui;
}
