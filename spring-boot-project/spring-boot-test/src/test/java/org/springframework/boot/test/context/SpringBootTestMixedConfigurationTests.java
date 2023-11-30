/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.test.context;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTestMixedConfigurationTests.Config;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootTest @SpringBootTest}.
 *
 * @author Dave Syer
 */
@DirtiesContext
@SpringBootTest
@ContextConfiguration(classes = Config.class, locations = "classpath:test.groovy")
class SpringBootTestMixedConfigurationTests {

	@Autowired
	private String foo;

	@Autowired
	private Config config;

	@Test
	void mixedConfigClasses() {
		assertThat(this.foo).isNotNull();
		assertThat(this.config).isNotNull();
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

	}

}
