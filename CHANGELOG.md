# Changelog

## Unreleased
- Add GetVersionOptions to Workflow.getVersion() for controlled version selection (executeWithVersion, executeWithMinVersion)

## 3.12.7
- Release use versions from git tags instead of static value (#1002)
- support for testrunner for java in vscode (#1001)
- Fix(RequestMapper): Correctly map all fields (#1000)
- add github action for version releases to Sonatype (#996)

## 3.12.6
- Adding logs to internal shadower to check for failures log (#918)
- Removing fossa as it is migrated to snyk (#919)
- Add codecov (#920)
- Add missing proto to thrift enum mappings and unit tests for both EnumMappers (#924)
- Remove unused code from cadence.internal package (#921)
- Refactor Test environment initialization to CadenceTestRule from WorkflowTest. (#923)
- upgrade mockito to 4.5.1 (#929)
- Report child workflow started synchronously (#928)
- Fix incorrect exception handling in WorkflowTchannelClient and add unit test coverage (#931)
- Fix WorkflowQueue#offer and increase test coverage (#932)
- Fix ActivityCompletionClient cancellation and failure by WorkflowExecution (#930)
- Fix incorrect exception for unsupported method in WorkflowService (#933)
- Fix flakiness in ManualActivityCompletionWorkflowTest (#937)
- Fix Domain GRPC->Thrift mapping and increase test coverage (#934)
- thrift install script (#939)
- Fix unimplementented methods of TestWorkflowEnvironment (#945)
- Fix bug in QueryWorkflowParameters.toString (#948)
- Fix NPE in RetryParameters and add unit tests to ExecuteActivityParameters (#946)
- Add copy function and unit tests to StartWorkflowExecutionParameters (#953)
- Fix Error handling and add test coverage for Thrift2ProtoAdapter (#950)
- Fix unhandled exceptions in WorkflowServiceTChannel (#954)
- Fix nil pointer issue (#982)
- add log and metrics on TBase message dataconverter usage (#986)
- added ExecutorWrapper in WorkerFactoryOptions (#988)
- Set skipArchival to true for getResultAsync (#990)

## 3.12.5
- Fix transient test failures
- Fix Request ID handling for server-side idempotency

## 3.12.4
- Fix incorrect span activation for local activities

## 3.12.3
- Remove unused direct dependency of com.google.http-client
- Fix context propagation bug that would link two parents in some cases

## 3.12.2
- Downgrade java-jwt dependency to 3.10.2 to fix #900

## 3.12.1
- Refactor ActivityTaskExecutor to use ActivityTask interface (#897)
- Fixed memory leak caused by incorrect context deactivation (#896)
- Added unsetCurrentContext to ContextPropagator interface (#898)

## 3.12.0
- Added support for new Async APIs for starting large numbers of workflows.
- Added support for two-legged OAuth flow
- Added support for standard JWT exp and iat claims
- Updated WorkerOptions to default to the Tracer used to initialize WorkflowClient.
- Fixed ClassCastException when using Promise#allOf(Promise<?>...)
- Fixed NullPointerException in ContextPropagation when Header is present but fields is null

## 3.11.0
- Added opentracing support in workflow lifecycles #876

## 3.10.1
- Fixed the bug: workflow already started for migration
- Populate tasklistkind in poll request

## 3.10.0
- Added migrationIWorkflowService
- Added migrationInterceptor
- Moved out TracingInterceptor from WorkflowTest to a separate package

## 3.9.1
- Added initialization of contextPropagators from passed options
- Updated rpc-caller header of grpc

## 3.9.0
### Added
- Add ability to override activity options
- Add executeWorkflow method to WorkflowInterceptor
- Add isolation group to service client options so that requests are populated with isolation group header
- Add cause tag for transient poll failures
- Add workflow start event and data converter to workflow info
### Changed
- Update dependencies
- Remove unused code
- Remove reflective objects exception due to failing builds
- Fix the gradle project for M1 macs
- Add cronschedule to history start event and fix isCron check of listworkflow for test env

## 3.8.1
- remove opentelemetry-bom dependency.
## 3.8.0
- Graceful shutdown based on sigterm handler
- Adding cross domain signal/child workflow creation support
- Adding open telemetry support so that open telemetry context is propagated to cadence-server
- Adding dynamic poller support for more efficient resource usage.
## 3.7.3
- Fix describe domain and update domain grpc calls to include domainConfiguration and replication configuration in response.
## 3.7.2
- Fix gRPC adapter for List APIs.

## 3.7.1
### Added
- Add gRPC channel to client options.

## 3.7.0
### Added
- Add gRPC compatibility layer.

## 3.6.2
### Added
- Add refresh tasks API to provide ability to refresh tasks on stuck workflows.

## 3.6.1
### Changed
- Fix a bug for PollOnlyIfExecutorHasCapacity flag about pool size.

## 3.6.0
### Added
- Introduce experimental flag and add logic for capacity based polling.

## 3.5.1
### Added
- Add CadenceChangeVersion support.
- Allow using other tags in metric reporter.
- Add metric tag to differentiate long-poll and normal request.
### Changed
- Fix identity for sticky worker.
- Improve contributing guide.

## 3.5.0
### Changed
- Fix consistent query interface which caused overloading ambiguity with variable argument

## 3.4.0
### Added
- Support delay start for cron workflows
- Add memo and search attributes to child workflow

## 3.3.0
### Added
- Added feature flags to ship breaking chgit loganges without impacting old behavior.

## 3.2.0
### Added
- Add Health check API to worker and service interface.
### Changed
- Replace sticky tasklist metrics scope with the same value.

## 3.1.0
### Added
- [New feature] Workflow Shadowing worker and tests. The new shadowing feature provides:
1. WorkflowShadower to run integration tests to replay workflow traffic from local/test environment.
2. ShadowingWorker to run worker
- [Breaking changes] Introduce new error type WorkflowAlreadyCompletedError in APIs SignalWorkflowExecution, CancelWorkflowExecution and TerminateWorkflowExecution.
- Add Java 11 support.
- Add binary checksum support.
- Add activity worker thread count metrics.
### Changed
- Add all missing version marker before upsert searchattributes.

## 3.0.0
### Added
- [New feature] Activity Local Dispatch: Allows Cadence worker to dispatch activity tasks through local tunnel after ScheduleActivity decisions are made. This is a performance optimization to reduce activity scheduling efforts.
- Pass TaskListActivitiesPerSecond to activity worker and remove the limit.
- Add missing workflowtype and activitytype metric tags.
### Changed
- [Breaking changes] Refactoring in Worker initialization path:
  - Worker.Factory -> WorkerFactory
  - Worker.FactoryOptions -> WorkerFactoryOptions
  - PollerOptions.Builder -> PollerOptions.newBuilder
  - SingleWorkerOptions.Builder -> SingleWorkerOptions.newBuilder
  - Added WorkerOptions Builder
  - WorkflowClient.newInstance(IWorkflowService, Domain, WorkflowClientOptions) -> WorkflowClient.newInstance(IWorkflowService, WorkflowClientOptions)
  - WorkflowClientOptions.Builder -> WorkflowClientOptions.newBuilder
  - Testing framework
- Fix activity end-to-end latency metric.
- Fix newProxyInstance with the correct class.
- Fix bug in worker.isSuspended().
- Improve worker start/shutdown logic.
- Improve retry logic.
- Fix race condition during serialization.

## 2.7.8
- Fix get raw history
- Improve signal processing error and log
- Fix replay error when querying workflow that contains activity retry

## 2.7.6
- Fix getVersion override when added new version
- Add async signal to untypedstub
- Fix RetryOptions.addDoNotRetry
- Add missing metrics from go client
- Fix a bug in setting retry expiration while getting history
- Fix start async return

## 2.7.5
- Added supports contextPropagators for localActivity

## v2.7.4
- Fix prometheus reporting issue
- Fix Promise.allOf should not block on empty input
- Misc: Added project directory to sourceItems path
- Add async start to untype stub

## v2.7.3
- Add wf type tag in decider metrics scope
- Fix WorkflowStub.fromTyped method
- Added missing fields to local activity task
- Honor user timeout for get workflow result

## v2.7.2
- Fix leak in Async GetWorkflowExecutionHistory
- Fix context timeout in execute workflow

## v2.7.1
- Fix a bug in build.gradle that prevented javadoc and sources from being published

## v2.7.0
- Add ParentClosePolicy to child workflows and also expose parent execution info for child workflows
- Add context propagation
- Fix various bugs around test workflow service and test mutable state implementation
- Use thrift IDLs from uber/cadence-idl repo as a submodule
- Various dependency updates including Docker base image and Gradle wrapper
- Miscellaneous bug fixes

## v2.6.3
- Add Upsert Search Attributes
- Support get search attributes inside workflow

## v2.6.2
- Cleanup threads created in signal method on decider close
- Fixed exception propagation from child workflows

## v2.6.1
- Add missing metrics and tags
- Add metrics for SCHEDULED_TO_STAR latency
- Support filtering query based on close status
- Documentation improvements

## v2.6.0
- Fix thread leak on non-deterministic error
- Support Search Attributes on Start workflow
- Make Worker.addWorkflowImplementationFactory method support 'options'

## v2.5.2
- Add saga class that helps user to implement saga pattern in Cadence
- Add activity tasklist rate limiter option to worker options

## v2.5.1
- Fix busy loop in local activity poller if there is no task
- Fix an issue in get history timeout
- Lock decider while processing
- Timer firing fix

## v2.5.0
- Local activities
- Make sure signals are applied in the same order as they appear in history
- Fix retry option without expiration in test env
- Maintain correct runID during reset for random ID generation
- Default retry policy for Cadence service API calls
- Change markers to use headers to serialize internal fields
- Miscellaneous stability and debuggability fixes

## v2.4.2
- Support Memo in visibility
- Fix getVersion without decision event
- Make NoopScope metric scope really a no-op operation
- Add some more fields in activity info
- Wire workflow id reuse policy in start workflow execution params
- Add missing metrics tags for activity and decision task processing
- Multiple fixes to get build and unit-tests passing when building cadence-java-client from Windows.
- Allow data converter to handle non-serializable throwables in the cause chain

## v2.4.1
- Update default long poll timeout to 2min to match server side config
- Fix deadlock in sticky decider cache eviction
- Fix cron schedule merge issue in child workflow option

## v2.4.0
- Fixed InternalServiceError Error message on continue_as_new
- Correctly calculate workflow e2e latency
- Exposing CancellationScope.run method
- Add TBase and TEnum type adapter for JsonDataConverter
- Cron child workflow

## v2.3.1
- Added support for SignalWithStart Service API
- Expose methods in TChannel service to allow user to add headers in Thrift request

## v2.3.0
- Added cron schedule support.
- Fix infinite retryer in activity and workflow worker due to non-retryable error.
- Fixed hanging on testEnv.close when testEnv was not started.
- Fix for NPE when method has base type return type like int.
- Fixed JsonDataConverter to correctly report non serializable exceptions.

## v2.2.0
- Added support for workflow and activity server side retries.
- Clean worker shutdown. Replaced Worker shutdown(Duration) with Worker shutdown, shutdownNow and awaitTermination.
- Fixed thread exhaustion with a large number of parallel async activities.

## v2.1.3
- Added RPC headers needed to enable sticky queries. Before this change
queries did not used cached workflows.

## v2.1.2
- Requires minimum server release v0.4.0
- Introduced WorkerFactory and FactoryOptions
- Added sticky workflow execution, which is caching of a workflow object between decisions. It is enabled by default,
to disable use FactoryOptions.disableStickyExecution property.
- Updated Thrift to expose new types of service exceptions: ServiceBusyError, DomainNotActiveError, LimitExceededError
- Added metric for corrupted signal as well as metrics related to caching and evictions.

## v1.0.0 (06-04-2018)
- POJO workflow, child workflow, activity execution.
- Sync and Async workflow execution.
- Query and Signal workflow execution.
- Test framework.
- Metrics and Logging support in client.
- Side effects, mutable side effects, random uuid and workflow getVersion support.
- Activity heartbeat throttling.
- Deterministic retry of failed operation.
