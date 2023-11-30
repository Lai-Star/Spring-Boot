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

package org.springframework.boot.actuate.autoconfigure.health;

import java.util.Collection;
import java.util.Map;

import org.springframework.boot.actuate.health.DefaultHealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.util.Assert;

/**
 * An auto-configured {@link HealthContributorRegistry} that ensures registered indicators
 * do not clash with groups names.
 *
 * @author Phillip Webb
 */
class AutoConfiguredHealthContributorRegistry extends DefaultHealthContributorRegistry {

	private final Collection<String> groupNames;

	AutoConfiguredHealthContributorRegistry(Map<String, HealthContributor> contributors,
			Collection<String> groupNames) {
		super(contributors);
		this.groupNames = groupNames;
		contributors.keySet().forEach(this::assertDoesNotClashWithGroup);
	}

	@Override
	public void registerContributor(String name, HealthContributor contributor) {
		assertDoesNotClashWithGroup(name);
		super.registerContributor(name, contributor);
	}

	private void assertDoesNotClashWithGroup(String name) {
		Assert.state(!this.groupNames.contains(name),
				() -> "HealthContributor with name \"" + name + "\" clashes with group");
	}

}
