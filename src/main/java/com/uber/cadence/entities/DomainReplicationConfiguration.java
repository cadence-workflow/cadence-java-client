package com.uber.cadence.entities;

import java.util.List;
import lombok.Data;

@Data
public class DomainReplicationConfiguration {
    private String activeClusterName;
    private List<ClusterReplicationConfiguration> clusters;
}
