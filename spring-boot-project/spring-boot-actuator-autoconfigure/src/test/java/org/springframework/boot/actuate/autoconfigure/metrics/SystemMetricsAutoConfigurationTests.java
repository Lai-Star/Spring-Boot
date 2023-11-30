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

package org.springframework.boot.actuate.autoconfigure.metrics;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.actuate.metrics.system.DiskSpaceMetricsBinder;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SystemMetricsAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Chris Bono
 */
class SystemMetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().with(MetricsRun.simple())
		.withConfiguration(AutoConfigurations.of(SystemMetricsAutoConfiguration.class));

	@Test
	void autoConfiguresUptimeMetrics() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(UptimeMetrics.class));
	}

	@Test
	void allowsCustomUptimeMetricsToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomUptimeMetricsConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(UptimeMetrics.class).hasBean("customUptimeMetrics"));
	}

	@Test
	void autoConfiguresProcessorMetrics() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(ProcessorMetrics.class));
	}

	@Test
	void allowsCustomProcessorMetricsToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomProcessorMetricsConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(ProcessorMetrics.class)
				.hasBean("customProcessorMetrics"));
	}

	@Test
	void autoConfiguresFileDescriptorMetrics() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(FileDescriptorMetrics.class));
	}

	@Test
	void allowsCustomFileDescriptorMetricsToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomFileDescriptorMetricsConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(FileDescriptorMetrics.class)
				.hasBean("customFileDescriptorMetrics"));
	}

	@Test
	void autoConfiguresDiskSpaceMetrics() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(DiskSpaceMetricsBinder.class));
	}

	@Test
	void allowsCustomDiskSpaceMetricsToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomDiskSpaceMetricsConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(DiskSpaceMetricsBinder.class)
				.hasBean("customDiskSpaceMetrics"));
	}

	@Test
	void diskSpaceMetricsUsesDefaultPath() {
		this.contextRunner.run((context) -> assertThat(context).hasBean("diskSpaceMetrics")
			.getBean(DiskSpaceMetricsBinder.class)
			.hasFieldOrPropertyWithValue("paths", Collections.singletonList(new File("."))));
	}

	@Test
	void allowsDiskSpaceMetricsPathToBeConfiguredWithSinglePath() {
		this.contextRunner.withPropertyValues("management.metrics.system.diskspace.paths:..")
			.run((context) -> assertThat(context).hasBean("diskSpaceMetrics")
				.getBean(DiskSpaceMetricsBinder.class)
				.hasFieldOrPropertyWithValue("paths", Collections.singletonList(new File(".."))));
	}

	@Test
	void allowsDiskSpaceMetricsPathToBeConfiguredWithMultiplePaths() {
		this.contextRunner.withPropertyValues("management.metrics.system.diskspace.paths:.,..")
			.run((context) -> assertThat(context).hasBean("diskSpaceMetrics")
				.getBean(DiskSpaceMetricsBinder.class)
				.hasFieldOrPropertyWithValue("paths", Arrays.asList(new File("."), new File(".."))));
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomUptimeMetricsConfiguration {

		@Bean
		UptimeMetrics customUptimeMetrics() {
			return new UptimeMetrics();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomProcessorMetricsConfiguration {

		@Bean
		ProcessorMetrics customProcessorMetrics() {
			return new ProcessorMetrics();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomFileDescriptorMetricsConfiguration {

		@Bean
		FileDescriptorMetrics customFileDescriptorMetrics() {
			return new FileDescriptorMetrics();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomDiskSpaceMetricsConfiguration {

		@Bean
		DiskSpaceMetricsBinder customDiskSpaceMetrics() {
			return new DiskSpaceMetricsBinder(Collections.singletonList(new File(System.getProperty("user.dir"))),
					Tags.empty());
		}

	}

}
