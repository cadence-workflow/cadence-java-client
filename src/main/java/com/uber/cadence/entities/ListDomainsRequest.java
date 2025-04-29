package com.uber.cadence.entities;

import lombok.Data;

@Data
public class ListDomainsRequest {
    private int pageSize;
    private byte[] nextPageToken;
}
