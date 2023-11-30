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

package org.springframework.boot.actuate.autoconfigure.metrics.export.signalfx;

import io.micrometer.signalfx.SignalFxConfig;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryPropertiesTests;
import org.springframework.boot.actuate.autoconfigure.metrics.export.signalfx.SignalFxProperties.HistogramType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SignalFxProperties}.
 *
 * @author Stephane Nicoll
 */
class SignalFxPropertiesTests extends StepRegistryPropertiesTests {

	@Test
	void defaultValuesAreConsistent() {
		SignalFxProperties properties = new SignalFxProperties();
		SignalFxConfig config = (key) -> null;
		assertStepRegistryDefaultValues(properties, config);
		// access token is mandatory
		assertThat(properties.getUri()).isEqualTo(config.uri());
		// source has no static default value
		// Not publishing cumulative or delta histograms implies that the default
		// histogram type should be published.
		assertThat(config.publishCumulativeHistogram()).isFalse();
		assertThat(config.publishDeltaHistogram()).isFalse();
		assertThat(properties.getPublishedHistogramType()).isEqualTo(HistogramType.DEFAULT);
	}

}
