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

import io.micrometer.core.instrument.binder.logging.Log4j2Metrics;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.logging.ConfigureClasspathToPreferLog4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Log4J2MetricsAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
@ConfigureClasspathToPreferLog4j2
class Log4J2MetricsWithLog4jLoggerContextAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().with(MetricsRun.simple())
		.withConfiguration(AutoConfigurations.of(Log4J2MetricsAutoConfiguration.class));

	@Test
	void autoConfiguresLog4J2Metrics() {
		assertThat(LogManager.getContext().getClass().getName())
			.isEqualTo("org.apache.logging.log4j.core.LoggerContext");
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(Log4j2Metrics.class));
	}

	@Test
	void allowsCustomLog4J2MetricsToBeUsed() {
		assertThat(LogManager.getContext().getClass().getName())
			.isEqualTo("org.apache.logging.log4j.core.LoggerContext");
		this.contextRunner.withUserConfiguration(CustomLog4J2MetricsConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(Log4j2Metrics.class).hasBean("customLog4J2Metrics"));
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomLog4J2MetricsConfiguration {

		@Bean
		Log4j2Metrics customLog4J2Metrics() {
			return new Log4j2Metrics();
		}

	}

}
