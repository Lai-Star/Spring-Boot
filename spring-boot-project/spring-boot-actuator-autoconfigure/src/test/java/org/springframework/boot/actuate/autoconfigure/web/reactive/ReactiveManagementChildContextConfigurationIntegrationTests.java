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

package org.springframework.boot.actuate.autoconfigure.web.reactive;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.env.ConfigTreePropertySource;
import org.springframework.boot.test.context.assertj.AssertableReactiveWebApplicationContext;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.web.context.ServerPortInfoApplicationContextInitializer;
import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.http.MediaType;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ReactiveManagementChildContextConfiguration}.
 *
 * @author Andy Wilkinson
 */
class ReactiveManagementChildContextConfigurationIntegrationTests {

	private final ReactiveWebApplicationContextRunner runner = new ReactiveWebApplicationContextRunner(
			AnnotationConfigReactiveWebServerApplicationContext::new)
		.withConfiguration(AutoConfigurations.of(ManagementContextAutoConfiguration.class,
				ReactiveWebServerFactoryAutoConfiguration.class, ReactiveManagementContextAutoConfiguration.class,
				WebEndpointAutoConfiguration.class, EndpointAutoConfiguration.class, HttpHandlerAutoConfiguration.class,
				WebFluxAutoConfiguration.class))
		.withUserConfiguration(SucceedingEndpoint.class)
		.withInitializer(new ServerPortInfoApplicationContextInitializer())
		.withPropertyValues("server.port=0", "management.server.port=0", "management.endpoints.web.exposure.include=*");

	@TempDir
	Path temp;

	@Test
	void endpointsAreBeneathActuatorByDefault() {
		this.runner.withPropertyValues("management.server.port:0").run(withWebTestClient((client) -> {
			String body = client.get()
				.uri("actuator/success")
				.accept(MediaType.APPLICATION_JSON)
				.exchangeToMono((response) -> response.bodyToMono(String.class))
				.block();
			assertThat(body).isEqualTo("Success");
		}));
	}

	@Test
	void whenManagementServerBasePathIsConfiguredThenEndpointsAreBeneathThatPath() {
		this.runner.withPropertyValues("management.server.port:0", "management.server.base-path:/manage")
			.run(withWebTestClient((client) -> {
				String body = client.get()
					.uri("manage/actuator/success")
					.accept(MediaType.APPLICATION_JSON)
					.exchangeToMono((response) -> response.bodyToMono(String.class))
					.block();
				assertThat(body).isEqualTo("Success");
			}));
	}

	@Test // gh-32941
	void whenManagementServerPortLoadedFromConfigTree() {
		this.runner.withInitializer(this::addConfigTreePropertySource)
			.run((context) -> assertThat(context).hasNotFailed());
	}

	private void addConfigTreePropertySource(ConfigurableApplicationContext applicationContext) {
		try {
			applicationContext.getEnvironment()
				.setConversionService((ConfigurableConversionService) ApplicationConversionService.getSharedInstance());
			Path configtree = this.temp.resolve("configtree");
			Path file = configtree.resolve("management/server/port");
			file.toFile().getParentFile().mkdirs();
			FileCopyUtils.copy("0".getBytes(StandardCharsets.UTF_8), file.toFile());
			ConfigTreePropertySource source = new ConfigTreePropertySource("configtree", configtree);
			applicationContext.getEnvironment().getPropertySources().addFirst(source);
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private ContextConsumer<AssertableReactiveWebApplicationContext> withWebTestClient(Consumer<WebClient> webClient) {
		return (context) -> {
			String port = context.getEnvironment().getProperty("local.management.port");
			WebClient client = WebClient.create("http://localhost:" + port);
			webClient.accept(client);
		};
	}

	@Endpoint(id = "success")
	static class SucceedingEndpoint {

		@ReadOperation
		String fail() {
			return "Success";
		}

	}

}
