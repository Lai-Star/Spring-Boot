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

package org.springframework.boot.autoconfigure.validation;

import java.util.HashMap;

import jakarta.validation.Validator;
import jakarta.validation.constraints.Min;
import org.hibernate.validator.HibernateValidator;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link ValidatorAdapter}.
 *
 * @author Stephane Nicoll
 * @author Madhura Bhave
 */
class ValidatorAdapterTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	void wrapLocalValidatorFactoryBean() {
		this.contextRunner.withUserConfiguration(LocalValidatorFactoryBeanConfig.class).run((context) -> {
			ValidatorAdapter wrapper = context.getBean(ValidatorAdapter.class);
			assertThat(wrapper.supports(SampleData.class)).isTrue();
			MapBindingResult errors = new MapBindingResult(new HashMap<String, Object>(), "test");
			wrapper.validate(new SampleData(40), errors);
			assertThat(errors.getErrorCount()).isOne();
		});
	}

	@Test
	void wrapperInvokesCallbackOnNonManagedBean() {
		this.contextRunner.withUserConfiguration(NonManagedBeanConfig.class).run((context) -> {
			LocalValidatorFactoryBean validator = context.getBean(NonManagedBeanConfig.class).validator;
			then(validator).should().setApplicationContext(any(ApplicationContext.class));
			then(validator).should().afterPropertiesSet();
			then(validator).should(never()).destroy();
			context.close();
			then(validator).should().destroy();
		});
	}

	@Test
	void wrapperDoesNotInvokeCallbackOnManagedBean() {
		this.contextRunner.withUserConfiguration(ManagedBeanConfig.class).run((context) -> {
			LocalValidatorFactoryBean validator = context.getBean(ManagedBeanConfig.class).validator;
			then(validator).should(never()).setApplicationContext(any(ApplicationContext.class));
			then(validator).should(never()).afterPropertiesSet();
			then(validator).should(never()).destroy();
			context.close();
			then(validator).should(never()).destroy();
		});
	}

	@Test
	void wrapperWhenValidationProviderNotPresentShouldNotThrowException() {
		ClassPathResource hibernateValidator = new ClassPathResource(
				"META-INF/services/jakarta.validation.spi.ValidationProvider");
		this.contextRunner
			.withClassLoader(new FilteredClassLoader(FilteredClassLoader.ClassPathResourceFilter.of(hibernateValidator),
					FilteredClassLoader.PackageFilter.of("org.hibernate.validator")))
			.run((context) -> ValidatorAdapter.get(context, null));
	}

	@Test
	void unwrapToJakartaValidatorShouldReturnJakartaValidator() {
		this.contextRunner.withUserConfiguration(LocalValidatorFactoryBeanConfig.class).run((context) -> {
			ValidatorAdapter wrapper = context.getBean(ValidatorAdapter.class);
			assertThat(wrapper.unwrap(Validator.class)).isInstanceOf(Validator.class);
		});
	}

	@Test
	void whenJakartaValidatorIsWrappedMultipleTimesUnwrapToJakartaValidatorShouldReturnJakartaValidator() {
		this.contextRunner.withUserConfiguration(DoubleWrappedConfig.class).run((context) -> {
			ValidatorAdapter wrapper = context.getBean(ValidatorAdapter.class);
			assertThat(wrapper.unwrap(Validator.class)).isInstanceOf(Validator.class);
		});
	}

	@Test
	void unwrapToUnsupportedTypeShouldThrow() {
		this.contextRunner.withUserConfiguration(LocalValidatorFactoryBeanConfig.class).run((context) -> {
			ValidatorAdapter wrapper = context.getBean(ValidatorAdapter.class);
			assertThatRuntimeException().isThrownBy(() -> wrapper.unwrap(HibernateValidator.class));
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class LocalValidatorFactoryBeanConfig {

		@Bean
		LocalValidatorFactoryBean validator() {
			return new LocalValidatorFactoryBean();
		}

		@Bean
		ValidatorAdapter wrapper(LocalValidatorFactoryBean validator) {
			return new ValidatorAdapter(validator, true);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DoubleWrappedConfig {

		@Bean
		LocalValidatorFactoryBean validator() {
			return new LocalValidatorFactoryBean();
		}

		@Bean
		ValidatorAdapter wrapper(LocalValidatorFactoryBean validator) {
			return new ValidatorAdapter(new Wrapper(validator), true);
		}

		static class Wrapper implements SmartValidator {

			private final SmartValidator delegate;

			Wrapper(SmartValidator delegate) {
				this.delegate = delegate;
			}

			@Override
			public boolean supports(Class<?> clazz) {
				return this.delegate.supports(clazz);
			}

			@Override
			public void validate(Object target, Errors errors) {
				this.delegate.validate(target, errors);
			}

			@Override
			public void validate(Object target, Errors errors, Object... validationHints) {
				this.delegate.validate(target, errors, validationHints);
			}

			@Override
			@SuppressWarnings("unchecked")
			public <T> T unwrap(Class<T> type) {
				if (type.isInstance(this.delegate)) {
					return (T) this.delegate;
				}
				return this.delegate.unwrap(type);
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	static class NonManagedBeanConfig {

		private final LocalValidatorFactoryBean validator = mock(LocalValidatorFactoryBean.class);

		@Bean
		ValidatorAdapter wrapper() {
			return new ValidatorAdapter(this.validator, false);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ManagedBeanConfig {

		private final LocalValidatorFactoryBean validator = mock(LocalValidatorFactoryBean.class);

		@Bean
		ValidatorAdapter wrapper() {
			return new ValidatorAdapter(this.validator, true);
		}

	}

	static class SampleData {

		@Min(42)
		private final int counter;

		SampleData(int counter) {
			this.counter = counter;
		}

	}

}
