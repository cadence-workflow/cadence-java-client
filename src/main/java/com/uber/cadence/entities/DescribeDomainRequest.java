package com.uber.cadence.entities;

import lombok.Data;

@Data
public class DescribeDomainRequest {
    private String name;
    private String uuid;
}
