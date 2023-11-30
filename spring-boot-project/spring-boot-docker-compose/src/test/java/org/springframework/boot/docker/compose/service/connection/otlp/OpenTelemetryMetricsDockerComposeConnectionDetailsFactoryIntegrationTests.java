/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.docker.compose.service.connection.otlp;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.export.otlp.OtlpMetricsConnectionDetails;
import org.springframework.boot.docker.compose.service.connection.test.AbstractDockerComposeIntegrationTests;
import org.springframework.boot.testsupport.testcontainers.DockerImageNames;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for
 * {@link OpenTelemetryMetricsDockerComposeConnectionDetailsFactory}.
 *
 * @author Eddú Meléndez
 */
class OpenTelemetryMetricsDockerComposeConnectionDetailsFactoryIntegrationTests
		extends AbstractDockerComposeIntegrationTests {

	OpenTelemetryMetricsDockerComposeConnectionDetailsFactoryIntegrationTests() {
		super("otlp-compose.yaml", DockerImageNames.opentelemetry());
	}

	@Test
	void runCreatesConnectionDetails() {
		OtlpMetricsConnectionDetails connectionDetails = run(OtlpMetricsConnectionDetails.class);
		assertThat(connectionDetails.getUrl()).startsWith("http://").endsWith("/v1/metrics");
	}

}
