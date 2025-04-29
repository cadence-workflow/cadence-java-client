package com.uber.cadence.entities;

import lombok.Data;

@Data
public class DeprecateDomainRequest {
    private String name;
    private String securityToken;
}
