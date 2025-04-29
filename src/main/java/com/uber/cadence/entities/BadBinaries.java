package com.uber.cadence.entities;

import java.util.Map;
import lombok.Data;

@Data
public class BadBinaries {
    private Map<String, BadBinaryInfo> binaries;
}
