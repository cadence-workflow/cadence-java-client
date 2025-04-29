package com.uber.cadence.entities;

import java.util.Map;
import lombok.Data;

@Data
public class DomainInfo {
    private String name;
    private DomainStatus status;
    private String description;
    private String ownerEmail;
    private Map<String, String> data;
    private String uuid;
}
