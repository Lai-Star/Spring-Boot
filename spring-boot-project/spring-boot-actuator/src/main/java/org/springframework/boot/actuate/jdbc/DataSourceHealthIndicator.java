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

package org.springframework.boot.actuate.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.IncorrectResultSetColumnCountException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link HealthIndicator} that tests the status of a {@link DataSource} and optionally
 * runs a test query.
 *
 * @author Dave Syer
 * @author Christian Dupuis
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Arthur Kalimullin
 * @since 2.0.0
 */
public class DataSourceHealthIndicator extends AbstractHealthIndicator implements InitializingBean {

	private DataSource dataSource;

	private String query;

	private JdbcTemplate jdbcTemplate;

	/**
	 * Create a new {@link DataSourceHealthIndicator} instance.
	 */
	public DataSourceHealthIndicator() {
		this(null, null);
	}

	/**
	 * Create a new {@link DataSourceHealthIndicator} using the specified
	 * {@link DataSource}.
	 * @param dataSource the data source
	 */
	public DataSourceHealthIndicator(DataSource dataSource) {
		this(dataSource, null);
	}

	/**
	 * Create a new {@link DataSourceHealthIndicator} using the specified
	 * {@link DataSource} and validation query.
	 * @param dataSource the data source
	 * @param query the validation query to use (can be {@code null})
	 */
	public DataSourceHealthIndicator(DataSource dataSource, String query) {
		super("DataSource health check failed");
		this.dataSource = dataSource;
		this.query = query;
		this.jdbcTemplate = (dataSource != null) ? new JdbcTemplate(dataSource) : null;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.state(this.dataSource != null, "DataSource for DataSourceHealthIndicator must be specified");
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		if (this.dataSource == null) {
			builder.up().withDetail("database", "unknown");
		}
		else {
			doDataSourceHealthCheck(builder);
		}
	}

	private void doDataSourceHealthCheck(Health.Builder builder) {
		builder.up().withDetail("database", getProduct());
		String validationQuery = this.query;
		if (StringUtils.hasText(validationQuery)) {
			builder.withDetail("validationQuery", validationQuery);
			// Avoid calling getObject as it breaks MySQL on Java 7 and later
			List<Object> results = this.jdbcTemplate.query(validationQuery, new SingleColumnRowMapper());
			Object result = DataAccessUtils.requiredSingleResult(results);
			builder.withDetail("result", result);
		}
		else {
			builder.withDetail("validationQuery", "isValid()");
			boolean valid = isConnectionValid();
			builder.status((valid) ? Status.UP : Status.DOWN);
		}
	}

	private String getProduct() {
		return this.jdbcTemplate.execute((ConnectionCallback<String>) this::getProduct);
	}

	private String getProduct(Connection connection) throws SQLException {
		return connection.getMetaData().getDatabaseProductName();
	}

	private Boolean isConnectionValid() {
		return this.jdbcTemplate.execute((ConnectionCallback<Boolean>) this::isConnectionValid);
	}

	private Boolean isConnectionValid(Connection connection) throws SQLException {
		return connection.isValid(0);
	}

	/**
	 * Set the {@link DataSource} to use.
	 * @param dataSource the data source
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	/**
	 * Set a specific validation query to use to validate a connection. If none is set, a
	 * validation based on {@link Connection#isValid(int)} is used.
	 * @param query the validation query to use
	 */
	public void setQuery(String query) {
		this.query = query;
	}

	/**
	 * Return the validation query or {@code null}.
	 * @return the query
	 */
	public String getQuery() {
		return this.query;
	}

	/**
	 * {@link RowMapper} that expects and returns results from a single column.
	 */
	private static class SingleColumnRowMapper implements RowMapper<Object> {

		@Override
		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			ResultSetMetaData metaData = rs.getMetaData();
			int columns = metaData.getColumnCount();
			if (columns != 1) {
				throw new IncorrectResultSetColumnCountException(1, columns);
			}
			return JdbcUtils.getResultSetValue(rs, 1);
		}

	}

}
