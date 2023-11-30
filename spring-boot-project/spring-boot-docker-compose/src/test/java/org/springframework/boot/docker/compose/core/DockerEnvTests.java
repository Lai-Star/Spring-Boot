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

package org.springframework.boot.docker.compose.core;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link DockerEnv}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DockerEnvTests {

	@Test
	void createWhenEnvIsNullReturnsEmpty() {
		DockerEnv env = new DockerEnv(null);
		assertThat(env.asMap()).isEmpty();
	}

	@Test
	void createWhenEnvIsEmptyReturnsEmpty() {
		DockerEnv env = new DockerEnv(Collections.emptyList());
		assertThat(env.asMap()).isEmpty();
	}

	@Test
	void createParsesEnv() {
		DockerEnv env = new DockerEnv(List.of("a=b", "c"));
		assertThat(env.asMap()).containsExactly(entry("a", "b"), entry("c", null));
	}

}
