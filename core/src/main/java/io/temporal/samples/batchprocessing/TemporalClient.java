/*
 *  Copyright (c) 2020 Temporal Technologies, Inc. All Rights Reserved
 *
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

package io.temporal.samples.batchprocessing;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.samples.batchprocessing.metrics.MetricsUtils;
import io.temporal.samples.batchprocessing.web.ServerInfo;
import io.temporal.serviceclient.SimpleSslContextBuilder;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import javax.net.ssl.SSLException;
import com.sun.net.httpserver.HttpServer;
import com.uber.m3.tally.RootScopeBuilder;
import com.uber.m3.tally.Scope;
import com.uber.m3.util.ImmutableMap;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.reporter.MicrometerClientStatsReporter;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;

public class TemporalClient {
  public static WorkflowServiceStubs getWorkflowServiceStubs(int metricsPort)
      throws FileNotFoundException, SSLException {
    WorkflowServiceStubsOptions.Builder workflowServiceStubsOptionsBuilder =
        WorkflowServiceStubsOptions.newBuilder();

    if (!ServerInfo.getCertPath().equals("") && !"".equals(ServerInfo.getKeyPath())) {
      InputStream clientCert = new FileInputStream(ServerInfo.getCertPath());

      InputStream clientKey = new FileInputStream(ServerInfo.getKeyPath());

      workflowServiceStubsOptionsBuilder.setSslContext(
          SimpleSslContextBuilder.forPKCS8(clientCert, clientKey).build());
    }

    // For temporal cloud this would likely be ${namespace}.tmprl.cloud:7233
    String targetEndpoint = ServerInfo.getAddress();
    // Your registered namespace.

    PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    // Set up a new scope, report every 1 second
    Scope scope =
            new RootScopeBuilder()
                    .reporter(new MicrometerClientStatsReporter(registry))
                    .reportEvery(com.uber.m3.util.Duration.ofSeconds(1));
    // Start the prometheus scrape endpoint for starter metrics
    HttpServer scrapeEndpoint = MetricsUtils.startPrometheusScrapeEndpoint(registry, metricsPort);
    // Stopping the starter will stop the http server that exposes the
    // scrape endpoint.
    Runtime.getRuntime().addShutdownHook(new Thread(() -> scrapeEndpoint.stop(1)));

    workflowServiceStubsOptionsBuilder.setTarget(targetEndpoint).setMetricsScope(scope).build();;
    WorkflowServiceStubs service = null;

    if (!ServerInfo.getAddress().equals("localhost:7233")) {
      // if not local server, then use the workflowServiceStubsOptionsBuilder
      service = WorkflowServiceStubs.newServiceStubs(workflowServiceStubsOptionsBuilder.build());
    } else {
      service = WorkflowServiceStubs.newLocalServiceStubs();
    }

    return service;
  }

  public static WorkflowClient get(int metricsPort) throws FileNotFoundException, SSLException {
    // TODO support local server
    // Get worker to poll the common task queue.
    // gRPC stubs wrapper that talks to the local docker instance of temporal service.
    // WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

    WorkflowServiceStubs service = getWorkflowServiceStubs(metricsPort);

    WorkflowClientOptions.Builder builder = WorkflowClientOptions.newBuilder();

    System.out.println("<<<<SERVER INFO>>>>:\n " + ServerInfo.getServerInfo());
    WorkflowClientOptions clientOptions = builder.setNamespace(ServerInfo.getNamespace()).build();

    // client that can be used to start and signal workflows
    WorkflowClient client = WorkflowClient.newInstance(service, clientOptions);

    System.out.println("Metrics are available at http://localhost:" + metricsPort + "/metrics");

    return client;
  }
}
