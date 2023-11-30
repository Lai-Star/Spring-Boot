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

package org.springframework.boot.autoconfigure.jdbc;

import java.sql.Connection;
import java.time.Duration;

import javax.sql.DataSource;

import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceImpl;
import oracle.ucp.util.OpaqueString;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataSourceAutoConfiguration} with Oracle UCP.
 *
 * @author Fabio Grassi
 * @author Stephane Nicoll
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class OracleUcpDataSourceConfigurationTests {

	private static final String PREFIX = "spring.datasource.oracleucp.";

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
		.withPropertyValues("spring.datasource.type=" + PoolDataSource.class.getName());

	@Test
	void testDataSourceExists() {
		this.contextRunner.run((context) -> {
			assertThat(context.getBeansOfType(DataSource.class)).hasSize(1);
			assertThat(context.getBeansOfType(PoolDataSourceImpl.class)).hasSize(1);
			try (Connection connection = context.getBean(DataSource.class).getConnection()) {
				assertThat(connection.isValid(1000)).isTrue();
			}
		});
	}

	@Test
	void testDataSourcePropertiesOverridden() {
		this.contextRunner.withPropertyValues(PREFIX + "url=jdbc:foo//bar/spam", PREFIX + "max-idle-time=1234")
			.run((context) -> {
				PoolDataSourceImpl ds = context.getBean(PoolDataSourceImpl.class);
				assertThat(ds.getURL()).isEqualTo("jdbc:foo//bar/spam");
				assertThat(ds.getMaxIdleTime()).isEqualTo(1234);
			});
	}

	@Test
	void testDataSourceConnectionPropertiesOverridden() {
		this.contextRunner.withPropertyValues(PREFIX + "connection-properties.autoCommit=false").run((context) -> {
			PoolDataSourceImpl ds = context.getBean(PoolDataSourceImpl.class);
			assertThat(ds.getConnectionProperty("autoCommit")).isEqualTo("false");
		});
	}

	@Test
	void testDataSourceDefaultsPreserved() {
		this.contextRunner.run((context) -> {
			PoolDataSourceImpl ds = context.getBean(PoolDataSourceImpl.class);
			assertThat(ds.getInitialPoolSize()).isZero();
			assertThat(ds.getMinPoolSize()).isEqualTo(1);
			assertThat(ds.getMaxPoolSize()).isEqualTo(Integer.MAX_VALUE);
			assertThat(ds.getInactiveConnectionTimeout()).isZero();
			assertThat(ds.getConnectionWaitDuration()).isEqualTo(Duration.ofSeconds(3));
			assertThat(ds.getTimeToLiveConnectionTimeout()).isZero();
			assertThat(ds.getAbandonedConnectionTimeout()).isZero();
			assertThat(ds.getTimeoutCheckInterval()).isEqualTo(30);
			assertThat(ds.getFastConnectionFailoverEnabled()).isFalse();
		});
	}

	@Test
	void nameIsAliasedToPoolName() {
		this.contextRunner.withPropertyValues("spring.datasource.name=myDS").run((context) -> {
			PoolDataSourceImpl ds = context.getBean(PoolDataSourceImpl.class);
			assertThat(ds.getConnectionPoolName()).isEqualTo("myDS");
		});
	}

	@Test
	void poolNameTakesPrecedenceOverName() {
		this.contextRunner
			.withPropertyValues("spring.datasource.name=myDS", PREFIX + "connection-pool-name=myOracleUcpDS")
			.run((context) -> {
				PoolDataSourceImpl ds = context.getBean(PoolDataSourceImpl.class);
				assertThat(ds.getConnectionPoolName()).isEqualTo("myOracleUcpDS");
			});
	}

	@Test
	void usesCustomJdbcConnectionDetailsWhenDefined() {
		this.contextRunner.withBean(JdbcConnectionDetails.class, TestJdbcConnectionDetails::new)
			.withPropertyValues(PREFIX + "url=jdbc:broken", PREFIX + "username=alice", PREFIX + "password=secret")
			.run((context) -> {
				assertThat(context).hasSingleBean(JdbcConnectionDetails.class)
					.doesNotHaveBean(PropertiesJdbcConnectionDetails.class);
				DataSource dataSource = context.getBean(DataSource.class);
				assertThat(dataSource).isInstanceOf(PoolDataSourceImpl.class);
				PoolDataSourceImpl oracleUcp = (PoolDataSourceImpl) dataSource;
				assertThat(oracleUcp.getUser()).isEqualTo("user-1");
				assertThat(oracleUcp).extracting("password")
					.extracting((o) -> ((OpaqueString) o).get())
					.isEqualTo("password-1");
				assertThat(oracleUcp.getConnectionFactoryClassName())
					.isEqualTo(DatabaseDriver.POSTGRESQL.getDriverClassName());
				assertThat(oracleUcp.getURL()).isEqualTo("jdbc:customdb://customdb.example.com:12345/database-1");
			});
	}

}
