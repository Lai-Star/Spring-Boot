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

package org.springframework.boot.autoconfigure.security.servlet;

import java.util.function.Function;

import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * {@link RequestMatcherProvider} that provides an {@link AntPathRequestMatcher}.
 *
 * @author Madhura Bhave
 * @since 2.1.8
 */
public class AntPathRequestMatcherProvider implements RequestMatcherProvider {

	private final Function<String, String> pathFactory;

	public AntPathRequestMatcherProvider(Function<String, String> pathFactory) {
		this.pathFactory = pathFactory;
	}

	@Override
	public RequestMatcher getRequestMatcher(String pattern) {
		return new AntPathRequestMatcher(this.pathFactory.apply(pattern));
	}

}
