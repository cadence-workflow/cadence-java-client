package com.uber.cadence.entities;

import lombok.Data;
import java.util.List;

@Data
public class ListDomainsResponse {
    private List<DescribeDomainResponse> domains;
    private byte[] nextPageToken;
}
