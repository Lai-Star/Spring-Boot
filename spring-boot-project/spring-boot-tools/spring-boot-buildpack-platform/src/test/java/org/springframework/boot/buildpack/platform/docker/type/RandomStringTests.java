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

package org.springframework.boot.buildpack.platform.docker.type;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link RandomString}.
 *
 * @author Phillip Webb
 */
class RandomStringTests {

	@Test
	void generateWhenPrefixIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> RandomString.generate(null, 10))
			.withMessage("Prefix must not be null");
	}

	@Test
	void generateGeneratesRandomString() {
		String s1 = RandomString.generate("abc-", 10);
		String s2 = RandomString.generate("abc-", 10);
		String s3 = RandomString.generate("abc-", 20);
		assertThat(s1).hasSize(14).startsWith("abc-").isNotEqualTo(s2);
		assertThat(s3).hasSize(24).startsWith("abc-");
	}

}
