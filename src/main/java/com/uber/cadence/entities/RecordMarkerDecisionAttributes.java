package com.uber.cadence.entities;

import lombok.Data;

@Data
public class RecordMarkerDecisionAttributes {
    private String markerName;
    private byte[] details;
    private Header header;
}
