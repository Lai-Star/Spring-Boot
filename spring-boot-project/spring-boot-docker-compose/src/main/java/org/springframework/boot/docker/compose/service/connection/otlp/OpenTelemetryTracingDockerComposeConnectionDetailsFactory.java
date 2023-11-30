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

import org.springframework.boot.actuate.autoconfigure.tracing.otlp.OtlpTracingConnectionDetails;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create
 * {@link OtlpTracingConnectionDetails} for an OTLP service.
 *
 * @author Eddú Meléndez
 */
class OpenTelemetryTracingDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<OtlpTracingConnectionDetails> {

	private static final int OTLP_PORT = 4318;

	OpenTelemetryTracingDockerComposeConnectionDetailsFactory() {
		super("otel/opentelemetry-collector-contrib",
				"org.springframework.boot.actuate.autoconfigure.tracing.otlp.OtlpAutoConfiguration");
	}

	@Override
	protected OtlpTracingConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new OpenTelemetryTracingDockerComposeConnectionDetails(source.getRunningService());
	}

	private static final class OpenTelemetryTracingDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements OtlpTracingConnectionDetails {

		private final String host;

		private final int port;

		private OpenTelemetryTracingDockerComposeConnectionDetails(RunningService source) {
			super(source);
			this.host = source.host();
			this.port = source.ports().get(OTLP_PORT);
		}

		@Override
		public String getUrl() {
			return "http://%s:%d/v1/traces".formatted(this.host, this.port);
		}

	}

}
