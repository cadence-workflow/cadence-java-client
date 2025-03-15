package com.uber.cadence.largeblob;

import com.uber.cadence.converter.DataConverter;
import com.uber.cadence.converter.JsonDataConverter;

import java.time.Duration;

public class Configuration {

    private Storage storage;
    private DataConverter dataConverter;
    private Long maxBytes;
    private Duration ttl;

    private Configuration() {
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private Storage storage;
        private DataConverter dataConverter = JsonDataConverter.getInstance();
        private Duration ttl;
        private Long maxBytes = 4096L;

        public Configuration build() {
            if (storage == null) {
                throw new IllegalArgumentException("storage must be provided");
            }

            Configuration configuration = new Configuration();
            configuration.storage = this.storage;
            configuration.dataConverter = this.dataConverter;
            configuration.ttl = this.ttl;
            configuration.maxBytes = this.maxBytes;
            return configuration;
        }

        public Builder setDataConverter(DataConverter dataConverter) {
            this.dataConverter = dataConverter;
            return this;
        }

        public Builder setStorage(Storage storage) {
            this.storage = storage;
            return this;
        }

        public Builder setTtl(Duration ttl) {
            this.ttl = ttl;
            return this;
        }

        public Builder setMaxBytes(Long maxBytes) {
            this.maxBytes = maxBytes;
            return this;
        }
    }

    public Storage getStorage() {
        return storage;
    }

    public DataConverter getDataConverter() {
        return dataConverter;
    }

    public Duration getTtl() {
        return ttl;
    }

    public Long getMaxBytes() {
        return maxBytes;
    }
}
