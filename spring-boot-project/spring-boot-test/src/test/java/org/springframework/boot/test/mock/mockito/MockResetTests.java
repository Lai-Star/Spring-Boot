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

package org.springframework.boot.test.mock.mockito;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.mock.mockito.example.ExampleService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * Tests for {@link MockReset}.
 *
 * @author Phillip Webb
 */
class MockResetTests {

	@Test
	void noneAttachesReset() {
		ExampleService mock = mock(ExampleService.class);
		assertThat(MockReset.get(mock)).isEqualTo(MockReset.NONE);
	}

	@Test
	void withSettingsOfNoneAttachesReset() {
		ExampleService mock = mock(ExampleService.class, MockReset.withSettings(MockReset.NONE));
		assertThat(MockReset.get(mock)).isEqualTo(MockReset.NONE);
	}

	@Test
	void beforeAttachesReset() {
		ExampleService mock = mock(ExampleService.class, MockReset.before());
		assertThat(MockReset.get(mock)).isEqualTo(MockReset.BEFORE);
	}

	@Test
	void afterAttachesReset() {
		ExampleService mock = mock(ExampleService.class, MockReset.after());
		assertThat(MockReset.get(mock)).isEqualTo(MockReset.AFTER);
	}

	@Test
	void withSettingsAttachesReset() {
		ExampleService mock = mock(ExampleService.class, MockReset.withSettings(MockReset.BEFORE));
		assertThat(MockReset.get(mock)).isEqualTo(MockReset.BEFORE);
	}

	@Test
	void apply() {
		ExampleService mock = mock(ExampleService.class, MockReset.apply(MockReset.AFTER, withSettings()));
		assertThat(MockReset.get(mock)).isEqualTo(MockReset.AFTER);
	}

}
