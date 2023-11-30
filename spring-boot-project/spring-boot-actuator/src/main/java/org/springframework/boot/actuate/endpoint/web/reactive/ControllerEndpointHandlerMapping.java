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

package org.springframework.boot.actuate.endpoint.web.reactive;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpoint;
import org.springframework.boot.actuate.endpoint.web.annotation.ExposableControllerEndpoint;
import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint;
import org.springframework.util.Assert;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

/**
 * {@link HandlerMapping} that exposes {@link ControllerEndpoint @ControllerEndpoint} and
 * {@link RestControllerEndpoint @RestControllerEndpoint} annotated endpoints over Spring
 * WebFlux.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
public class ControllerEndpointHandlerMapping extends RequestMappingHandlerMapping {

	private final EndpointMapping endpointMapping;

	private final CorsConfiguration corsConfiguration;

	private final Map<Object, ExposableControllerEndpoint> handlers;

	/**
	 * Create a new {@link ControllerEndpointHandlerMapping} instance providing mappings
	 * for the specified endpoints.
	 * @param endpointMapping the base mapping for all endpoints
	 * @param endpoints the web endpoints
	 * @param corsConfiguration the CORS configuration for the endpoints or {@code null}
	 */
	public ControllerEndpointHandlerMapping(EndpointMapping endpointMapping,
			Collection<ExposableControllerEndpoint> endpoints, CorsConfiguration corsConfiguration) {
		Assert.notNull(endpointMapping, "EndpointMapping must not be null");
		Assert.notNull(endpoints, "Endpoints must not be null");
		this.endpointMapping = endpointMapping;
		this.handlers = getHandlers(endpoints);
		this.corsConfiguration = corsConfiguration;
		setOrder(-100);
	}

	private Map<Object, ExposableControllerEndpoint> getHandlers(Collection<ExposableControllerEndpoint> endpoints) {
		Map<Object, ExposableControllerEndpoint> handlers = new LinkedHashMap<>();
		endpoints.forEach((endpoint) -> handlers.put(endpoint.getController(), endpoint));
		return Collections.unmodifiableMap(handlers);
	}

	@Override
	protected void initHandlerMethods() {
		this.handlers.keySet().forEach(this::detectHandlerMethods);
	}

	@Override
	protected void registerHandlerMethod(Object handler, Method method, RequestMappingInfo mapping) {
		ExposableControllerEndpoint endpoint = this.handlers.get(handler);
		mapping = withEndpointMappedPatterns(endpoint, mapping);
		super.registerHandlerMethod(handler, method, mapping);
	}

	private RequestMappingInfo withEndpointMappedPatterns(ExposableControllerEndpoint endpoint,
			RequestMappingInfo mapping) {
		Set<PathPattern> patterns = mapping.getPatternsCondition().getPatterns();
		if (patterns.isEmpty()) {
			patterns = Collections.singleton(getPathPatternParser().parse(""));
		}
		String[] endpointMappedPatterns = patterns.stream()
			.map((pattern) -> getEndpointMappedPattern(endpoint, pattern))
			.toArray(String[]::new);
		return mapping.mutate().paths(endpointMappedPatterns).build();
	}

	private String getEndpointMappedPattern(ExposableControllerEndpoint endpoint, PathPattern pattern) {
		return this.endpointMapping.createSubPath(endpoint.getRootPath() + pattern);
	}

	@Override
	protected boolean hasCorsConfigurationSource(Object handler) {
		return this.corsConfiguration != null;
	}

	@Override
	protected CorsConfiguration initCorsConfiguration(Object handler, Method method, RequestMappingInfo mapping) {
		return this.corsConfiguration;
	}

}
