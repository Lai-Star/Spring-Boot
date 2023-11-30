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

package org.springframework.boot.actuate.autoconfigure.system;

import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration;
import org.springframework.boot.actuate.system.DiskSpaceHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link DiskSpaceHealthIndicator}.
 *
 * @author Mattias Severson
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@AutoConfiguration(before = HealthContributorAutoConfiguration.class)
@ConditionalOnEnabledHealthIndicator("diskspace")
@EnableConfigurationProperties(DiskSpaceHealthIndicatorProperties.class)
public class DiskSpaceHealthContributorAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(name = "diskSpaceHealthIndicator")
	public DiskSpaceHealthIndicator diskSpaceHealthIndicator(DiskSpaceHealthIndicatorProperties properties) {
		return new DiskSpaceHealthIndicator(properties.getPath(), properties.getThreshold());
	}

}
