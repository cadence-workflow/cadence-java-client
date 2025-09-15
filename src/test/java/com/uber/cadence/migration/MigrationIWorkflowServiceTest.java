/*
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package com.uber.cadence.migration;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.uber.cadence.*;
import com.uber.cadence.serviceclient.ClientOptions;
import com.uber.cadence.serviceclient.IWorkflowService;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class MigrationIWorkflowServiceTest {

  @Mock private IWorkflowService serviceOld;

  @Mock private IWorkflowService serviceNew;
  private MigrationIWorkflowService migrationService;

  @Before
  public void setUp() {
    serviceOld = mock(IWorkflowService.class);
    serviceNew = mock(IWorkflowService.class);
    migrationService =
        new MigrationIWorkflowService(serviceOld, "domainOld", serviceNew, "domainNew");
  }

  // No previous workflow found - launch a workflow in new cluster
  @Test
  public void testStartWorkflowExecution_startNewWorkflow() throws BaseError {

    StartWorkflowExecutionRequest startRequest =
        new StartWorkflowExecutionRequest()
            .setWorkflowId("123")
            .setWorkflowType(new WorkflowType().setName("sampleWorkflow"))
            .setRequestId("123");

    DescribeWorkflowExecutionRequest describeWorkflowExecutionRequest =
        new DescribeWorkflowExecutionRequest()
            .setDomain("domainNew")
            .setExecution(new WorkflowExecution().setWorkflowId("123"));
    DescribeWorkflowExecutionRequest describeOldWorkflowExecutionRequest =
        new DescribeWorkflowExecutionRequest()
            .setDomain("domainOld")
            .setExecution(new WorkflowExecution().setWorkflowId("123"));

    // Verify DescribeWorkflowExecution calls for both services return null
    when(serviceNew.DescribeWorkflowExecution(describeWorkflowExecutionRequest)).thenReturn(null);
    when(serviceOld.DescribeWorkflowExecution(describeOldWorkflowExecutionRequest))
        .thenReturn(null);

    StartWorkflowExecutionResponse responseNew = new StartWorkflowExecutionResponse();
    when(serviceNew.StartWorkflowExecution(startRequest)).thenReturn(responseNew);

    StartWorkflowExecutionResponse response = migrationService.StartWorkflowExecution(startRequest);

    verify(serviceNew, times(1)).DescribeWorkflowExecution(describeWorkflowExecutionRequest);
    verify(serviceOld, times(1)).DescribeWorkflowExecution(describeOldWorkflowExecutionRequest);

    assertEquals(responseNew, response);

    // Verify that the StartWorkflowExecution method is only called once on serviceNew
    verify(serviceNew, times(1)).StartWorkflowExecution(startRequest);

    // Verify that no other methods are called
    verifyNoMoreInteractions(serviceNew);
    verifyNoMoreInteractions(serviceOld);
  }

  // Previous running workflow found: expected to launch a wf in the old cluster
  @Test
  public void testStartWorkflowExecution_startOldWorkflow() throws BaseError {

    StartWorkflowExecutionRequest startRequest =
        new StartWorkflowExecutionRequest()
            .setWorkflowId("123")
            .setWorkflowType(new WorkflowType().setName("sampleWorkflow"))
            .setRequestId("123");

    when(serviceNew.DescribeWorkflowExecution(any())).thenReturn(null);

    DescribeWorkflowExecutionResponse describeWorkflowExecutionResponse =
        new DescribeWorkflowExecutionResponse();
    when(serviceOld.DescribeWorkflowExecution(any())).thenReturn(describeWorkflowExecutionResponse);

    StartWorkflowExecutionResponse responseOld = new StartWorkflowExecutionResponse();
    when(serviceOld.StartWorkflowExecution(any())).thenReturn(responseOld);

    StartWorkflowExecutionResponse response = migrationService.StartWorkflowExecution(startRequest);

    // Verify interactions
    verify(serviceNew, times(1)).DescribeWorkflowExecution(any());
    verify(serviceOld, times(1)).DescribeWorkflowExecution(any());
    verify(serviceOld, times(1)).StartWorkflowExecution(any());

    // Verify that no other methods are called
    verifyNoMoreInteractions(serviceNew);
    verifyNoMoreInteractions(serviceOld);

    // Assert the response
    assertEquals(responseOld, response);
  }

  @Test
  public void testStartWorkflow_noWorkflowID() throws BaseError {
    StartWorkflowExecutionRequest startRequest =
        new StartWorkflowExecutionRequest()
            .setWorkflowType(new WorkflowType().setName("sampleWorkflow"))
            .setRequestId("123");

    StartWorkflowExecutionResponse mockResponse =
        new StartWorkflowExecutionResponse().setRunId("123");
    when(serviceNew.StartWorkflowExecution(any())).thenReturn(mockResponse);
    StartWorkflowExecutionResponse startWorkflowExecutionResponse =
        migrationService.StartWorkflowExecution(startRequest);
    verify(serviceNew, times(1)).StartWorkflowExecution(any());

    assertEquals(startWorkflowExecutionResponse, mockResponse);
  }

  @Test
  public void testStartWorkflow_errorInDescribeWorkflowExecution() throws BaseError {

    StartWorkflowExecutionRequest startRequest =
        new StartWorkflowExecutionRequest()
            .setWorkflowId("123")
            .setWorkflowType(new WorkflowType().setName("sampleWorkflow"))
            .setRequestId("123");

    when(serviceNew.DescribeWorkflowExecution(any())).thenReturn(null);
    when(serviceOld.DescribeWorkflowExecution(any())).thenReturn(null);
    StartWorkflowExecutionResponse startWorkflowExecutionResponse =
        migrationService.StartWorkflowExecution(startRequest);
    // Verify interactions
    verify(serviceNew, times(1)).DescribeWorkflowExecution(any());
    verify(serviceOld, times(1)).DescribeWorkflowExecution(any());

    assertNull(startWorkflowExecutionResponse);
  }

  @Test
  public void testListWorkflows_InitialRequest() throws BaseError {

    String domainNew = "test";
    int one = 1;

    ListWorkflowExecutionsRequest request =
        new ListWorkflowExecutionsRequest()
            .setDomain(domainNew)
            .setPageSize(one)
            .setNextPageToken((byte[]) null);

    ListWorkflowExecutionsResponse mockSingleResultResponse =
        new ListWorkflowExecutionsResponse().setExecutions(new ArrayList<>());

    WorkflowExecutionInfo executionInfo = new WorkflowExecutionInfo();
    executionInfo.setExecution(
        new WorkflowExecution().setWorkflowId("testWfId").setRunId("testRunId"));
    mockSingleResultResponse.getExecutions().add(executionInfo);
    mockSingleResultResponse.setNextPageToken("testToken".getBytes());

    // fetch from new cluster for initial request
    when(serviceNew.ListWorkflowExecutions(any())).thenReturn(mockSingleResultResponse);
    ListWorkflowExecutionsResponse response = migrationService.ListWorkflowExecutions(request);
    assertEquals(mockSingleResultResponse, response);
  }

  // calling old cluster when new cluster returns empty response
  @Test
  public void testListWorkflow_OldClusterCall() throws BaseError {

    String domainNew = "test";
    int one = 1;

    ListWorkflowExecutionsRequest request =
        new ListWorkflowExecutionsRequest()
            .setDomain(domainNew)
            .setPageSize(one)
            .setNextPageToken((byte[]) null);

    ListWorkflowExecutionsResponse mockEmptyResponse =
        new ListWorkflowExecutionsResponse()
            .setExecutions(new ArrayList<>())
            .setNextPageToken((byte[]) null);

    ListWorkflowExecutionsResponse mockSingleResultResponse =
        new ListWorkflowExecutionsResponse().setExecutions(new ArrayList<>());

    WorkflowExecutionInfo executionInfo = new WorkflowExecutionInfo();
    executionInfo.setExecution(
        new WorkflowExecution().setWorkflowId("testWfId").setRunId("testRunId"));
    mockSingleResultResponse.getExecutions().add(executionInfo);
    mockSingleResultResponse.setNextPageToken("testToken".getBytes());

    when(serviceNew.ListWorkflowExecutions(any())).thenReturn(mockEmptyResponse);
    ListWorkflowExecutionsResponse response = migrationService.ListWorkflowExecutions(request);
    assertEquals(mockEmptyResponse, response);

    when(serviceOld.ListWorkflowExecutions(any())).thenReturn(mockSingleResultResponse);
    response = migrationService.ListWorkflowExecutions(request);
    assertEquals(mockSingleResultResponse, response);
  }

  // if fetching from new cluster result size is less than pageSize, fetch additional records from
  // Old Cluster
  @Test
  public void testListWorkflow_fetchFromBothCluster() throws BaseError {
    String domainNew = "test";
    int one = 1;
    int two = 2;

    ListWorkflowExecutionsRequest request =
        new ListWorkflowExecutionsRequest()
            .setDomain(domainNew)
            .setPageSize(one)
            .setNextPageToken((byte[]) null);

    ListWorkflowExecutionsRequest requestTwoItems =
        new ListWorkflowExecutionsRequest()
            .setDomain(domainNew)
            .setPageSize(two)
            .setNextPageToken((byte[]) null);

    ListWorkflowExecutionsResponse mockSingleResultResponse =
        new ListWorkflowExecutionsResponse().setExecutions(new ArrayList<>());

    WorkflowExecutionInfo executionInfo = new WorkflowExecutionInfo();
    executionInfo.setExecution(
        new WorkflowExecution().setWorkflowId("testWfId").setRunId("testRunId"));
    mockSingleResultResponse.getExecutions().add(executionInfo);
    mockSingleResultResponse.setNextPageToken("testToken".getBytes());

    when(serviceOld.ListWorkflowExecutions(request)).thenReturn(mockSingleResultResponse);
    ListWorkflowExecutionsResponse response = migrationService.ListWorkflowExecutions(request);
    assertEquals(mockSingleResultResponse, response);

    when(serviceNew.ListWorkflowExecutions(requestTwoItems)).thenReturn(mockSingleResultResponse);
    response = migrationService.ListWorkflowExecutions(request);
    assertEquals(mockSingleResultResponse, response);
  }

  @Test
  public void testListWorkflows_emptyRequestTests() throws BaseError {

    // Test when request is null
    try {
      migrationService.ListWorkflowExecutions(null);
    } catch (BadRequestError e) {
      assertEquals("List request is null", e.getMessage());
    }

    // Test when domain is null
    try {
      migrationService.ListWorkflowExecutions(new ListWorkflowExecutionsRequest().setPageSize(10));
    } catch (BadRequestError e) {
      assertEquals("Domain is null or empty", e.getMessage());
    }
  }

  // Test when error returned from internal client, return same error
  @Test
  public void testListWorkflow_error() throws BaseError {
    String domainNew = "test";

    when(serviceNew.ListWorkflowExecutions(any())).thenReturn(null);
    ListWorkflowExecutionsResponse response =
        migrationService.ListWorkflowExecutions(
            new ListWorkflowExecutionsRequest().setDomain(domainNew));
    verify(serviceNew, times(1)).ListWorkflowExecutions(any());
    assertNull(response);
  }

  @Test
  public void testListWorkflow_FromClusterOnly() throws BaseError {

    String domain = "test";

    ListWorkflowExecutionsRequest request =
        new ListWorkflowExecutionsRequest()
            .setDomain(domain)
            .setPageSize(1)
            .setNextPageToken((byte[]) null);

    ListWorkflowExecutionsResponse mockEmptyResponse =
        new ListWorkflowExecutionsResponse()
            .setExecutions(new ArrayList<>())
            .setNextPageToken((byte[]) null);

    // Test fetch only from 'from' cluster
    when(serviceOld.ListWorkflowExecutions(any())).thenReturn(mockEmptyResponse);
    ListWorkflowExecutionsResponse response = migrationService.ListWorkflowExecutions(request);
    assertEquals(mockEmptyResponse, response);
  }

  @Test
  public void testListWorkflows_ResponseWithToken() throws BaseError {

    String domainNew = "test";

    ListWorkflowExecutionsRequest request =
        new ListWorkflowExecutionsRequest()
            .setDomain(domainNew)
            .setPageSize(1)
            .setNextPageToken((byte[]) null);

    ListWorkflowExecutionsResponse expectedResponseWithToken = new ListWorkflowExecutionsResponse();
    expectedResponseWithToken.setExecutions(new ArrayList<>());
    WorkflowExecutionInfo executionInfo1 = new WorkflowExecutionInfo();
    executionInfo1.setExecution(
        new WorkflowExecution().setWorkflowId("testWfId").setRunId("testRunId"));
    WorkflowExecutionInfo executionInfo2 = new WorkflowExecutionInfo();
    executionInfo2.setExecution(
        new WorkflowExecution().setWorkflowId("testWfId").setRunId("testRunId"));
    expectedResponseWithToken.getExecutions().add(executionInfo1);
    expectedResponseWithToken.getExecutions().add(executionInfo2);
    expectedResponseWithToken.setNextPageToken("totestToken".getBytes());

    when(serviceNew.ListWorkflowExecutions(any())).thenReturn(expectedResponseWithToken);
    ListWorkflowExecutionsResponse response = migrationService.ListWorkflowExecutions(request);
    assertEquals(expectedResponseWithToken, response);

    ListWorkflowExecutionsRequest requestTwoItems =
        new ListWorkflowExecutionsRequest()
            .setDomain(domainNew)
            .setPageSize(2)
            .setNextPageToken((byte[]) null);

    when(serviceNew.ListWorkflowExecutions(any())).thenReturn(expectedResponseWithToken);
    response = migrationService.ListWorkflowExecutions(requestTwoItems);
    assertEquals(expectedResponseWithToken, response);
  }

  @Test
  public void testScanWorkflow_InitialRequest() throws BaseError {

    String domainNew = "test";

    ListWorkflowExecutionsRequest request =
        new ListWorkflowExecutionsRequest()
            .setDomain(domainNew)
            .setPageSize(1)
            .setNextPageToken((byte[]) null);

    ListWorkflowExecutionsResponse mockSingleResultResponse =
        new ListWorkflowExecutionsResponse().setExecutions(new ArrayList<>());

    WorkflowExecutionInfo executionInfo = new WorkflowExecutionInfo();
    executionInfo.setExecution(
        new WorkflowExecution().setWorkflowId("testWfId").setRunId("testRunId"));
    mockSingleResultResponse.getExecutions().add(executionInfo);
    mockSingleResultResponse.setNextPageToken("testToken".getBytes());

    // Mock the serviceNew to return the expected response for the initial request.
    when(serviceNew.ScanWorkflowExecutions(any())).thenReturn(mockSingleResultResponse);

    // Perform the test and check if the response matches the expected result.
    ListWorkflowExecutionsResponse response = migrationService.ScanWorkflowExecutions(request);
    assertEquals(mockSingleResultResponse, response);
  }

  // Test scanWorkflow when new cluster returns an empty response and it falls back to the old
  // cluster.
  @Test
  public void testScanWorkflow_OldClusterCall() throws BaseError {

    String domainNew = "test";

    ListWorkflowExecutionsRequest request =
        new ListWorkflowExecutionsRequest()
            .setDomain(domainNew)
            .setPageSize(1)
            .setNextPageToken((byte[]) null);

    ListWorkflowExecutionsResponse mockEmptyResponse =
        new ListWorkflowExecutionsResponse()
            .setExecutions(new ArrayList<>())
            .setNextPageToken((byte[]) null);

    ListWorkflowExecutionsResponse mockSingleResultResponse =
        new ListWorkflowExecutionsResponse().setExecutions(new ArrayList<>());

    WorkflowExecutionInfo executionInfo = new WorkflowExecutionInfo();
    executionInfo.setExecution(
        new WorkflowExecution().setWorkflowId("testWfId").setRunId("testRunId"));
    mockSingleResultResponse.getExecutions().add(executionInfo);
    mockSingleResultResponse.setNextPageToken("testToken".getBytes());

    // Mock the serviceNew to return an empty response.
    when(serviceNew.ScanWorkflowExecutions(any())).thenReturn(mockEmptyResponse);

    // Perform the first test to check if the response is empty as the new cluster returned no
    // results.
    ListWorkflowExecutionsResponse response = migrationService.ScanWorkflowExecutions(request);
    assertEquals(mockEmptyResponse, response);

    // Mock the serviceOld to return the expected response.
    when(serviceOld.ScanWorkflowExecutions(any())).thenReturn(mockSingleResultResponse);

    // Perform the second test to check if the response is now populated with data from the old
    // cluster.
    response = migrationService.ScanWorkflowExecutions(request);
    assertEquals(mockSingleResultResponse, response);
  }

  @Test
  public void testScanWorkflow_FetchFromBothClusters() throws BaseError {

    String domainNew = "test";

    ListWorkflowExecutionsRequest request =
        new ListWorkflowExecutionsRequest()
            .setDomain(domainNew)
            .setPageSize(1)
            .setNextPageToken((byte[]) null);

    ListWorkflowExecutionsResponse mockSingleResultResponse =
        new ListWorkflowExecutionsResponse().setExecutions(new ArrayList<>());

    WorkflowExecutionInfo executionInfo = new WorkflowExecutionInfo();
    executionInfo.setExecution(
        new WorkflowExecution().setWorkflowId("testWfId").setRunId("testRunId"));
    mockSingleResultResponse.getExecutions().add(executionInfo);
    mockSingleResultResponse.setNextPageToken("testToken".getBytes());

    // Mock the serviceOld to return the expected response.
    when(serviceOld.ScanWorkflowExecutions(any())).thenReturn(mockSingleResultResponse);

    // Perform the first test to check if the response is populated with data from the old cluster.
    ListWorkflowExecutionsResponse response = migrationService.ScanWorkflowExecutions(request);
    assertEquals(mockSingleResultResponse, response);

    ListWorkflowExecutionsRequest requestTwoItems =
        new ListWorkflowExecutionsRequest()
            .setDomain(domainNew)
            .setPageSize(2)
            .setNextPageToken((byte[]) null);

    // Mock the serviceNew to return the expected response.
    when(serviceNew.ScanWorkflowExecutions(any())).thenReturn(mockSingleResultResponse);

    // Perform the second test to check if the response is now populated with data from the new
    // cluster.
    response = migrationService.ScanWorkflowExecutions(requestTwoItems);
    assertEquals(mockSingleResultResponse, response);
  }

  @Test
  public void testScanWorkflow_EmptyRequestTests() throws BaseError {

    // Test when the request is null.
    try {
      migrationService.ScanWorkflowExecutions(null);
    } catch (BadRequestError e) {
      assertEquals("List request is null", e.getMessage());
    }

    // Test when the domain is null.
    try {
      migrationService.ScanWorkflowExecutions(new ListWorkflowExecutionsRequest().setPageSize(10));
    } catch (BadRequestError e) {
      assertEquals("Domain is null or empty", e.getMessage());
    }
  }

  // Test when an error is returned from the internal client, and the response is null.
  @Test
  public void testScanWorkflow_Error() throws BaseError {

    String domainNew = "test";

    when(serviceNew.ListWorkflowExecutions(any())).thenReturn(null);
    ListWorkflowExecutionsResponse response =
        migrationService.ScanWorkflowExecutions(
            new ListWorkflowExecutionsRequest().setDomain(domainNew));
    verify(serviceNew, times(1)).ScanWorkflowExecutions(any());
    assertNull(response);
  }

  // Test scanWorkflow when fetching only from the 'from' cluster.
  @Test
  public void testScanWorkflow_FromClusterOnly() throws BaseError {

    String domain = "test";

    ListWorkflowExecutionsRequest request =
        new ListWorkflowExecutionsRequest()
            .setDomain(domain)
            .setPageSize(1)
            .setNextPageToken((byte[]) null);

    ListWorkflowExecutionsResponse mockEmptyResponse =
        new ListWorkflowExecutionsResponse()
            .setExecutions(new ArrayList<>())
            .setNextPageToken((byte[]) null);

    // Mock the serviceOld to return an empty response.
    when(serviceOld.ScanWorkflowExecutions(any())).thenReturn(mockEmptyResponse);

    // Perform the test to check if the response is empty as the new cluster returned no results.
    ListWorkflowExecutionsResponse response = migrationService.ScanWorkflowExecutions(request);
    assertEquals(mockEmptyResponse, response);
  }

  @Test
  public void testScanWorkflows_ResponseWithToken() throws BaseError {

    String domainNew = "test";

    ListWorkflowExecutionsRequest request =
        new ListWorkflowExecutionsRequest()
            .setDomain(domainNew)
            .setPageSize(1)
            .setNextPageToken((byte[]) null);

    ListWorkflowExecutionsResponse expectedResponseWithToken = new ListWorkflowExecutionsResponse();
    expectedResponseWithToken.setExecutions(new ArrayList<>());
    WorkflowExecutionInfo executionInfo1 = new WorkflowExecutionInfo();
    executionInfo1.setExecution(
        new WorkflowExecution().setWorkflowId("testWfId").setRunId("testRunId"));
    WorkflowExecutionInfo executionInfo2 = new WorkflowExecutionInfo();
    executionInfo2.setExecution(
        new WorkflowExecution().setWorkflowId("testWfId").setRunId("testRunId"));
    expectedResponseWithToken.getExecutions().add(executionInfo1);
    expectedResponseWithToken.getExecutions().add(executionInfo2);
    expectedResponseWithToken.setNextPageToken("totestToken".getBytes());

    // Mock the serviceNew to return the expected response with a token.
    when(serviceNew.ScanWorkflowExecutions(any())).thenReturn(expectedResponseWithToken);

    // Perform the first test to check if the response contains a token from the new cluster.
    ListWorkflowExecutionsResponse response = migrationService.ScanWorkflowExecutions(request);
    assertEquals(expectedResponseWithToken, response);

    ListWorkflowExecutionsRequest requestTwoItems =
        new ListWorkflowExecutionsRequest()
            .setDomain(domainNew)
            .setPageSize(2)
            .setNextPageToken((byte[]) null);

    // Perform the second test to check if the response contains a token from the new cluster for a
    // different page size.
    when(serviceNew.ScanWorkflowExecutions(any())).thenReturn(expectedResponseWithToken);
    response = migrationService.ScanWorkflowExecutions(requestTwoItems);
    assertEquals(expectedResponseWithToken, response);
  }

  @Test
  public void testCountWorkflow_bothClusterSuccess() throws BaseError {

    String domain = "test";
    String query = "";

    CountWorkflowExecutionsRequest request =
        new CountWorkflowExecutionsRequest().setDomain(domain).setQuery(query);
    CountWorkflowExecutionsResponse mockResponseOld =
        new CountWorkflowExecutionsResponse().setCount(2);
    CountWorkflowExecutionsResponse mockResponseNew =
        new CountWorkflowExecutionsResponse().setCount(3);

    CountWorkflowExecutionsResponse expectedResponse = new CountWorkflowExecutionsResponse();
    expectedResponse.setCount(5);

    // both clusters return successful response
    when(serviceOld.CountWorkflowExecutions(any())).thenReturn(mockResponseOld);
    when(serviceNew.CountWorkflowExecutions(any())).thenReturn(mockResponseNew);
    CountWorkflowExecutionsResponse response = migrationService.CountWorkflowExecutions(request);
    assertEquals(expectedResponse, response);
  }

  @Test
  public void testCountWorkflow_errorInOneCluster() throws BaseError {

    String domain = "test";
    String query = "";

    CountWorkflowExecutionsRequest request =
        new CountWorkflowExecutionsRequest().setDomain(domain).setQuery(query);
    CountWorkflowExecutionsResponse mockResponseOld =
        new CountWorkflowExecutionsResponse().setCount(2);

    CountWorkflowExecutionsResponse expectedResponse = new CountWorkflowExecutionsResponse();
    expectedResponse.setCount(2);

    when(serviceOld.CountWorkflowExecutions(any())).thenReturn(mockResponseOld);
    when(serviceNew.CountWorkflowExecutions(any())).thenReturn(null);
    CountWorkflowExecutionsResponse response = migrationService.CountWorkflowExecutions(request);
    assertEquals(expectedResponse, response);
  }

  // query in the new cluster
  @Test
  public void testQueryWorkflow_queryWorkflowInNew() throws BaseError {

    String domain = "test";
    String wfID = "123";

    QueryWorkflowRequest queryWorkflowRequest =
        new QueryWorkflowRequest()
            .setDomain(domain)
            .setQuery(new WorkflowQuery())
            .setExecution(new WorkflowExecution().setWorkflowId(wfID));

    DescribeWorkflowExecutionResponse describeWorkflowExecutionResponse =
        new DescribeWorkflowExecutionResponse();

    when(serviceNew.DescribeWorkflowExecution(any())).thenReturn(describeWorkflowExecutionResponse);
    when(serviceOld.DescribeWorkflowExecution(any())).thenReturn(null);

    QueryWorkflowResponse mockResponse = new QueryWorkflowResponse();
    when(serviceNew.QueryWorkflow(any())).thenReturn(mockResponse);

    QueryWorkflowResponse response = migrationService.QueryWorkflow(queryWorkflowRequest);

    // Verify interactions
    verify(serviceNew, times(1)).DescribeWorkflowExecution(any());
    verify(serviceOld, times(1)).DescribeWorkflowExecution(any());
    verify(serviceNew, times(1)).QueryWorkflow(any());

    // Verify that no other methods are called
    verifyNoMoreInteractions(serviceNew);
    verifyNoMoreInteractions(serviceOld);

    // Assert the response
    assertEquals(mockResponse, response);
  }

  // query found in the old cluster
  @Test
  public void testQueryWorkflow_queryWorkflowInOld() throws BaseError {

    String domain = "test";
    String wfID = "123";

    QueryWorkflowRequest queryWorkflowRequest =
        new QueryWorkflowRequest()
            .setDomain(domain)
            .setQuery(new WorkflowQuery())
            .setExecution(new WorkflowExecution().setWorkflowId(wfID));

    when(serviceNew.DescribeWorkflowExecution(any())).thenReturn(null);

    DescribeWorkflowExecutionResponse describeWorkflowExecutionResponse =
        new DescribeWorkflowExecutionResponse();
    when(serviceOld.DescribeWorkflowExecution(any())).thenReturn(describeWorkflowExecutionResponse);

    QueryWorkflowResponse mockResponse = new QueryWorkflowResponse();
    when(serviceOld.QueryWorkflow(any())).thenReturn(mockResponse);

    QueryWorkflowResponse response = migrationService.QueryWorkflow(queryWorkflowRequest);

    // Verify interactions
    verify(serviceNew, times(1)).DescribeWorkflowExecution(any());
    verify(serviceOld, times(1)).DescribeWorkflowExecution(any());
    verify(serviceOld, times(1)).QueryWorkflow(any());

    // Verify that no other methods are called
    verifyNoMoreInteractions(serviceNew);
    verifyNoMoreInteractions(serviceOld);

    // Assert the response
    assertEquals(mockResponse, response);
  }

  @Test
  public void testQueryWorkflow_noWorkflowID() throws BaseError {

    String domain = "test";
    QueryWorkflowRequest request = new QueryWorkflowRequest().setDomain(domain);
    QueryWorkflowResponse response = new QueryWorkflowResponse();
    try {
      response = migrationService.QueryWorkflow(request);
      assertNull(response);
    } catch (NullPointerException e) {
      assertNotNull(response);
    }
  }

  @Test
  public void testQueryWorkflow_errorInDescribeWorkflowExecution() throws BaseError {

    String domain = "test";
    String wfID = "123";

    QueryWorkflowRequest queryWorkflowRequest =
        new QueryWorkflowRequest()
            .setDomain(domain)
            .setQuery(new WorkflowQuery())
            .setExecution(new WorkflowExecution().setWorkflowId(wfID));

    when(serviceNew.DescribeWorkflowExecution(any())).thenReturn(null);
    when(serviceOld.DescribeWorkflowExecution(any())).thenReturn(null);
    QueryWorkflowResponse queryWorkflowResponse =
        migrationService.QueryWorkflow(queryWorkflowRequest);
    // Verify interactions
    verify(serviceNew, times(1)).DescribeWorkflowExecution(any());
    verify(serviceOld, times(1)).DescribeWorkflowExecution(any());

    assertNull(queryWorkflowResponse);
  }

  @Test
  public void testListOpenWorkflows_InitialRequest() throws BaseError {

    String domain = "test";

    ListOpenWorkflowExecutionsRequest request =
        new ListOpenWorkflowExecutionsRequest()
            .setDomain(domain)
            .setMaximumPageSize(1)
            .setNextPageToken((byte[]) null)
            .setExecutionFilter(null)
            .setTypeFilter(null)
            .setStartTimeFilter(
                new StartTimeFilter().setEarliestTime(1000).setLatestTime(1000000000));

    ListOpenWorkflowExecutionsResponse mockSingleResultResponse =
        new ListOpenWorkflowExecutionsResponse()
            .setExecutions(new ArrayList<>())
            .setNextPageToken("testToken".getBytes());

    WorkflowExecutionInfo executionInfo = new WorkflowExecutionInfo();
    executionInfo.setExecution(
        new WorkflowExecution().setWorkflowId("testWfId").setRunId("testRunId"));
    mockSingleResultResponse.getExecutions().add(executionInfo);

    // fetch from new cluster for initial request
    when(serviceNew.ListOpenWorkflowExecutions(any())).thenReturn(mockSingleResultResponse);
    ListOpenWorkflowExecutionsResponse response =
        migrationService.ListOpenWorkflowExecutions(request);
    assertEquals(mockSingleResultResponse, response);
  }

  // calling old cluster when new cluster returns empty response
  @Test
  public void testListOpenWorkflow_OldClusterCall() throws BaseError {

    String domain = "test";

    ListOpenWorkflowExecutionsRequest request =
        new ListOpenWorkflowExecutionsRequest()
            .setDomain(domain)
            .setMaximumPageSize(1)
            .setNextPageToken((byte[]) null)
            .setExecutionFilter(null)
            .setTypeFilter(null)
            .setStartTimeFilter(
                new StartTimeFilter().setEarliestTime(1000).setLatestTime(1000000000));

    ListOpenWorkflowExecutionsResponse mockEmptyResponse =
        new ListOpenWorkflowExecutionsResponse()
            .setExecutions(new ArrayList<>())
            .setNextPageToken((byte[]) null);

    ListOpenWorkflowExecutionsResponse mockSingleResultResponse =
        new ListOpenWorkflowExecutionsResponse()
            .setExecutions(new ArrayList<>())
            .setNextPageToken("testToken".getBytes());

    WorkflowExecutionInfo executionInfo = new WorkflowExecutionInfo();
    executionInfo.setExecution(
        new WorkflowExecution().setWorkflowId("testWfId").setRunId("testRunId"));
    mockSingleResultResponse.getExecutions().add(executionInfo);

    when(serviceNew.ListOpenWorkflowExecutions(any())).thenReturn(mockEmptyResponse);
    ListOpenWorkflowExecutionsResponse response =
        migrationService.ListOpenWorkflowExecutions(request);
    assertEquals(mockEmptyResponse, response);

    when(serviceOld.ListOpenWorkflowExecutions(any())).thenReturn(mockSingleResultResponse);
    response = migrationService.ListOpenWorkflowExecutions(request);
    assertEquals(mockSingleResultResponse, response);
  }

  // if fetching from new cluster result size is less than pageSize, fetch additional records from
  // Old Cluster
  @Test
  public void testListOpenWorkflow_fetchFromBothCluster() throws BaseError {

    String domain = "test";

    ListOpenWorkflowExecutionsRequest requestTwoPages =
        new ListOpenWorkflowExecutionsRequest()
            .setDomain(domain)
            .setMaximumPageSize(2)
            .setNextPageToken((byte[]) null)
            .setExecutionFilter(null)
            .setTypeFilter(null)
            .setStartTimeFilter(
                new StartTimeFilter().setEarliestTime(1000).setLatestTime(1000000000));

    ListOpenWorkflowExecutionsResponse mockTwoResultResponse =
        new ListOpenWorkflowExecutionsResponse()
            .setExecutions(new ArrayList<>())
            .setNextPageToken("testToken".getBytes());

    WorkflowExecutionInfo executionInfo = new WorkflowExecutionInfo();
    executionInfo.setExecution(
        new WorkflowExecution().setWorkflowId("testWfId").setRunId("testRunId"));
    mockTwoResultResponse.getExecutions().add(executionInfo);

    when(serviceNew.ListOpenWorkflowExecutions(requestTwoPages)).thenReturn(mockTwoResultResponse);
    ListOpenWorkflowExecutionsResponse response =
        migrationService.ListOpenWorkflowExecutions(requestTwoPages);
    assertEquals(mockTwoResultResponse, response);

    when(serviceOld.ListOpenWorkflowExecutions(requestTwoPages)).thenReturn(mockTwoResultResponse);
    response = migrationService.ListOpenWorkflowExecutions(requestTwoPages);
    mockTwoResultResponse.getExecutions().add(executionInfo);
    assertEquals(mockTwoResultResponse, response);
  }

  @Test
  public void testListOpenWorkflows_emptyRequestTests() throws BaseError {

    // Test when request is null
    try {
      migrationService.ListOpenWorkflowExecutions(null);
    } catch (BadRequestError e) {
      assertEquals("List request is null", e.getMessage());
    }

    // Test when domain is null
    try {
      migrationService.ListOpenWorkflowExecutions(
          new ListOpenWorkflowExecutionsRequest().setMaximumPageSize(10));
    } catch (BadRequestError e) {
      assertEquals("Domain is null or empty", e.getMessage());
    }
  }

  // Test when error returned from internal client, return same error
  @Test
  public void testListOpenWorkflow_error() throws BaseError {
    String domain = "test";

    when(serviceNew.ListOpenWorkflowExecutions(any())).thenReturn(null);
    ListOpenWorkflowExecutionsResponse response =
        migrationService.ListOpenWorkflowExecutions(
            new ListOpenWorkflowExecutionsRequest().setDomain(domain));
    verify(serviceNew, times(1)).ListOpenWorkflowExecutions(any());
    assertNull(response);
  }

  @Test
  public void testListOpenWorkflow_FromClusterOnly() throws BaseError {

    String domain = "test";

    ListOpenWorkflowExecutionsRequest request =
        new ListOpenWorkflowExecutionsRequest()
            .setDomain(domain)
            .setMaximumPageSize(1)
            .setNextPageToken((byte[]) null);

    ListOpenWorkflowExecutionsResponse mockEmptyResponse =
        new ListOpenWorkflowExecutionsResponse()
            .setExecutions(new ArrayList<>())
            .setNextPageToken((byte[]) null);

    when(serviceOld.ListOpenWorkflowExecutions(any())).thenReturn(mockEmptyResponse);
    ListOpenWorkflowExecutionsResponse response =
        migrationService.ListOpenWorkflowExecutions(request);
    assertEquals(mockEmptyResponse, response);
  }

  @Test
  public void testListOpenWorkflows_ResponseWithToken() throws BaseError {

    String domain = "test";

    ListOpenWorkflowExecutionsRequest request =
        new ListOpenWorkflowExecutionsRequest()
            .setDomain(domain)
            .setMaximumPageSize(1)
            .setNextPageToken((byte[]) null);

    ListOpenWorkflowExecutionsResponse expectedResponseWithToken =
        new ListOpenWorkflowExecutionsResponse();
    expectedResponseWithToken.setExecutions(new ArrayList<>());
    WorkflowExecutionInfo executionInfo1 = new WorkflowExecutionInfo();
    executionInfo1.setExecution(
        new WorkflowExecution().setWorkflowId("testWfId").setRunId("testRunId"));
    WorkflowExecutionInfo executionInfo2 = new WorkflowExecutionInfo();
    executionInfo2.setExecution(
        new WorkflowExecution().setWorkflowId("testWfId").setRunId("testRunId"));
    expectedResponseWithToken.getExecutions().add(executionInfo1);
    expectedResponseWithToken.getExecutions().add(executionInfo2);
    expectedResponseWithToken.setNextPageToken("totestToken".getBytes());

    when(serviceNew.ListOpenWorkflowExecutions(any())).thenReturn(expectedResponseWithToken);
    ListOpenWorkflowExecutionsResponse response =
        migrationService.ListOpenWorkflowExecutions(request);
    assertEquals(expectedResponseWithToken, response);

    ListOpenWorkflowExecutionsRequest requestTwoItems =
        new ListOpenWorkflowExecutionsRequest()
            .setDomain(domain)
            .setMaximumPageSize(2)
            .setNextPageToken((byte[]) null);

    when(serviceNew.ListOpenWorkflowExecutions(any())).thenReturn(expectedResponseWithToken);
    response = migrationService.ListOpenWorkflowExecutions(requestTwoItems);
    assertEquals(expectedResponseWithToken, response);
  }

  @Test
  public void testListClosedWorkflows_InitialRequest() throws BaseError {

    String domain = "test";

    ListClosedWorkflowExecutionsRequest request =
        new ListClosedWorkflowExecutionsRequest()
            .setDomain(domain)
            .setMaximumPageSize(1)
            .setNextPageToken((byte[]) null)
            .setExecutionFilter(null)
            .setTypeFilter(null)
            .setStartTimeFilter(
                new StartTimeFilter().setEarliestTime(1000).setLatestTime(1000000000));

    ListClosedWorkflowExecutionsResponse mockSingleResultResponse =
        new ListClosedWorkflowExecutionsResponse()
            .setExecutions(new ArrayList<>())
            .setNextPageToken("testToken".getBytes());

    WorkflowExecutionInfo executionInfo = new WorkflowExecutionInfo();
    executionInfo.setExecution(
        new WorkflowExecution().setWorkflowId("testWfId").setRunId("testRunId"));
    mockSingleResultResponse.getExecutions().add(executionInfo);

    // fetch from new cluster for initial request
    when(serviceNew.ListClosedWorkflowExecutions(any())).thenReturn(mockSingleResultResponse);
    ListClosedWorkflowExecutionsResponse response =
        migrationService.ListClosedWorkflowExecutions(request);
    assertEquals(mockSingleResultResponse, response);
  }

  // calling old cluster when new cluster returns empty response
  @Test
  public void testListClosedWorkflow_OldClusterCall() throws BaseError {

    String domain = "test";

    ListClosedWorkflowExecutionsRequest request =
        new ListClosedWorkflowExecutionsRequest()
            .setDomain(domain)
            .setMaximumPageSize(1)
            .setNextPageToken((byte[]) null)
            .setExecutionFilter(null)
            .setTypeFilter(null)
            .setStartTimeFilter(
                new StartTimeFilter().setEarliestTime(1000).setLatestTime(1000000000));

    ListClosedWorkflowExecutionsResponse mockEmptyResponse =
        new ListClosedWorkflowExecutionsResponse()
            .setExecutions(new ArrayList<>())
            .setNextPageToken((byte[]) null);

    ListClosedWorkflowExecutionsResponse mockSingleResultResponse =
        new ListClosedWorkflowExecutionsResponse()
            .setExecutions(new ArrayList<>())
            .setNextPageToken("testToken".getBytes());

    WorkflowExecutionInfo executionInfo = new WorkflowExecutionInfo();
    executionInfo.setExecution(
        new WorkflowExecution().setWorkflowId("testWfId").setRunId("testRunId"));
    mockSingleResultResponse.getExecutions().add(executionInfo);

    when(serviceNew.ListClosedWorkflowExecutions(any())).thenReturn(mockEmptyResponse);
    ListClosedWorkflowExecutionsResponse response =
        migrationService.ListClosedWorkflowExecutions(request);
    assertEquals(mockEmptyResponse, response);

    when(serviceOld.ListClosedWorkflowExecutions(any())).thenReturn(mockSingleResultResponse);
    response = migrationService.ListClosedWorkflowExecutions(request);
    assertEquals(mockSingleResultResponse, response);
  }

  // if fetching from new cluster result size is less than pageSize, fetch additional records from
  // Old Cluster
  @Test
  public void testListClosedWorkflow_fetchFromBothCluster() throws BaseError {

    String domain = "test";

    ListClosedWorkflowExecutionsRequest requestTwoPages =
        new ListClosedWorkflowExecutionsRequest()
            .setDomain(domain)
            .setMaximumPageSize(2)
            .setNextPageToken((byte[]) null)
            .setExecutionFilter(null)
            .setTypeFilter(null)
            .setStartTimeFilter(
                new StartTimeFilter().setEarliestTime(1000).setLatestTime(1000000000));

    ListClosedWorkflowExecutionsResponse mockTwoResultResponse =
        new ListClosedWorkflowExecutionsResponse()
            .setExecutions(new ArrayList<>())
            .setNextPageToken("testToken".getBytes());

    WorkflowExecutionInfo executionInfo = new WorkflowExecutionInfo();
    executionInfo.setExecution(
        new WorkflowExecution().setWorkflowId("testWfId").setRunId("testRunId"));
    mockTwoResultResponse.getExecutions().add(executionInfo);

    when(serviceNew.ListClosedWorkflowExecutions(requestTwoPages))
        .thenReturn(mockTwoResultResponse);
    ListClosedWorkflowExecutionsResponse response =
        migrationService.ListClosedWorkflowExecutions(requestTwoPages);
    assertEquals(mockTwoResultResponse, response);

    when(serviceOld.ListClosedWorkflowExecutions(requestTwoPages))
        .thenReturn(mockTwoResultResponse);
    response = migrationService.ListClosedWorkflowExecutions(requestTwoPages);
    mockTwoResultResponse.getExecutions().add(executionInfo);
    assertEquals(mockTwoResultResponse, response);
  }

  @Test
  public void testListClosedWorkflows_emptyRequestTests() throws BaseError {

    // Test when request is null
    try {
      migrationService.ListClosedWorkflowExecutions(null);
    } catch (BadRequestError e) {
      assertEquals("List request is null", e.getMessage());
    }

    // Test when domain is null
    try {
      migrationService.ListClosedWorkflowExecutions(
          new ListClosedWorkflowExecutionsRequest().setMaximumPageSize(10));
    } catch (BadRequestError e) {
      assertEquals("Domain is null or empty", e.getMessage());
    }
  }

  // Test when error returned from internal client, return same error
  @Test
  public void testListClosedWorkflow_error() throws BaseError {
    String domain = "test";

    when(serviceNew.ListClosedWorkflowExecutions(any())).thenReturn(null);
    ListClosedWorkflowExecutionsResponse response =
        migrationService.ListClosedWorkflowExecutions(
            new ListClosedWorkflowExecutionsRequest().setDomain(domain));
    verify(serviceNew, times(1)).ListClosedWorkflowExecutions(any());
    assertNull(response);
  }

  @Test
  public void testListClosedWorkflow_FromClusterOnly() throws BaseError {

    String domain = "test";

    ListClosedWorkflowExecutionsRequest request =
        new ListClosedWorkflowExecutionsRequest()
            .setDomain(domain)
            .setMaximumPageSize(1)
            .setNextPageToken((byte[]) null);

    ListClosedWorkflowExecutionsResponse mockEmptyResponse =
        new ListClosedWorkflowExecutionsResponse()
            .setExecutions(new ArrayList<>())
            .setNextPageToken((byte[]) null);

    when(serviceOld.ListClosedWorkflowExecutions(any())).thenReturn(mockEmptyResponse);
    ListClosedWorkflowExecutionsResponse response =
        migrationService.ListClosedWorkflowExecutions(request);
    assertEquals(mockEmptyResponse, response);
  }

  @Test
  public void testListClosedWorkflows_ResponseWithToken() throws BaseError {

    String domain = "test";

    ListClosedWorkflowExecutionsRequest request =
        new ListClosedWorkflowExecutionsRequest()
            .setDomain(domain)
            .setMaximumPageSize(1)
            .setNextPageToken((byte[]) null);

    ListClosedWorkflowExecutionsResponse expectedResponseWithToken =
        new ListClosedWorkflowExecutionsResponse();
    expectedResponseWithToken.setExecutions(new ArrayList<>());
    WorkflowExecutionInfo executionInfo1 = new WorkflowExecutionInfo();
    executionInfo1.setExecution(
        new WorkflowExecution().setWorkflowId("testWfId").setRunId("testRunId"));
    WorkflowExecutionInfo executionInfo2 = new WorkflowExecutionInfo();
    executionInfo2.setExecution(
        new WorkflowExecution().setWorkflowId("testWfId").setRunId("testRunId"));
    expectedResponseWithToken.getExecutions().add(executionInfo1);
    expectedResponseWithToken.getExecutions().add(executionInfo2);
    expectedResponseWithToken.setNextPageToken("totestToken".getBytes());

    when(serviceNew.ListClosedWorkflowExecutions(any())).thenReturn(expectedResponseWithToken);
    ListClosedWorkflowExecutionsResponse response =
        migrationService.ListClosedWorkflowExecutions(request);
    assertEquals(expectedResponseWithToken, response);

    ListClosedWorkflowExecutionsRequest requestTwoItems =
        new ListClosedWorkflowExecutionsRequest()
            .setDomain(domain)
            .setMaximumPageSize(2)
            .setNextPageToken((byte[]) null);

    when(serviceOld.ListClosedWorkflowExecutions(any())).thenReturn(expectedResponseWithToken);
    response = migrationService.ListClosedWorkflowExecutions(requestTwoItems);
    assertEquals(expectedResponseWithToken, response);
  }

  @Test
  public void testSignalWithStartWorkflowExecution_NewService() throws BaseError {
    SignalWithStartWorkflowExecutionRequest request =
        new SignalWithStartWorkflowExecutionRequest()
            .setWorkflowId("newSignalWorkflow")
            .setSignalName("TestSignal")
            .setDomain("domainNew");

    when(serviceNew.SignalWithStartWorkflowExecution(request))
        .thenReturn(new StartWorkflowExecutionResponse().setRunId("newRunId"));

    StartWorkflowExecutionResponse response =
        migrationService.SignalWithStartWorkflowExecution(request);

    assertEquals("newRunId", response.getRunId());
    verify(serviceNew, times(1)).SignalWithStartWorkflowExecution(request);
    verify(serviceOld, never()).SignalWithStartWorkflowExecution(request);
  }

  @Test
  public void testSignalWithStartWorkflowExecution_OldService() throws BaseError {
    SignalWithStartWorkflowExecutionRequest signal =
        new SignalWithStartWorkflowExecutionRequest()
            .setWorkflowId("testSignal")
            .setDomain("oldDomain")
            .setSignalName("sampleSignal");

    when(serviceNew.DescribeWorkflowExecution(any())).thenReturn(null);

    DescribeWorkflowExecutionResponse describeWorkflowExecutionResponse =
        new DescribeWorkflowExecutionResponse();
    when(serviceOld.DescribeWorkflowExecution(any())).thenReturn(describeWorkflowExecutionResponse);

    when(serviceOld.SignalWithStartWorkflowExecution(signal))
        .thenReturn(new StartWorkflowExecutionResponse().setRunId("oldRunID"));
    StartWorkflowExecutionResponse responseOld =
        migrationService.SignalWithStartWorkflowExecution(signal);

    verify(serviceNew, times(1)).DescribeWorkflowExecution(any());
    verify(serviceOld, times(1)).DescribeWorkflowExecution(any());
    verify(serviceOld, times(1)).SignalWithStartWorkflowExecution(any());

    verifyNoMoreInteractions(serviceNew);
    verifyNoMoreInteractions(serviceOld);

    assertEquals(responseOld.getRunId(), "oldRunID");
  }

  @Test
  public void testSignalWorkflowExecution_SignalNewService() throws BaseError {
    SignalWorkflowExecutionRequest request =
        new SignalWorkflowExecutionRequest()
            .setWorkflowExecution(new WorkflowExecution().setWorkflowId("newSignal"))
            .setSignalName("signalNew")
            .setDomain("domainNew");

    doNothing().when(serviceNew).SignalWorkflowExecution(request);

    migrationService.SignalWorkflowExecution(request);

    verify(serviceNew, times(1)).SignalWorkflowExecution(request);
    verify(serviceOld, never()).SignalWorkflowExecution(any());
  }

  @Test
  public void testSignalWorkflowExecution_SignalInOldService() throws BaseError {
    SignalWorkflowExecutionRequest request =
        new SignalWorkflowExecutionRequest()
            .setWorkflowExecution(new WorkflowExecution().setWorkflowId("oldSignal"))
            .setSignalName("signalOld")
            .setDomain("domainOld");

    when(serviceNew.DescribeWorkflowExecution(any())).thenReturn(null);

    DescribeWorkflowExecutionResponse describeWorkflowExecutionResponse =
        new DescribeWorkflowExecutionResponse();
    when(serviceOld.DescribeWorkflowExecution(any())).thenReturn(describeWorkflowExecutionResponse);

    doNothing().when(serviceOld).SignalWorkflowExecution(request);
    migrationService.SignalWorkflowExecution(request);

    verify(serviceNew, times(1)).DescribeWorkflowExecution(any());
    verify(serviceOld, times(1)).DescribeWorkflowExecution(any());
    verify(serviceOld, times(1)).SignalWorkflowExecution(any());

    verifyNoMoreInteractions(serviceNew);
    verifyNoMoreInteractions(serviceOld);
  }

  @Test
  public void testGetOptions() {
    when(serviceOld.getOptions())
        .thenReturn(ClientOptions.newBuilder().setServiceName("serviceName").build());
    ClientOptions options = migrationService.getOptions();
    assertEquals("serviceName", options.getServiceName());
  }
}
