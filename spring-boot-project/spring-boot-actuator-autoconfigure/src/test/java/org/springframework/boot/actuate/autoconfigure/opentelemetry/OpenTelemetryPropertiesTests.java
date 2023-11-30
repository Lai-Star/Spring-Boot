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

package org.springframework.boot.actuate.autoconfigure.opentelemetry;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link OpenTelemetryProperties}.
 *
 * @author Moritz Halbritter
 */
class OpenTelemetryPropertiesTests {

	private final ApplicationContextRunner runner = new ApplicationContextRunner().withPropertyValues(
			"management.opentelemetry.resource-attributes.a=alpha",
			"management.opentelemetry.resource-attributes.b=beta");

	@Test
	@ClassPathExclusions("opentelemetry-sdk-*")
	void shouldNotDependOnOpenTelemetrySdk() {
		this.runner.withUserConfiguration(TestConfiguration.class).run((context) -> {
			OpenTelemetryProperties properties = context.getBean(OpenTelemetryProperties.class);
			assertThat(properties.getResourceAttributes()).containsOnly(entry("a", "alpha"), entry("b", "beta"));
		});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(OpenTelemetryProperties.class)
	private static class TestConfiguration {

	}

}
