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

package org.springframework.boot.test.util;

import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ApplicationContextTestUtils}.
 *
 * @author Stephane Nicoll
 */
class ApplicationContextTestUtilsTests {

	@Test
	void closeNull() {
		ApplicationContextTestUtils.closeAll(null);
	}

	@Test
	void closeNonClosableContext() {
		ApplicationContext mock = mock(ApplicationContext.class);
		ApplicationContextTestUtils.closeAll(mock);
	}

	@Test
	void closeContextAndParent() {
		ConfigurableApplicationContext mock = mock(ConfigurableApplicationContext.class);
		ConfigurableApplicationContext parent = mock(ConfigurableApplicationContext.class);
		given(mock.getParent()).willReturn(parent);
		given(parent.getParent()).willReturn(null);
		ApplicationContextTestUtils.closeAll(mock);
		then(mock).should().getParent();
		then(mock).should().close();
		then(parent).should().getParent();
		then(parent).should().close();
	}

}
