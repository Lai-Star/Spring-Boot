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

package org.springframework.boot.actuate.autoconfigure.metrics.export.datadog;

import io.micrometer.core.instrument.Clock;
import io.micrometer.datadog.DatadogConfig;
import io.micrometer.datadog.DatadogMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DatadogMetricsExportAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
class DatadogMetricsExportAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(DatadogMetricsExportAutoConfiguration.class));

	@Test
	void backsOffWithoutAClock() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(DatadogMeterRegistry.class));
	}

	@Test
	void failsWithoutAnApiKey() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.run((context) -> assertThat(context).hasFailed());
	}

	@Test
	void autoConfiguresConfigAndMeterRegistry() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.withPropertyValues("management.datadog.metrics.export.api-key=abcde")
			.run((context) -> assertThat(context).hasSingleBean(DatadogMeterRegistry.class)
				.hasSingleBean(DatadogConfig.class));
	}

	@Test
	void autoConfigurationCanBeDisabledWithDefaultsEnabledProperty() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.withPropertyValues("management.defaults.metrics.export.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(DatadogMeterRegistry.class)
				.doesNotHaveBean(DatadogConfig.class));
	}

	@Test
	void autoConfigurationCanBeDisabledWithSpecificEnabledProperty() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.withPropertyValues("management.datadog.metrics.export.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(DatadogMeterRegistry.class)
				.doesNotHaveBean(DatadogConfig.class));
	}

	@Test
	void allowsCustomConfigToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomConfigConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(DatadogMeterRegistry.class)
				.hasSingleBean(DatadogConfig.class)
				.hasBean("customConfig"));
	}

	@Test
	void allowsCustomRegistryToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomRegistryConfiguration.class)
			.withPropertyValues("management.datadog.metrics.export.api-key=abcde")
			.run((context) -> assertThat(context).hasSingleBean(DatadogMeterRegistry.class)
				.hasBean("customRegistry")
				.hasSingleBean(DatadogConfig.class));
	}

	@Test
	void stopsMeterRegistryWhenContextIsClosed() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.withPropertyValues("management.datadog.metrics.export.api-key=abcde")
			.run((context) -> {
				DatadogMeterRegistry registry = context.getBean(DatadogMeterRegistry.class);
				assertThat(registry.isClosed()).isFalse();
				context.close();
				assertThat(registry.isClosed()).isTrue();
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class BaseConfiguration {

		@Bean
		Clock clock() {
			return Clock.SYSTEM;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class CustomConfigConfiguration {

		@Bean
		DatadogConfig customConfig() {
			return (key) -> {
				if ("datadog.apiKey".equals(key)) {
					return "12345";
				}
				return null;
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class CustomRegistryConfiguration {

		@Bean
		DatadogMeterRegistry customRegistry(DatadogConfig config, Clock clock) {
			return new DatadogMeterRegistry(config, clock);
		}

	}

}
