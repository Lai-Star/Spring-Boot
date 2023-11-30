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

package org.springframework.boot.actuate.metrics.web.client;

import io.micrometer.observation.ObservationRegistry;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.http.client.observation.ClientRequestObservationConvention;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient.Builder;

/**
 * {@link RestClientCustomizer} that configures the {@link Builder RestClient builder} to
 * record request observations.
 *
 * @author Moritz Halbritter
 * @since 3.2.0
 */
public class ObservationRestClientCustomizer implements RestClientCustomizer {

	private final ObservationRegistry observationRegistry;

	private final ClientRequestObservationConvention observationConvention;

	/**
	 * Create a new {@link ObservationRestClientCustomizer}.
	 * @param observationRegistry the observation registry
	 * @param observationConvention the observation convention
	 */
	public ObservationRestClientCustomizer(ObservationRegistry observationRegistry,
			ClientRequestObservationConvention observationConvention) {
		Assert.notNull(observationConvention, "ObservationConvention must not be null");
		Assert.notNull(observationRegistry, "ObservationRegistry must not be null");
		this.observationRegistry = observationRegistry;
		this.observationConvention = observationConvention;
	}

	@Override
	public void customize(Builder restClientBuilder) {
		restClientBuilder.observationRegistry(this.observationRegistry);
		restClientBuilder.observationConvention(this.observationConvention);
	}

}
