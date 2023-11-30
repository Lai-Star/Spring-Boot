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

package org.springframework.boot.actuate.health;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A collection of named health endpoint contributors (either {@link HealthContributor} or
 * {@link ReactiveHealthContributor}).
 *
 * @param <C> the contributor type
 * @author Phillip Webb
 * @since 2.0.0
 * @see NamedContributor
 */
public interface NamedContributors<C> extends Iterable<NamedContributor<C>> {

	/**
	 * Return the contributor with the given name.
	 * @param name the name of the contributor
	 * @return a contributor instance or {@code null}
	 */
	C getContributor(String name);

	/**
	 * Return a stream of the {@link NamedContributor named contributors}.
	 * @return the stream of named contributors
	 */
	default Stream<NamedContributor<C>> stream() {
		return StreamSupport.stream(spliterator(), false);
	}

}
