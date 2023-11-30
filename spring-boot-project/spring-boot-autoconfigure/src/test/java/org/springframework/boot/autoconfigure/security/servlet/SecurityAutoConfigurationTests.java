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

package org.springframework.boot.autoconfigure.security.servlet;

import java.security.interfaces.RSAPublicKey;
import java.util.EnumSet;

import jakarta.servlet.DispatcherType;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.test.City;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean;
import org.springframework.boot.web.servlet.filter.OrderedFilter;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.data.repository.query.SecurityEvaluationContextExtension;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SecurityAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Rob Winch
 * @author Andy Wilkinson
 * @author Madhura Bhave
 */
class SecurityAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(SecurityAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class));

	@Test
	void testWebConfiguration() {
		this.contextRunner.run((context) -> {
			assertThat(context.getBean(AuthenticationManagerBuilder.class)).isNotNull();
			assertThat(context.getBean(FilterChainProxy.class).getFilterChains()).hasSize(1);
		});
	}

	@Test
	void enableWebSecurityIsConditionalOnClass() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("org.springframework.security.config"))
			.run((context) -> assertThat(context).doesNotHaveBean("springSecurityFilterChain"));
	}

	@Test
	void filterChainBeanIsConditionalOnClassSecurityFilterChain() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(SecurityFilterChain.class))
			.run((context) -> assertThat(context).doesNotHaveBean(SecurityFilterChain.class));
	}

	@Test
	void securityConfigurerBacksOffWhenOtherSecurityFilterChainBeanPresent() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(WebMvcAutoConfiguration.class))
			.withUserConfiguration(TestSecurityFilterChainConfig.class)
			.run((context) -> {
				assertThat(context.getBeansOfType(SecurityFilterChain.class)).hasSize(1);
				assertThat(context.containsBean("testSecurityFilterChain")).isTrue();
			});
	}

	@Test
	void testFilterIsNotRegisteredInNonWeb() {
		try (AnnotationConfigApplicationContext customContext = new AnnotationConfigApplicationContext()) {
			customContext.register(SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class,
					PropertyPlaceholderAutoConfiguration.class);
			customContext.refresh();
			assertThat(customContext.containsBean("securityFilterChainRegistration")).isFalse();
		}
	}

	@Test
	void defaultAuthenticationEventPublisherRegistered() {
		this.contextRunner.run((context) -> assertThat(context.getBean(AuthenticationEventPublisher.class))
			.isInstanceOf(DefaultAuthenticationEventPublisher.class));
	}

	@Test
	void defaultAuthenticationEventPublisherIsConditionalOnMissingBean() {
		this.contextRunner.withUserConfiguration(AuthenticationEventPublisherConfiguration.class)
			.run((context) -> assertThat(context.getBean(AuthenticationEventPublisher.class))
				.isInstanceOf(AuthenticationEventPublisherConfiguration.TestAuthenticationEventPublisher.class));
	}

	@Test
	void testDefaultFilterOrder() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(SecurityFilterAutoConfiguration.class))
			.run((context) -> assertThat(
					context.getBean("securityFilterChainRegistration", DelegatingFilterProxyRegistrationBean.class)
						.getOrder())
				.isEqualTo(OrderedFilter.REQUEST_WRAPPER_FILTER_MAX_ORDER - 100));
	}

	@Test
	void testCustomFilterOrder() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(SecurityFilterAutoConfiguration.class))
			.withPropertyValues("spring.security.filter.order:12345")
			.run((context) -> assertThat(
					context.getBean("securityFilterChainRegistration", DelegatingFilterProxyRegistrationBean.class)
						.getOrder())
				.isEqualTo(12345));
	}

	@Test
	void testJpaCoexistsHappily() {
		this.contextRunner.withPropertyValues("spring.datasource.url:jdbc:hsqldb:mem:testsecdb")
			.withUserConfiguration(EntityConfiguration.class)
			.withConfiguration(
					AutoConfigurations.of(HibernateJpaAutoConfiguration.class, DataSourceAutoConfiguration.class))
			.run((context) -> assertThat(context.getBean(JpaTransactionManager.class)).isNotNull());
		// This can fail if security @Conditionals force early instantiation of the
		// HibernateJpaAutoConfiguration (e.g. the EntityManagerFactory is not found)
	}

	@Test
	void testSecurityEvaluationContextExtensionSupport() {
		this.contextRunner
			.run((context) -> assertThat(context).getBean(SecurityEvaluationContextExtension.class).isNotNull());
	}

	@Test
	void defaultFilterDispatcherTypes() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(SecurityFilterAutoConfiguration.class))
			.run((context) -> {
				DelegatingFilterProxyRegistrationBean bean = context.getBean("securityFilterChainRegistration",
						DelegatingFilterProxyRegistrationBean.class);
				assertThat(bean).extracting("dispatcherTypes", InstanceOfAssertFactories.iterable(DispatcherType.class))
					.containsExactlyInAnyOrderElementsOf(EnumSet.allOf(DispatcherType.class));
			});
	}

	@Test
	void customFilterDispatcherTypes() {
		this.contextRunner.withPropertyValues("spring.security.filter.dispatcher-types:INCLUDE,ERROR")
			.withConfiguration(AutoConfigurations.of(SecurityFilterAutoConfiguration.class))
			.run((context) -> {
				DelegatingFilterProxyRegistrationBean bean = context.getBean("securityFilterChainRegistration",
						DelegatingFilterProxyRegistrationBean.class);
				assertThat(bean).extracting("dispatcherTypes", InstanceOfAssertFactories.iterable(DispatcherType.class))
					.containsOnly(DispatcherType.INCLUDE, DispatcherType.ERROR);
			});
	}

	@Test
	void emptyFilterDispatcherTypesDoNotThrowException() {
		this.contextRunner.withPropertyValues("spring.security.filter.dispatcher-types:")
			.withConfiguration(AutoConfigurations.of(SecurityFilterAutoConfiguration.class))
			.run((context) -> {
				DelegatingFilterProxyRegistrationBean bean = context.getBean("securityFilterChainRegistration",
						DelegatingFilterProxyRegistrationBean.class);
				assertThat(bean).extracting("dispatcherTypes", InstanceOfAssertFactories.iterable(DispatcherType.class))
					.isEmpty();
			});
	}

	@Test
	void whenAConfigurationPropertyBindingConverterIsDefinedThenBindingToAnRsaKeySucceeds() {
		this.contextRunner.withUserConfiguration(ConverterConfiguration.class, PropertiesConfiguration.class)
			.withPropertyValues("jwt.public-key=classpath:public-key-location")
			.run((context) -> assertThat(context.getBean(JwtProperties.class).getPublicKey()).isNotNull());
	}

	@Test
	void whenTheBeanFactoryHasAConversionServiceAndAConfigurationPropertyBindingConverterIsDefinedThenBindingToAnRsaKeySucceeds() {
		this.contextRunner
			.withInitializer(
					(context) -> context.getBeanFactory().setConversionService(new ApplicationConversionService()))
			.withUserConfiguration(ConverterConfiguration.class, PropertiesConfiguration.class)
			.withPropertyValues("jwt.public-key=classpath:public-key-location")
			.run((context) -> assertThat(context.getBean(JwtProperties.class).getPublicKey()).isNotNull());
	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(City.class)
	static class EntityConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class AuthenticationEventPublisherConfiguration {

		@Bean
		AuthenticationEventPublisher authenticationEventPublisher() {
			return new TestAuthenticationEventPublisher();
		}

		class TestAuthenticationEventPublisher implements AuthenticationEventPublisher {

			@Override
			public void publishAuthenticationSuccess(Authentication authentication) {

			}

			@Override
			public void publishAuthenticationFailure(AuthenticationException exception, Authentication authentication) {

			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestSecurityFilterChainConfig {

		@Bean
		SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
			return http.securityMatcher("/**")
				.authorizeHttpRequests((authorize) -> authorize.anyRequest().authenticated())
				.build();

		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConverterConfiguration {

		@Bean
		@ConfigurationPropertiesBinding
		Converter<String, TargetType> targetTypeConverter() {
			return new Converter<>() {

				@Override
				public TargetType convert(String input) {
					return new TargetType();
				}

			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(JwtProperties.class)
	static class PropertiesConfiguration {

	}

	@ConfigurationProperties("jwt")
	static class JwtProperties {

		private RSAPublicKey publicKey;

		RSAPublicKey getPublicKey() {
			return this.publicKey;
		}

		void setPublicKey(RSAPublicKey publicKey) {
			this.publicKey = publicKey;
		}

	}

	static class TargetType {

	}

}
