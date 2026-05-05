# Timer to Histogram Migration

## Overview

This document describes the migration from timer metrics to histogram metrics in the Cadence Java client. The migration uses a dual-emit pattern where **both timer and histogram metrics are always emitted**, allowing for gradual migration of dashboards and alerts without requiring a coordinated flag day.

## Why Migrate?

Timers and histograms serve similar purposes (measuring latencies and durations) but have different characteristics:

- **Timers**: Legacy approach, currently used throughout the codebase
- **Histograms**: More flexible, better support for custom buckets and percentile calculations

## Migration Strategy

### Phase 1: Dual Emission (Current)

Both timer and histogram metrics are emitted simultaneously:

```java
// Old code:
Stopwatch sw = scope.timer(MetricsType.DECISION_POLL_LATENCY).start();
// ... do work ...
sw.stop();

// New code (dual emit):
DualStopwatch sw = MetricsEmit.startLatency(
    scope,
    MetricsType.DECISION_POLL_LATENCY,
    HistogramBuckets.DEFAULT_1MS_100S
);
// ... do work ...
sw.stop(); // Records to BOTH timer and histogram
```

### Phase 2: Dashboard/Alert Migration (Next)

Update all dashboards and alerts to use histogram metrics instead of timer metrics. This can be done gradually since both are being emitted.

### Phase 3: Remove Timer Emission (Future)

Once all dashboards/alerts are migrated, remove timer emission:

```java
// Future code (histogram only):
Stopwatch sw = scope.histogram(
    MetricsType.DECISION_POLL_LATENCY,
    HistogramBuckets.DEFAULT_1MS_100S
).start();
// ... do work ...
sw.stop();
```

## Helper Classes

### HistogramBuckets

Defines standard bucket configurations:

- `DEFAULT_1MS_100S`: For most latency measurements (1ms to 100s range)
  - Fine-grained: 1ms steps from 1-10ms
  - Medium-grained: 10ms steps from 10-100ms
  - Coarse: 100ms steps from 100ms-1s
  - Second-level: 1s steps from 1-100s
  - Use for: Most RPC calls, decision/activity poll, execution latencies

- `LOW_1MS_100S`: Low-resolution version for high-cardinality metrics (1ms to 100s)
  - Approximately half the buckets of DEFAULT_1MS_100S
  - Use for: Per-activity-type, per-workflow-type metrics with high cardinality

- `HIGH_1MS_24H`: For long-running operations (1ms to 24 hours)
  - Extended range for multi-hour workflows
  - Use for: Workflow end-to-end latency, long-running activities

- `MID_1MS_24H`: Lower-resolution version of HIGH_1MS_24H
  - Fewer buckets than HIGH_1MS_24H
  - Use for: When HIGH_1MS_24H's cardinality is too high

### MetricsEmit

Provides dual-emit helper methods:

- `emitLatency(scope, name, duration, buckets)`: Directly record a duration
- `startLatency(scope, name, buckets)`: Create a dual stopwatch

### DualStopwatch

A stopwatch wrapper that records to both timer and histogram when `.stop()` is called.

## Migration Checklist

For each timer metric:

1. ✅ Identify the timer usage (e.g., `scope.timer(name).start()`)
2. ✅ Replace with `MetricsEmit.startLatency(scope, name, buckets)`
3. ✅ Choose appropriate bucket configuration (typically `HistogramBuckets.DEFAULT_1MS_100S`)
4. ✅ Verify both metrics are being emitted
5. ⏳ Update dashboards to use histogram metric
6. ⏳ Update alerts to use histogram metric
7. ⏳ (Future) Remove timer emission

## Example Conversions

### Example 1: Poll Latency

```java
// Before:
Stopwatch sw = scope.timer(MetricsType.DECISION_POLL_LATENCY).start();
PollForDecisionTaskResponse result = service.PollForDecisionTask(request);
sw.stop();

// After:
DualStopwatch sw = MetricsEmit.startLatency(
    scope,
    MetricsType.DECISION_POLL_LATENCY,
    HistogramBuckets.DEFAULT_1MS_100S
);
PollForDecisionTaskResponse result = service.PollForDecisionTask(request);
sw.stop();
```

### Example 2: Execution Latency

```java
// Before:
Stopwatch sw = metricsScope.timer(MetricsType.ACTIVITY_EXEC_LATENCY).start();
Result response = handler.handle(task, metricsScope, false);
sw.stop();

// After:
DualStopwatch sw = MetricsEmit.startLatency(
    metricsScope,
    MetricsType.ACTIVITY_EXEC_LATENCY,
    HistogramBuckets.DEFAULT_1MS_100S
);
Result response = handler.handle(task, metricsScope, false);
sw.stop();
```

### Example 3: Direct Duration Recording

```java
// Before:
Duration scheduledToStartLatency = Duration.between(scheduledTime, startedTime);
scope.timer(MetricsType.DECISION_SCHEDULED_TO_START_LATENCY).record(scheduledToStartLatency);

// After:
Duration scheduledToStartLatency = Duration.between(scheduledTime, startedTime);
MetricsEmit.emitLatency(
    scope,
    MetricsType.DECISION_SCHEDULED_TO_START_LATENCY,
    scheduledToStartLatency,
    HistogramBuckets.DEFAULT_1MS_100S
);
```

## Testing

The migration preserves existing timer behavior while adding histogram emission, so:

- Existing timer-based tests continue to work
- Existing timer-based dashboards/alerts continue to work
- New histogram metrics are available for gradual migration

## Timeline

1. **Now**: Dual emission in place, both metrics available
2. **Next Quarter**: Migrate dashboards and alerts to histograms
3. **Future Release**: Remove timer emission, histogram-only

## Questions?

Contact the Cadence team for guidance on specific metrics or migration questions.
