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

package org.springframework.boot.actuate.autoconfigure.cloudfoundry;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AccessLevel}.
 *
 * @author Madhura Bhave
 */
class AccessLevelTests {

	@Test
	void accessToHealthEndpointShouldNotBeRestricted() {
		assertThat(AccessLevel.RESTRICTED.isAccessAllowed("health")).isTrue();
		assertThat(AccessLevel.FULL.isAccessAllowed("health")).isTrue();
	}

	@Test
	void accessToInfoEndpointShouldNotBeRestricted() {
		assertThat(AccessLevel.RESTRICTED.isAccessAllowed("info")).isTrue();
		assertThat(AccessLevel.FULL.isAccessAllowed("info")).isTrue();
	}

	@Test
	void accessToDiscoveryEndpointShouldNotBeRestricted() {
		assertThat(AccessLevel.RESTRICTED.isAccessAllowed("")).isTrue();
		assertThat(AccessLevel.FULL.isAccessAllowed("")).isTrue();
	}

	@Test
	void accessToAnyOtherEndpointShouldBeRestricted() {
		assertThat(AccessLevel.RESTRICTED.isAccessAllowed("env")).isFalse();
		assertThat(AccessLevel.FULL.isAccessAllowed("")).isTrue();
	}

}
