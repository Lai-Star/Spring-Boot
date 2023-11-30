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
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.health.HealthEndpointSupport.HealthResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ReactiveHealthEndpointWebExtension}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class ReactiveHealthEndpointWebExtensionTests extends
		HealthEndpointSupportTests<ReactiveHealthEndpointWebExtension, ReactiveHealthContributorRegistry, ReactiveHealthContributor, Mono<? extends HealthComponent>> {

	@Test
	void healthReturnsSystemHealth() {
		this.registry.registerContributor("test", createContributor(this.up));
		WebEndpointResponse<? extends HealthComponent> response = create(this.registry, this.groups)
			.health(ApiVersion.LATEST, null, SecurityContext.NONE)
			.block();
		HealthComponent health = response.getBody();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health).isInstanceOf(SystemHealth.class);
		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	void healthWithNoContributorReturnsUp() {
		assertThat(this.registry).isEmpty();
		WebEndpointResponse<? extends HealthComponent> response = create(this.registry,
				HealthEndpointGroups.of(mock(HealthEndpointGroup.class), Collections.emptyMap()))
			.health(ApiVersion.LATEST, null, SecurityContext.NONE)
			.block();
		assertThat(response.getStatus()).isEqualTo(200);
		HealthComponent health = response.getBody();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health).isInstanceOf(Health.class);
	}

	@Test
	void healthWhenPathDoesNotExistReturnsHttp404() {
		this.registry.registerContributor("test", createContributor(this.up));
		WebEndpointResponse<? extends HealthComponent> response = create(this.registry, this.groups)
			.health(ApiVersion.LATEST, null, SecurityContext.NONE, "missing")
			.block();
		assertThat(response.getBody()).isNull();
		assertThat(response.getStatus()).isEqualTo(404);
	}

	@Test
	void healthWhenPathExistsReturnsHealth() {
		this.registry.registerContributor("test", createContributor(this.up));
		WebEndpointResponse<? extends HealthComponent> response = create(this.registry, this.groups)
			.health(ApiVersion.LATEST, null, SecurityContext.NONE, "test")
			.block();
		assertThat(response.getBody()).isEqualTo(this.up);
		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Override
	protected ReactiveHealthEndpointWebExtension create(ReactiveHealthContributorRegistry registry,
			HealthEndpointGroups groups, Duration slowIndicatorLoggingThreshold) {
		return new ReactiveHealthEndpointWebExtension(registry, groups, slowIndicatorLoggingThreshold);
	}

	@Override
	protected ReactiveHealthContributorRegistry createRegistry() {
		return new DefaultReactiveHealthContributorRegistry();
	}

	@Override
	protected ReactiveHealthContributor createContributor(Health health) {
		return (ReactiveHealthIndicator) () -> Mono.just(health);
	}

	@Override
	protected ReactiveHealthContributor createCompositeContributor(
			Map<String, ReactiveHealthContributor> contributors) {
		return CompositeReactiveHealthContributor.fromMap(contributors);
	}

	@Override
	protected HealthComponent getHealth(HealthResult<Mono<? extends HealthComponent>> result) {
		return result.getHealth().block();
	}

}
