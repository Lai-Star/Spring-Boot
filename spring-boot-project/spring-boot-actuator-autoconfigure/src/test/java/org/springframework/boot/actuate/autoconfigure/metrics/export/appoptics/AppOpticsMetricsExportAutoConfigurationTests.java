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

package org.springframework.boot.actuate.autoconfigure.metrics.export.appoptics;

import io.micrometer.appoptics.AppOpticsConfig;
import io.micrometer.appoptics.AppOpticsMeterRegistry;
import io.micrometer.core.instrument.Clock;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AppOpticsMetricsExportAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
class AppOpticsMetricsExportAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(AppOpticsMetricsExportAutoConfiguration.class));

	@Test
	void backsOffWithoutAClock() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(AppOpticsMeterRegistry.class));
	}

	@Test
	void autoConfiguresItsConfigAndMeterRegistry() {
		this.contextRunner.withPropertyValues("management.appoptics.metrics.export.api-token=abcde")
			.withUserConfiguration(BaseConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(AppOpticsMeterRegistry.class)
				.hasSingleBean(AppOpticsConfig.class));
	}

	@Test
	void autoConfigurationCanBeDisabledWithDefaultsEnabledProperty() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.withPropertyValues("management.defaults.metrics.export.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(AppOpticsMeterRegistry.class)
				.doesNotHaveBean(AppOpticsConfig.class));
	}

	@Test
	void autoConfigurationCanBeDisabledWithSpecificEnabledProperty() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.withPropertyValues("management.appoptics.metrics.export.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(AppOpticsMeterRegistry.class)
				.doesNotHaveBean(AppOpticsConfig.class));
	}

	@Test
	void allowsCustomConfigToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomConfigConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(AppOpticsMeterRegistry.class)
				.hasSingleBean(AppOpticsConfig.class)
				.hasBean("customConfig"));
	}

	@Test
	void allowsCustomRegistryToBeUsed() {
		this.contextRunner.withPropertyValues("management.appoptics.metrics.export.api-token=abcde")
			.withUserConfiguration(CustomRegistryConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(AppOpticsMeterRegistry.class)
				.hasBean("customRegistry")
				.hasSingleBean(AppOpticsConfig.class));
	}

	@Test
	void stopsMeterRegistryWhenContextIsClosed() {
		this.contextRunner.withPropertyValues("management.appoptics.metrics.export.api-token=abcde")
			.withUserConfiguration(BaseConfiguration.class)
			.run((context) -> {
				AppOpticsMeterRegistry registry = context.getBean(AppOpticsMeterRegistry.class);
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
		AppOpticsConfig customConfig() {
			return (key) -> "appoptics.apiToken".equals(key) ? "abcde" : null;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class CustomRegistryConfiguration {

		@Bean
		AppOpticsMeterRegistry customRegistry(AppOpticsConfig config, Clock clock) {
			return new AppOpticsMeterRegistry(config, clock);
		}

	}

}
