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

package org.springframework.boot.actuate.metrics;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.Statistic;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.InvalidEndpointRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link MetricsEndpoint}.
 *
 * @author Andy Wilkinson
 * @author Jon Schneider
 */
class MetricsEndpointTests {

	private final MeterRegistry registry = new SimpleMeterRegistry(SimpleConfig.DEFAULT, new MockClock());

	private final MetricsEndpoint endpoint = new MetricsEndpoint(this.registry);

	@Test
	void listNamesHandlesEmptyListOfMeters() {
		MetricsEndpoint.MetricNamesDescriptor result = this.endpoint.listNames();
		assertThat(result.getNames()).isEmpty();
	}

	@Test
	void listNamesProducesListOfUniqueMeterNames() {
		this.registry.counter("com.example.alpha");
		this.registry.counter("com.example.charlie");
		this.registry.counter("com.example.bravo");
		this.registry.counter("com.example.delta");
		this.registry.counter("com.example.delta");
		this.registry.counter("com.example.echo");
		this.registry.counter("com.example.bravo");
		MetricsEndpoint.MetricNamesDescriptor result = this.endpoint.listNames();
		assertThat(result.getNames()).containsExactly("com.example.alpha", "com.example.bravo", "com.example.charlie",
				"com.example.delta", "com.example.echo");
	}

	@Test
	void listNamesResponseOverCompositeRegistries() {
		CompositeMeterRegistry composite = new CompositeMeterRegistry();
		SimpleMeterRegistry reg1 = new SimpleMeterRegistry();
		SimpleMeterRegistry reg2 = new SimpleMeterRegistry();
		composite.add(reg1);
		composite.add(reg2);
		reg1.counter("counter1").increment();
		reg2.counter("counter2").increment();
		MetricsEndpoint endpoint = new MetricsEndpoint(composite);
		assertThat(endpoint.listNames().getNames()).containsExactly("counter1", "counter2");
	}

	@Test
	void metricValuesAreTheSumOfAllTimeSeriesMatchingTags() {
		this.registry.counter("cache", "result", "hit", "host", "1").increment(2);
		this.registry.counter("cache", "result", "miss", "host", "1").increment(2);
		this.registry.counter("cache", "result", "hit", "host", "2").increment(2);
		MetricsEndpoint.MetricDescriptor response = this.endpoint.metric("cache", Collections.emptyList());
		assertThat(response.getName()).isEqualTo("cache");
		assertThat(availableTagKeys(response)).containsExactly("result", "host");
		assertThat(getCount(response)).hasValue(6.0);
		response = this.endpoint.metric("cache", Collections.singletonList("result:hit"));
		assertThat(availableTagKeys(response)).containsExactly("host");
		assertThat(getCount(response)).hasValue(4.0);
	}

	@Test
	void findFirstMatchingMetersFromNestedRegistries() {
		CompositeMeterRegistry composite = new CompositeMeterRegistry();
		SimpleMeterRegistry firstLevel0 = new SimpleMeterRegistry();
		CompositeMeterRegistry firstLevel1 = new CompositeMeterRegistry();
		SimpleMeterRegistry secondLevel = new SimpleMeterRegistry();
		composite.add(firstLevel0);
		composite.add(firstLevel1);
		firstLevel1.add(secondLevel);
		secondLevel.counter("cache", "result", "hit", "host", "1").increment(2);
		secondLevel.counter("cache", "result", "miss", "host", "1").increment(2);
		secondLevel.counter("cache", "result", "hit", "host", "2").increment(2);
		MetricsEndpoint endpoint = new MetricsEndpoint(composite);
		MetricsEndpoint.MetricDescriptor response = endpoint.metric("cache", Collections.emptyList());
		assertThat(response.getName()).isEqualTo("cache");
		assertThat(availableTagKeys(response)).containsExactly("result", "host");
		assertThat(getCount(response)).hasValue(6.0);
		response = endpoint.metric("cache", Collections.singletonList("result:hit"));
		assertThat(availableTagKeys(response)).containsExactly("host");
		assertThat(getCount(response)).hasValue(4.0);
	}

