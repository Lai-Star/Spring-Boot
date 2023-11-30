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

package org.springframework.boot.autoconfigure.web.reactive;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ContextPathCompositeHandler;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.WebHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * Tests for {@link HttpHandlerAutoConfiguration}.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
class HttpHandlerAutoConfigurationTests {

	private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(HttpHandlerAutoConfiguration.class));

	@Test
	void shouldNotProcessIfExistingHttpHandler() {
		this.contextRunner.withUserConfiguration(CustomHttpHandler.class).run((context) -> {
			assertThat(context).hasSingleBean(HttpHandler.class);
			assertThat(context).getBean(HttpHandler.class).isSameAs(context.getBean("customHttpHandler"));
		});
	}

	@Test
	void shouldConfigureHttpHandlerAnnotation() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(WebFluxAutoConfiguration.class))
			.run((context) -> assertThat(context).hasSingleBean(HttpHandler.class));
	}

	@Test
	void shouldConfigureHttpHandlerWithoutWebFluxAutoConfiguration() {
		this.contextRunner.withUserConfiguration(CustomWebHandler.class)
			.run((context) -> assertThat(context).hasSingleBean(HttpHandler.class));
	}

	@Test
	void shouldConfigureBasePathCompositeHandler() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(WebFluxAutoConfiguration.class))
			.withPropertyValues("spring.webflux.base-path=/something")
			.run((context) -> {
				assertThat(context).hasSingleBean(HttpHandler.class);
				HttpHandler httpHandler = context.getBean(HttpHandler.class);
				assertThat(httpHandler).isInstanceOf(ContextPathCompositeHandler.class)
					.extracting("handlerMap", InstanceOfAssertFactories.map(String.class, HttpHandler.class))
					.containsKey("/something");
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomHttpHandler {

		@Bean
		HttpHandler customHttpHandler() {
			return (serverHttpRequest, serverHttpResponse) -> null;
		}

		@Bean
		RouterFunction<ServerResponse> routerFunction() {
			return route(GET("/test"), (serverRequest) -> null);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomWebHandler {

		@Bean
		WebHandler webHandler() {
			return new DispatcherHandler();
		}

	}

}
