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

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.annotation.AbstractWebEndpointIntegrationTests;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpointDiscoverer;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContext;
import org.springframework.boot.web.reactive.context.ReactiveWebServerInitializedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for web endpoints exposed using WebFlux.
 *
 * @author Andy Wilkinson
 * @see WebFluxEndpointHandlerMapping
 */
class WebFluxEndpointIntegrationTests
		extends AbstractWebEndpointIntegrationTests<AnnotationConfigReactiveWebServerApplicationContext> {

	WebFluxEndpointIntegrationTests() {
		super(WebFluxEndpointIntegrationTests::createApplicationContext,
				WebFluxEndpointIntegrationTests::applyAuthenticatedConfiguration);

	}

	private static AnnotationConfigReactiveWebServerApplicationContext createApplicationContext() {
		AnnotationConfigReactiveWebServerApplicationContext context = new AnnotationConfigReactiveWebServerApplicationContext();
		context.register(ReactiveConfiguration.class);
		return context;
	}

	private static void applyAuthenticatedConfiguration(AnnotationConfigReactiveWebServerApplicationContext context) {
		context.register(AuthenticatedConfiguration.class);
	}

	@Test
	void responseToOptionsRequestIncludesCorsHeaders() {
		load(TestEndpointConfiguration.class,
				(client) -> client.options()
					.uri("/test")
					.accept(MediaType.APPLICATION_JSON)
					.header("Access-Control-Request-Method", "POST")
					.header("Origin", "https://example.com")
					.exchange()
					.expectStatus()
					.isOk()
					.expectHeader()
					.valueEquals("Access-Control-Allow-Origin", "https://example.com")
					.expectHeader()
					.valueEquals("Access-Control-Allow-Methods", "GET,POST"));
	}

	@Test
	void readOperationsThatReturnAResourceSupportRangeRequests() {
		load(ResourceEndpointConfiguration.class, (client) -> {
			byte[] responseBody = client.get()
				.uri("/resource")
				.header("Range", "bytes=0-3")
				.exchange()
				.expectStatus()
				.isEqualTo(HttpStatus.PARTIAL_CONTENT)
				.expectHeader()
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.returnResult(byte[].class)
				.getResponseBodyContent();
			assertThat(responseBody).containsExactly(0, 1, 2, 3);
		});
	}

	@Override
	protected int getPort(AnnotationConfigReactiveWebServerApplicationContext context) {
		return context.getBean(ReactiveConfiguration.class).port;
	}

	@Configuration(proxyBeanMethods = false)
	@EnableWebFlux
	@ImportAutoConfiguration(ErrorWebFluxAutoConfiguration.class)
	static class ReactiveConfiguration {

		private int port;

		@Bean
		NettyReactiveWebServerFactory netty() {
			return new NettyReactiveWebServerFactory(0);
		}

		@Bean
		HttpHandler httpHandler(ApplicationContext applicationContext) {
			return WebHttpHandlerBuilder.applicationContext(applicationContext).build();
		}

		@Bean
		WebFluxEndpointHandlerMapping webEndpointHandlerMapping(Environment environment,
				WebEndpointDiscoverer endpointDiscoverer, EndpointMediaTypes endpointMediaTypes) {
			CorsConfiguration corsConfiguration = new CorsConfiguration();
			corsConfiguration.setAllowedOrigins(Arrays.asList("https://example.com"));
			corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST"));
			String endpointPath = environment.getProperty("endpointPath");
			return new WebFluxEndpointHandlerMapping(new EndpointMapping(endpointPath),
					endpointDiscoverer.getEndpoints(), endpointMediaTypes, corsConfiguration,
					new EndpointLinksResolver(endpointDiscoverer.getEndpoints()), StringUtils.hasText(endpointPath));
		}

		@Bean
		ApplicationListener<ReactiveWebServerInitializedEvent> serverInitializedListener() {
			return (event) -> this.port = event.getWebServer().getPort();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class AuthenticatedConfiguration {

		@Bean
		WebFilter webFilter() {
			return (exchange, chain) -> chain.filter(exchange)
				.contextWrite(ReactiveSecurityContextHolder.withAuthentication(new UsernamePasswordAuthenticationToken(
						"Alice", "secret", Arrays.asList(new SimpleGrantedAuthority("ROLE_ACTUATOR")))));
		}

	}

}
