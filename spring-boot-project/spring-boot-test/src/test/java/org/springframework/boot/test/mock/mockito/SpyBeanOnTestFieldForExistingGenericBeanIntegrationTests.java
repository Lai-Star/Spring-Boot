/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.test.mock.mockito;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.example.ExampleGenericService;
import org.springframework.boot.test.mock.mockito.example.ExampleGenericServiceCaller;
import org.springframework.boot.test.mock.mockito.example.SimpleExampleIntegerGenericService;
import org.springframework.boot.test.mock.mockito.example.SimpleExampleStringGenericService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

/**
 * Test {@link SpyBean @SpyBean} on a test class field can be used to replace existing
 * beans.
 *
 * @author Phillip Webb
 * @see SpyBeanOnTestFieldForExistingBeanCacheIntegrationTests
 */
@ExtendWith(SpringExtension.class)
class SpyBeanOnTestFieldForExistingGenericBeanIntegrationTests {

	// gh-7625

	@SpyBean
	private ExampleGenericService<String> exampleService;

	@Autowired
	private ExampleGenericServiceCaller caller;

	@Test
	void testSpying() {
		assertThat(this.caller.sayGreeting()).isEqualTo("I say 123 simple");
		then(this.exampleService).should().greeting();
	}

	@Configuration(proxyBeanMethods = false)
	@Import({ ExampleGenericServiceCaller.class, SimpleExampleIntegerGenericService.class })
	static class SpyBeanOnTestFieldForExistingBeanConfig {

		@Bean
		ExampleGenericService<String> simpleExampleStringGenericService() {
			// In order to trigger issue we need a method signature that returns the
			// generic type not the actual implementation class
			return new SimpleExampleStringGenericService();
		}

	}

}