	@Test
	void matchingMeterNotFoundInNestedRegistries() {
		CompositeMeterRegistry composite = new CompositeMeterRegistry();
		CompositeMeterRegistry firstLevel = new CompositeMeterRegistry();
		SimpleMeterRegistry secondLevel = new SimpleMeterRegistry();
		composite.add(firstLevel);
		firstLevel.add(secondLevel);
		MetricsEndpoint endpoint = new MetricsEndpoint(composite);
		MetricsEndpoint.MetricDescriptor response = endpoint.metric("invalid.metric.name", Collections.emptyList());
		assertThat(response).isNull();
	}

	@Test
	void metricTagValuesAreDeduplicated() {
		this.registry.counter("cache", "host", "1", "region", "east", "result", "hit");
		this.registry.counter("cache", "host", "1", "region", "east", "result", "miss");
		MetricsEndpoint.MetricDescriptor response = this.endpoint.metric("cache", Collections.singletonList("host:1"));
		assertThat(response.getAvailableTags()
			.stream()
			.filter((t) -> t.getTag().equals("region"))
			.flatMap((t) -> t.getValues().stream())).containsExactly("east");
	}

	@Test
	void metricWithSpaceInTagValue() {
		this.registry.counter("counter", "key", "a space").increment(2);
		MetricsEndpoint.MetricDescriptor response = this.endpoint.metric("counter",
				Collections.singletonList("key:a space"));
		assertThat(response.getName()).isEqualTo("counter");
		assertThat(availableTagKeys(response)).isEmpty();
		assertThat(getCount(response)).hasValue(2.0);
	}

	@Test
	void metricWithInvalidTag() {
		assertThatExceptionOfType(InvalidEndpointRequestException.class)
			.isThrownBy(() -> this.endpoint.metric("counter", Collections.singletonList("key")));
	}

	@Test
	void metricPresentInOneRegistryOfACompositeAndNotAnother() {
		CompositeMeterRegistry composite = new CompositeMeterRegistry();
		SimpleMeterRegistry reg1 = new SimpleMeterRegistry();
		SimpleMeterRegistry reg2 = new SimpleMeterRegistry();
		composite.add(reg1);
		composite.add(reg2);
		reg1.counter("counter1").increment();
		reg2.counter("counter2").increment();
		MetricsEndpoint endpoint = new MetricsEndpoint(composite);
		assertThat(endpoint.metric("counter1", Collections.emptyList())).isNotNull();
		assertThat(endpoint.metric("counter2", Collections.emptyList())).isNotNull();
	}

	@Test
	void nonExistentMetric() {
		MetricsEndpoint.MetricDescriptor response = this.endpoint.metric("does.not.exist", Collections.emptyList());
		assertThat(response).isNull();
	}

	@Test
	void maxAggregation() {
		SimpleMeterRegistry reg = new SimpleMeterRegistry();
		reg.timer("timer", "k", "v1").record(1, TimeUnit.SECONDS);
		reg.timer("timer", "k", "v2").record(2, TimeUnit.SECONDS);
		assertMetricHasStatisticEqualTo(reg, "timer", Statistic.MAX, 2.0);
	}

	@Test
	void countAggregation() {
		SimpleMeterRegistry reg = new SimpleMeterRegistry();
		reg.counter("counter", "k", "v1").increment();
		reg.counter("counter", "k", "v2").increment();
		assertMetricHasStatisticEqualTo(reg, "counter", Statistic.COUNT, 2.0);
	}

	private void assertMetricHasStatisticEqualTo(MeterRegistry registry, String metricName, Statistic stat,
			Double value) {
		MetricsEndpoint endpoint = new MetricsEndpoint(registry);
		assertThat(endpoint.metric(metricName, Collections.emptyList())
			.getMeasurements()
			.stream()
			.filter((sample) -> sample.getStatistic().equals(stat))
			.findAny()).hasValueSatisfying((sample) -> assertThat(sample.getValue()).isEqualTo(value));
	}

	private Optional<Double> getCount(MetricsEndpoint.MetricDescriptor response) {
		return response.getMeasurements()
			.stream()
			.filter((sample) -> sample.getStatistic().equals(Statistic.COUNT))
			.findAny()
			.map(MetricsEndpoint.Sample::getValue);
	}

	private Stream<String> availableTagKeys(MetricsEndpoint.MetricDescriptor response) {
		return response.getAvailableTags().stream().map(MetricsEndpoint.AvailableTag::getTag);
	}

}
