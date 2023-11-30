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

package org.springframework.boot.autoconfigure.cassandra;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import org.junit.jupiter.api.Test;
import org.rnorth.ducttape.TimeoutException;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.testcontainers.CassandraContainer;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link CassandraAutoConfiguration} that only uses password authentication.
 *
 * @author Stephane Nicoll
 */
@Testcontainers(disabledWithoutDocker = true)
class CassandraAutoConfigurationWithPasswordAuthenticationIntegrationTests {

	@Container
	static final CassandraContainer cassandra = new PasswordAuthenticatorCassandraContainer().withStartupAttempts(5)
		.withStartupTimeout(Duration.ofMinutes(10))
		.waitingFor(new CassandraWaitStrategy());

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(CassandraAutoConfiguration.class))
		.withPropertyValues(
				"spring.cassandra.contact-points:" + cassandra.getHost() + ":" + cassandra.getFirstMappedPort(),
				"spring.cassandra.local-datacenter=datacenter1", "spring.cassandra.connection.connect-timeout=60s",
				"spring.cassandra.connection.init-query-timeout=60s", "spring.cassandra.request.timeout=60s");

	@Test
	void authenticationWithValidUsernameAndPassword() {
		this.contextRunner
			.withPropertyValues("spring.cassandra.username=cassandra", "spring.cassandra.password=cassandra")
			.run((context) -> {
				SimpleStatement select = SimpleStatement.newInstance("SELECT release_version FROM system.local")
					.setConsistencyLevel(ConsistencyLevel.LOCAL_ONE);
				assertThat(context.getBean(CqlSession.class).execute(select).one()).isNotNull();
			});
	}

	@Test
	void authenticationWithInvalidCredentials() {
		this.contextRunner
			.withPropertyValues("spring.cassandra.username=not-a-user", "spring.cassandra.password=invalid-password")
			.run((context) -> assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() -> context.getBean(CqlSession.class))
				.withMessageContaining("Authentication error"));
	}

	static final class PasswordAuthenticatorCassandraContainer extends CassandraContainer {

		@Override
		protected void containerIsCreated(String containerId) {
			String config = copyFileFromContainer("/etc/cassandra/cassandra.yaml",
					(stream) -> StreamUtils.copyToString(stream, StandardCharsets.UTF_8));
			String updatedConfig = config.replace("authenticator: AllowAllAuthenticator",
					"authenticator: PasswordAuthenticator");
			copyFileToContainer(Transferable.of(updatedConfig.getBytes(StandardCharsets.UTF_8)),
					"/etc/cassandra/cassandra.yaml");
		}

	}

	static final class CassandraWaitStrategy extends AbstractWaitStrategy {

		@Override
		protected void waitUntilReady() {
			try {
				Unreliables.retryUntilSuccess((int) this.startupTimeout.getSeconds(), TimeUnit.SECONDS, () -> {
					getRateLimiter().doWhenReady(() -> cqlSessionBuilder().build());
					return true;
				});
			}
			catch (TimeoutException ex) {
				throw new ContainerLaunchException(
						"Timed out waiting for Cassandra to be accessible for query execution");
			}
		}

		private CqlSessionBuilder cqlSessionBuilder() {
			return CqlSession.builder()
				.addContactPoint(new InetSocketAddress(this.waitStrategyTarget.getHost(),
						this.waitStrategyTarget.getFirstMappedPort()))
				.withLocalDatacenter("datacenter1")
				.withAuthCredentials("cassandra", "cassandra");
		}

	}

}
