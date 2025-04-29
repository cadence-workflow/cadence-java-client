package com.uber.cadence.entities;

import java.util.List;
import lombok.Data;

@Data
public class History {
    private List<HistoryEvent> events;
}
