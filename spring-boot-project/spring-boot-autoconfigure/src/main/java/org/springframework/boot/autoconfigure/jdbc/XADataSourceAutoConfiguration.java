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

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import jakarta.transaction.TransactionManager;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties.DataSourceBeanCreationException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertyNameAliases;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.jdbc.XADataSourceWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link DataSource} with XA.
 *
 * @author Phillip Webb
 * @author Josh Long
 * @author Madhura Bhave
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @since 1.2.0
 */
@AutoConfiguration(before = DataSourceAutoConfiguration.class)
@EnableConfigurationProperties(DataSourceProperties.class)
@ConditionalOnClass({ DataSource.class, TransactionManager.class, EmbeddedDatabaseType.class })
@ConditionalOnBean(XADataSourceWrapper.class)
@ConditionalOnMissingBean(DataSource.class)
public class XADataSourceAutoConfiguration implements BeanClassLoaderAware {

	private ClassLoader classLoader;

	@Bean
	@ConditionalOnMissingBean(JdbcConnectionDetails.class)
	PropertiesJdbcConnectionDetails jdbcConnectionDetails(DataSourceProperties properties) {
		return new PropertiesJdbcConnectionDetails(properties);
	}

	@Bean
	public DataSource dataSource(XADataSourceWrapper wrapper, DataSourceProperties properties,
			JdbcConnectionDetails connectionDetails, ObjectProvider<XADataSource> xaDataSource) throws Exception {
		return wrapper
			.wrapDataSource(xaDataSource.getIfAvailable(() -> createXaDataSource(properties, connectionDetails)));
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	private XADataSource createXaDataSource(DataSourceProperties properties, JdbcConnectionDetails connectionDetails) {
		String className = connectionDetails.getXaDataSourceClassName();
		Assert.state(StringUtils.hasLength(className), "No XA DataSource class name specified");
		XADataSource dataSource = createXaDataSourceInstance(className);
		bindXaProperties(dataSource, properties, connectionDetails);
		return dataSource;
	}

	private XADataSource createXaDataSourceInstance(String className) {
		try {
			Class<?> dataSourceClass = ClassUtils.forName(className, this.classLoader);
			Object instance = BeanUtils.instantiateClass(dataSourceClass);
			Assert.isInstanceOf(XADataSource.class, instance);
			return (XADataSource) instance;
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to create XADataSource instance from '" + className + "'");
		}
	}

	private void bindXaProperties(XADataSource target, DataSourceProperties dataSourceProperties,
			JdbcConnectionDetails connectionDetails) {
		Binder binder = new Binder(getBinderSource(dataSourceProperties, connectionDetails));
		binder.bind(ConfigurationPropertyName.EMPTY, Bindable.ofInstance(target));
	}

	private ConfigurationPropertySource getBinderSource(DataSourceProperties dataSourceProperties,
			JdbcConnectionDetails connectionDetails) {
		Map<Object, Object> properties = new HashMap<>(dataSourceProperties.getXa().getProperties());
		properties.computeIfAbsent("user", (key) -> connectionDetails.getUsername());
		properties.computeIfAbsent("password", (key) -> connectionDetails.getPassword());
		try {
			properties.computeIfAbsent("url", (key) -> connectionDetails.getJdbcUrl());
		}
		catch (DataSourceBeanCreationException ex) {
			// Continue as not all XA DataSource's require a URL
		}
		MapConfigurationPropertySource source = new MapConfigurationPropertySource(properties);
		ConfigurationPropertyNameAliases aliases = new ConfigurationPropertyNameAliases();
		aliases.addAliases("user", "username");
		return source.withAliases(aliases);
	}

}
