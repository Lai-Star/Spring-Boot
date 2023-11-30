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

package org.springframework.boot.autoconfigure.elasticsearch;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClientBuilder;

/**
 * Callback interface that can be implemented by beans wishing to further customize the
 * {@link org.elasticsearch.client.RestClient} through a {@link RestClientBuilder} whilst
 * retaining default auto-configuration.
 *
 * @author Brian Clozel
 * @author Vedran Pavic
 * @since 2.1.0
 */
@FunctionalInterface
public interface RestClientBuilderCustomizer {

	/**
	 * Customize the {@link RestClientBuilder}.
	 * <p>
	 * Possibly overrides customizations made with the {@code "spring.elasticsearch.rest"}
	 * configuration properties namespace. For more targeted changes, see
	 * {@link #customize(HttpAsyncClientBuilder)} and
	 * {@link #customize(RequestConfig.Builder)}.
	 * @param builder the builder to customize
	 */
	void customize(RestClientBuilder builder);

	/**
	 * Customize the {@link HttpAsyncClientBuilder}.
	 * @param builder the builder
	 * @since 2.3.0
	 */
	default void customize(HttpAsyncClientBuilder builder) {
	}

	/**
	 * Customize the {@link Builder}.
	 * @param builder the builder
	 * @since 2.3.0
	 */
	default void customize(Builder builder) {
	}

}
