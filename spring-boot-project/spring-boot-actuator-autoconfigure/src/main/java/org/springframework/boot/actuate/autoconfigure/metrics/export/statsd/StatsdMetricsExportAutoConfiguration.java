/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.statsd;

import io.micrometer.core.instrument.Clock;
import io.micrometer.statsd.StatsdConfig;
import io.micrometer.statsd.StatsdMeterRegistry;

import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.ConditionalOnEnabledMetricsExport;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for exporting metrics to StatsD.
 *
 * @author Jon Schneider
 * @since 2.0.0
 */
@AutoConfiguration(
		before = { CompositeMeterRegistryAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class },
		after = MetricsAutoConfiguration.class)
@ConditionalOnBean(Clock.class)
@ConditionalOnClass(StatsdMeterRegistry.class)
@ConditionalOnEnabledMetricsExport("statsd")
@EnableConfigurationProperties(StatsdProperties.class)
public class StatsdMetricsExportAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public StatsdConfig statsdConfig(StatsdProperties statsdProperties) {
		return new StatsdPropertiesConfigAdapter(statsdProperties);
	}

	@Bean
	@ConditionalOnMissingBean
	public StatsdMeterRegistry statsdMeterRegistry(StatsdConfig statsdConfig, Clock clock) {
		return new StatsdMeterRegistry(statsdConfig, clock);
	}

}
