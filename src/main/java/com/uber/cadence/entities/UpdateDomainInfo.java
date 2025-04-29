package com.uber.cadence.entities;

import java.util.Map;
import lombok.Data;

@Data
public class UpdateDomainInfo {
    private String description;
    private String ownerEmail;
    private Map<String, String> data;
}
