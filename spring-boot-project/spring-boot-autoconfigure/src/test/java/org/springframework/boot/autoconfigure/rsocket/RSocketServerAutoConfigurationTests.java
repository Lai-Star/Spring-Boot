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

package org.springframework.boot.autoconfigure.rsocket;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.rsocket.context.RSocketPortInfoApplicationContextInitializer;
import org.springframework.boot.rsocket.context.RSocketServerBootstrap;
import org.springframework.boot.rsocket.server.RSocketServerCustomizer;
import org.springframework.boot.rsocket.server.RSocketServerFactory;
import org.springframework.boot.ssl.NoSuchSslBundleException;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.http.client.ReactorResourceFactory;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RSocketServerAutoConfiguration}.
 *
 * @author Brian Clozel
 * @author Verónica Vásquez
 * @author Scott Frederick
 */
class RSocketServerAutoConfigurationTests {

	@Test
	void shouldNotCreateBeansByDefault() {
		contextRunner().run((context) -> assertThat(context).doesNotHaveBean(WebServerFactoryCustomizer.class)
			.doesNotHaveBean(RSocketServerFactory.class)
			.doesNotHaveBean(RSocketServerBootstrap.class));
	}

	@Test
	void shouldNotCreateDefaultBeansForReactiveWebAppWithoutMapping() {
		reactiveWebContextRunner()
			.run((context) -> assertThat(context).doesNotHaveBean(WebServerFactoryCustomizer.class)
				.doesNotHaveBean(RSocketServerFactory.class)
				.doesNotHaveBean(RSocketServerBootstrap.class));
	}

	@Test
	void shouldNotCreateDefaultBeansForReactiveWebAppWithWrongTransport() {
		reactiveWebContextRunner()
			.withPropertyValues("spring.rsocket.server.transport=tcp", "spring.rsocket.server.mapping-path=/rsocket")
			.run((context) -> assertThat(context).doesNotHaveBean(WebServerFactoryCustomizer.class)
				.doesNotHaveBean(RSocketServerFactory.class)
				.doesNotHaveBean(RSocketServerBootstrap.class));
	}

	@Test
	void shouldCreateDefaultBeansForReactiveWebApp() {
		reactiveWebContextRunner()
			.withPropertyValues("spring.rsocket.server.transport=websocket",
					"spring.rsocket.server.mapping-path=/rsocket")
			.run((context) -> assertThat(context).hasSingleBean(RSocketWebSocketNettyRouteProvider.class));
	}

	@Test
	void shouldCreateDefaultBeansForRSocketServerWhenPortIsSet() {
		reactiveWebContextRunner().withPropertyValues("spring.rsocket.server.port=0")
			.run((context) -> assertThat(context).hasSingleBean(RSocketServerFactory.class)
				.hasSingleBean(RSocketServerBootstrap.class)
				.hasSingleBean(RSocketServerCustomizer.class));
	}

	@Test
	void shouldSetLocalServerPortWhenRSocketServerPortIsSet() {
		reactiveWebContextRunner().withPropertyValues("spring.rsocket.server.port=0")
			.withInitializer(new RSocketPortInfoApplicationContextInitializer())
			.run((context) -> {
				assertThat(context).hasSingleBean(RSocketServerFactory.class)
					.hasSingleBean(RSocketServerBootstrap.class)
					.hasSingleBean(RSocketServerCustomizer.class);
				assertThat(context.getEnvironment().getProperty("local.rsocket.server.port")).isNotNull();
			});
	}

	@Test
	void shouldSetFragmentWhenRSocketServerFragmentSizeIsSet() {
		reactiveWebContextRunner()
			.withPropertyValues("spring.rsocket.server.port=0", "spring.rsocket.server.fragment-size=12KB")
			.run((context) -> {
				assertThat(context).hasSingleBean(RSocketServerFactory.class);
				RSocketServerFactory factory = context.getBean(RSocketServerFactory.class);
				assertThat(factory).hasFieldOrPropertyWithValue("fragmentSize", DataSize.ofKilobytes(12));
			});
	}

