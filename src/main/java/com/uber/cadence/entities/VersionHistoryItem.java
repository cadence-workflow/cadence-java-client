package com.uber.cadence.entities;

import lombok.Data;

@Data
public class VersionHistoryItem {
    private Long eventId;
    private Long version;
}
