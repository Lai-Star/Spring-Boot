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

package org.springframework.boot.test.web.client;

import org.springframework.aot.AotDetector;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate.HttpClientOption;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.servlet.server.AbstractServletWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContextAnnotationUtils;

/**
 * {@link ContextCustomizer} for {@link TestRestTemplate}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class TestRestTemplateContextCustomizer implements ContextCustomizer {

	@Override
	public void customizeContext(ConfigurableApplicationContext context,
			MergedContextConfiguration mergedContextConfiguration) {
		if (AotDetector.useGeneratedArtifacts()) {
			return;
		}
		SpringBootTest springBootTest = TestContextAnnotationUtils
			.findMergedAnnotation(mergedContextConfiguration.getTestClass(), SpringBootTest.class);
		if (springBootTest.webEnvironment().isEmbedded()) {
			registerTestRestTemplate(context);
		}
	}

	private void registerTestRestTemplate(ConfigurableApplicationContext context) {
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		if (beanFactory instanceof BeanDefinitionRegistry registry) {
			registerTestRestTemplate(registry);
		}
	}

	private void registerTestRestTemplate(BeanDefinitionRegistry registry) {
		RootBeanDefinition definition = new RootBeanDefinition(TestRestTemplateRegistrar.class);
		definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		registry.registerBeanDefinition(TestRestTemplateRegistrar.class.getName(), definition);
	}

	@Override
	public boolean equals(Object obj) {
		return (obj != null) && (obj.getClass() == getClass());
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	/**
	 * {@link BeanDefinitionRegistryPostProcessor} that runs after the
	 * {@link ConfigurationClassPostProcessor} and add a {@link TestRestTemplateFactory}
	 * bean definition when a {@link TestRestTemplate} hasn't already been registered.
	 */
	static class TestRestTemplateRegistrar implements BeanDefinitionRegistryPostProcessor, Ordered, BeanFactoryAware {

		private BeanFactory beanFactory;

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			this.beanFactory = beanFactory;
		}

		@Override
		public int getOrder() {
			return Ordered.LOWEST_PRECEDENCE;
		}

		@Override
		public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
			if (AotDetector.useGeneratedArtifacts()) {
				return;
			}
			if (BeanFactoryUtils.beanNamesForTypeIncludingAncestors((ListableBeanFactory) this.beanFactory,
					TestRestTemplate.class, false, false).length == 0) {
				registry.registerBeanDefinition(TestRestTemplate.class.getName(),
						new RootBeanDefinition(TestRestTemplateFactory.class));
			}

		}

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		}

	}

	/**
	 * {@link FactoryBean} used to create and configure a {@link TestRestTemplate}.
	 */
	public static class TestRestTemplateFactory implements FactoryBean<TestRestTemplate>, ApplicationContextAware {

		private static final HttpClientOption[] DEFAULT_OPTIONS = {};

		private static final HttpClientOption[] SSL_OPTIONS = { HttpClientOption.SSL };

		private TestRestTemplate template;

		@Override
		public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
			RestTemplateBuilder builder = getRestTemplateBuilder(applicationContext);
			boolean sslEnabled = isSslEnabled(applicationContext);
			TestRestTemplate template = new TestRestTemplate(builder, null, null,
					sslEnabled ? SSL_OPTIONS : DEFAULT_OPTIONS);
			LocalHostUriTemplateHandler handler = new LocalHostUriTemplateHandler(applicationContext.getEnvironment(),
					sslEnabled ? "https" : "http");
			template.setUriTemplateHandler(handler);
			this.template = template;
		}

		private boolean isSslEnabled(ApplicationContext context) {
			try {
				AbstractServletWebServerFactory webServerFactory = context
					.getBean(AbstractServletWebServerFactory.class);
				return webServerFactory.getSsl() != null && webServerFactory.getSsl().isEnabled();
			}
			catch (NoSuchBeanDefinitionException ex) {
				return false;
			}
		}

		private RestTemplateBuilder getRestTemplateBuilder(ApplicationContext applicationContext) {
			try {
				return applicationContext.getBean(RestTemplateBuilder.class);
			}
			catch (NoSuchBeanDefinitionException ex) {
				return new RestTemplateBuilder();
			}
		}

		@Override
		public boolean isSingleton() {
			return true;
		}

		@Override
		public Class<?> getObjectType() {
			return TestRestTemplate.class;
		}

		@Override
		public TestRestTemplate getObject() throws Exception {
			return this.template;
		}

	}

}