	@Test
	void shouldFailToSetFragmentWhenRSocketServerFragmentSizeIsBelow64() {
		reactiveWebContextRunner()
			.withPropertyValues("spring.rsocket.server.port=0", "spring.rsocket.server.fragment-size=60B")
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context.getStartupFailure())
					.hasMessageContaining("The smallest allowed mtu size is 64 bytes, provided: 60");
			});
	}

	@Test
	void shouldUseSslWhenRocketServerSslIsConfigured() {
		reactiveWebContextRunner()
			.withPropertyValues("spring.rsocket.server.ssl.keyStore=classpath:rsocket/test.jks",
					"spring.rsocket.server.ssl.keyPassword=password", "spring.rsocket.server.port=0")
			.run((context) -> assertThat(context).hasSingleBean(RSocketServerFactory.class)
				.hasSingleBean(RSocketServerBootstrap.class)
				.hasSingleBean(RSocketServerCustomizer.class)
				.getBean(RSocketServerFactory.class)
				.hasFieldOrPropertyWithValue("ssl.keyStore", "classpath:rsocket/test.jks")
				.hasFieldOrPropertyWithValue("ssl.keyPassword", "password"));
	}

	@Test
	@Disabled
	void shouldUseSslWhenRocketServerSslIsConfiguredWithSslBundle() {
		reactiveWebContextRunner()
			.withPropertyValues("spring.rsocket.server.port=0", "spring.rsocket.server.ssl.bundle=test-bundle",
					"spring.ssl.bundle.jks.test-bundle.keystore.location=classpath:rsocket/test.jks",
					"spring.ssl.bundle.jks.test-bundle.key.password=password")
			.run((context) -> assertThat(context).hasSingleBean(RSocketServerFactory.class)
				.hasSingleBean(RSocketServerBootstrap.class)
				.hasSingleBean(RSocketServerCustomizer.class)
				.getBean(RSocketServerFactory.class)
				.hasFieldOrPropertyWithValue("sslBundle.details.keyStore", "classpath:rsocket/test.jks")
				.hasFieldOrPropertyWithValue("sslBundle.details.keyPassword", "password"));
	}

	@Test
	void shouldFailWhenSslIsConfiguredWithMissingBundle() {
		reactiveWebContextRunner()
			.withPropertyValues("spring.rsocket.server.port=0", "spring.rsocket.server.ssl.bundle=test-bundle")
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(NoSuchSslBundleException.class)
					.withFailMessage("SSL bundle name 'test-bundle' is not valid");
			});
	}

	@Test
	void shouldUseCustomServerBootstrap() {
		contextRunner().withUserConfiguration(CustomServerBootstrapConfig.class)
			.run((context) -> assertThat(context).getBeanNames(RSocketServerBootstrap.class)
				.containsExactly("customServerBootstrap"));
	}

	@Test
	void shouldUseCustomNettyRouteProvider() {
		reactiveWebContextRunner().withUserConfiguration(CustomNettyRouteProviderConfig.class)
			.withPropertyValues("spring.rsocket.server.transport=websocket",
					"spring.rsocket.server.mapping-path=/rsocket")
			.run((context) -> assertThat(context).getBeanNames(RSocketWebSocketNettyRouteProvider.class)
				.containsExactly("customNettyRouteProvider"));
	}

	@Test
	void whenSpringWebIsNotPresentThenEmbeddedServerConfigurationBacksOff() {
		contextRunner().withClassLoader(new FilteredClassLoader(ReactorResourceFactory.class))
			.withPropertyValues("spring.rsocket.server.port=0")
			.run((context) -> assertThat(context).doesNotHaveBean(RSocketServerFactory.class));
	}

	private ApplicationContextRunner contextRunner() {
		return new ApplicationContextRunner().withUserConfiguration(BaseConfiguration.class)
			.withConfiguration(AutoConfigurations.of(RSocketServerAutoConfiguration.class));
	}

	private ReactiveWebApplicationContextRunner reactiveWebContextRunner() {
		return new ReactiveWebApplicationContextRunner().withUserConfiguration(BaseConfiguration.class)
			.withConfiguration(AutoConfigurations.of(RSocketServerAutoConfiguration.class, SslAutoConfiguration.class));
	}

	@Configuration(proxyBeanMethods = false)
	static class BaseConfiguration {

		@Bean
		RSocketMessageHandler messageHandler() {
			RSocketMessageHandler messageHandler = new RSocketMessageHandler();
			messageHandler.setRSocketStrategies(RSocketStrategies.builder()
				.encoder(CharSequenceEncoder.textPlainOnly())
				.decoder(StringDecoder.allMimeTypes())
				.build());
			return messageHandler;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomServerBootstrapConfig {

		@Bean
		RSocketServerBootstrap customServerBootstrap() {
			return mock(RSocketServerBootstrap.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomNettyRouteProviderConfig {

		@Bean
		RSocketWebSocketNettyRouteProvider customNettyRouteProvider() {
			return mock(RSocketWebSocketNettyRouteProvider.class);
		}

	}

}
