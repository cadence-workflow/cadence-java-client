package com.uber.cadence.entities;

import lombok.Data;
import java.util.List;

@Data
public class GetWorkflowExecutionHistoryResponse {
    private History history;
    private List<DataBlob> rawHistory;
    private byte[] nextPageToken;
    private boolean archived;
}
