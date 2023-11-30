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

package org.springframework.boot.actuate.endpoint.web.annotation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
import org.springframework.boot.actuate.endpoint.invoke.convert.ConversionServiceParameterValueMapper;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.web.embedded.tomcat.TomcatEmbeddedWebappClassLoader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.convert.support.DefaultConversionService;

import static org.mockito.Mockito.mock;

/**
 * Base configuration shared by tests.
 *
 * @author Andy Wilkinson
 */
@Configuration(proxyBeanMethods = false)
class BaseConfiguration {

	@Bean
	AbstractWebEndpointIntegrationTests.EndpointDelegate endpointDelegate() {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		if (classLoader instanceof TomcatEmbeddedWebappClassLoader) {
			Thread.currentThread().setContextClassLoader(classLoader.getParent());
		}
		try {
			return mock(AbstractWebEndpointIntegrationTests.EndpointDelegate.class);
		}
		finally {
			Thread.currentThread().setContextClassLoader(classLoader);
		}
	}

	@Bean
	EndpointMediaTypes endpointMediaTypes() {
		List<String> mediaTypes = Arrays.asList("application/vnd.test+json", "application/json");
		return new EndpointMediaTypes(mediaTypes, mediaTypes);
	}

	@Bean
	WebEndpointDiscoverer webEndpointDiscoverer(EndpointMediaTypes endpointMediaTypes,
			ApplicationContext applicationContext) {
		ParameterValueMapper parameterMapper = new ConversionServiceParameterValueMapper(
				DefaultConversionService.getSharedInstance());
		return new WebEndpointDiscoverer(applicationContext, parameterMapper, endpointMediaTypes, null,
				Collections.emptyList(), Collections.emptyList());
	}

	@Bean
	static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

}
