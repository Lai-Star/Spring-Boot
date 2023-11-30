/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.mongo;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.city.City;
import org.springframework.boot.autoconfigure.data.jpa.city.CityRepository;
import org.springframework.boot.autoconfigure.data.mongo.country.Country;
import org.springframework.boot.autoconfigure.data.mongo.country.CountryRepository;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MongoRepositoriesAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Oliver Gierke
 */
class MixedMongoRepositoriesAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@AfterEach
	void close() {
		this.context.close();
	}

	@Test
	void testDefaultRepositoryConfiguration() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration.class, BaseConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(CountryRepository.class)).isNotNull();
	}

	@Test
	void testMixedRepositoryConfiguration() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(MixedConfiguration.class, BaseConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(CountryRepository.class)).isNotNull();
		assertThat(this.context.getBean(CityRepository.class)).isNotNull();
	}

	@Test
	void testJpaRepositoryConfigurationWithMongoTemplate() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(JpaConfiguration.class, BaseConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(CityRepository.class)).isNotNull();
	}

	@Test
	void testJpaRepositoryConfigurationWithMongoOverlap() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(OverlapConfiguration.class, BaseConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(CityRepository.class)).isNotNull();
	}

	@Test
	void testJpaRepositoryConfigurationWithMongoOverlapDisabled() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.data.mongodb.repositories.type:none").applyTo(this.context);
		this.context.register(OverlapConfiguration.class, BaseConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(CityRepository.class)).isNotNull();
	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(MongoAutoConfiguration.class)
	// Not this package or its parent
	@EnableMongoRepositories(basePackageClasses = Country.class)
	static class TestConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(MongoAutoConfiguration.class)
	@EnableMongoRepositories(basePackageClasses = Country.class)
	@EntityScan(basePackageClasses = City.class)
	@EnableJpaRepositories(basePackageClasses = CityRepository.class)
	static class MixedConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(MongoAutoConfiguration.class)
	@EntityScan(basePackageClasses = City.class)
	@EnableJpaRepositories(basePackageClasses = CityRepository.class)
	static class JpaConfiguration {

	}

	// In this one the Jpa repositories and the auto-configuration packages overlap, so
	// Mongo will try and configure the same repositories
	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(CityRepository.class)
	@EnableJpaRepositories(basePackageClasses = CityRepository.class)
	static class OverlapConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@Import(Registrar.class)
	static class BaseConfiguration {

	}

	static class Registrar implements ImportSelector {

		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			List<String> names = new ArrayList<>();
			for (Class<?> type : new Class<?>[] { DataSourceAutoConfiguration.class,
					HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class,
					MongoAutoConfiguration.class, MongoDataAutoConfiguration.class,
					MongoRepositoriesAutoConfiguration.class }) {
				names.add(type.getName());
			}
			return StringUtils.toStringArray(names);
		}

	}

}
