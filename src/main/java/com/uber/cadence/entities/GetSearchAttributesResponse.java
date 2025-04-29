package com.uber.cadence.entities;

import lombok.Data;
import java.util.Map;

@Data
public class GetSearchAttributesResponse {
    private Map<String, IndexedValueType> keys;
}
