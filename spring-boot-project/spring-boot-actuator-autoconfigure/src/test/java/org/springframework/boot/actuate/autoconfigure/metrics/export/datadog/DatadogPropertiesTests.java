/*
 * Copyright 2012-2019 the original author or authors.
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

import io.micrometer.datadog.DatadogConfig;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryPropertiesTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DatadogProperties}.
 *
 * @author Stephane Nicoll
 */
class DatadogPropertiesTests extends StepRegistryPropertiesTests {

	@Test
	void defaultValuesAreConsistent() {
		DatadogProperties properties = new DatadogProperties();
		DatadogConfig config = (key) -> null;
		assertStepRegistryDefaultValues(properties, config);
		assertThat(properties.isDescriptions()).isEqualTo(config.descriptions());
		assertThat(properties.getHostTag()).isEqualTo(config.hostTag());
		assertThat(properties.getUri()).isEqualTo(config.uri());
	}

}
