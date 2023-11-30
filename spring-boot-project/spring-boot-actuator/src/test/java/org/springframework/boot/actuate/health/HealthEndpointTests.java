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

package org.springframework.boot.actuate.health;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.actuate.health.HealthEndpointSupport.HealthResult;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HealthEndpoint}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
@ExtendWith(OutputCaptureExtension.class)
class HealthEndpointTests extends
		HealthEndpointSupportTests<HealthEndpoint, HealthContributorRegistry, HealthContributor, HealthComponent> {

	@Test
	void healthReturnsSystemHealth() {
		this.registry.registerContributor("test", createContributor(this.up));
		HealthComponent health = create(this.registry, this.groups).health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health).isInstanceOf(SystemHealth.class);
	}

	@Test
	void healthWithNoContributorReturnsUp() {
		assertThat(this.registry).isEmpty();
		HealthComponent health = create(this.registry,
				HealthEndpointGroups.of(mock(HealthEndpointGroup.class), Collections.emptyMap()))
			.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health).isInstanceOf(Health.class);
	}

	@Test
	void healthWhenPathDoesNotExistReturnsNull() {
		this.registry.registerContributor("test", createContributor(this.up));
		HealthComponent health = create(this.registry, this.groups).healthForPath("missing");
		assertThat(health).isNull();
	}

	@Test
	void healthWhenPathExistsReturnsHealth() {
		this.registry.registerContributor("test", createContributor(this.up));
		HealthComponent health = create(this.registry, this.groups).healthForPath("test");
		assertThat(health).isEqualTo(this.up);
	}

	@Test
	void healthWhenIndicatorIsSlow(CapturedOutput output) {
		HealthIndicator indicator = () -> {
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException ex) {
			}
			return this.up;
		};
		this.registry.registerContributor("test", indicator);
		create(this.registry, this.groups, Duration.ofMillis(10)).health();
		assertThat(output).contains("Health contributor");
		assertThat(output).contains("to respond");

	}

	@Override
	protected HealthEndpoint create(HealthContributorRegistry registry, HealthEndpointGroups groups,
			Duration slowIndicatorLoggingThreshold) {
		return new HealthEndpoint(registry, groups, slowIndicatorLoggingThreshold);
	}

	@Override
	protected HealthContributorRegistry createRegistry() {
		return new DefaultHealthContributorRegistry();
	}

	@Override
	protected HealthContributor createContributor(Health health) {
		return (HealthIndicator) () -> health;
	}

	@Override
	protected HealthContributor createCompositeContributor(Map<String, HealthContributor> contributors) {
		return CompositeHealthContributor.fromMap(contributors);
	}

	@Override
	protected HealthComponent getHealth(HealthResult<HealthComponent> result) {
		return result.getHealth();
	}

}
