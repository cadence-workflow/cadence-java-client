package com.uber.cadence.entities;

import lombok.Data;

@Data
public class DataBlob {
    private byte[] data;
    private EncodingType encodingType;
}
