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

package org.springframework.boot.actuate.autoconfigure.metrics.export.dynatrace;

import io.micrometer.dynatrace.DynatraceConfig;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryPropertiesTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DynatraceProperties}.
 *
 * @author Andy Wilkinson
 */
class DynatracePropertiesTests extends StepRegistryPropertiesTests {

	@Test
	void defaultValuesAreConsistent() {
		DynatraceProperties properties = new DynatraceProperties();
		DynatraceConfig config = (key) -> null;
		assertStepRegistryDefaultValues(properties, config);
		assertThat(properties.getV1().getTechnologyType()).isEqualTo(config.technologyType());
		assertThat(properties.getV2().isUseDynatraceSummaryInstruments())
			.isEqualTo(config.useDynatraceSummaryInstruments());
		assertThat(properties.getV2().isExportMeterMetadata()).isEqualTo(config.exportMeterMetadata());
	}

}
