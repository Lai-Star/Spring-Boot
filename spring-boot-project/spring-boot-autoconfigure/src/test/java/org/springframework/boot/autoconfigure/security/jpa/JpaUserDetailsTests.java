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

package org.springframework.boot.autoconfigure.security.jpa;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

/**
 * The EntityScanRegistrar can cause problems with Spring security and its eager
 * instantiation needs. This test is designed to fail if the Entities can't be scanned
 * because the registrar doesn't get a callback with the right beans (essentially because
 * their instantiation order was accelerated by Security).
 *
 * @author Dave Syer
 */
@ContextConfiguration(classes = JpaUserDetailsTests.Main.class, loader = SpringBootContextLoader.class)
@DirtiesContext
class JpaUserDetailsTests {

	@Test
	void contextLoads() {
	}

	@Import({ EmbeddedDataSourceConfiguration.class, DataSourceAutoConfiguration.class,
			HibernateJpaAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class,
			SecurityAutoConfiguration.class })
	static class Main {

	}

}
