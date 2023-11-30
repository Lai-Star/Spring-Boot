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

package org.springframework.boot.autoconfigure.amqp;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.ContainerCustomizer;
import org.springframework.amqp.rabbit.config.DirectRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.RabbitListenerConfigUtils;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.DirectMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.autoconfigure.thread.Threading;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.VirtualThreadTaskExecutor;

/**
 * Configuration for Spring AMQP annotation driven endpoints.
 *
 * @author Stephane Nicoll
 * @author Josh Thornhill
 * @author Moritz Halbritter
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(EnableRabbit.class)
class RabbitAnnotationDrivenConfiguration {

	private final ObjectProvider<MessageConverter> messageConverter;

	private final ObjectProvider<MessageRecoverer> messageRecoverer;

	private final ObjectProvider<RabbitRetryTemplateCustomizer> retryTemplateCustomizers;

	private final RabbitProperties properties;

	RabbitAnnotationDrivenConfiguration(ObjectProvider<MessageConverter> messageConverter,
			ObjectProvider<MessageRecoverer> messageRecoverer,
			ObjectProvider<RabbitRetryTemplateCustomizer> retryTemplateCustomizers, RabbitProperties properties) {
		this.messageConverter = messageConverter;
		this.messageRecoverer = messageRecoverer;
		this.retryTemplateCustomizers = retryTemplateCustomizers;
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnThreading(Threading.PLATFORM)
	SimpleRabbitListenerContainerFactoryConfigurer simpleRabbitListenerContainerFactoryConfigurer() {
		return simpleListenerConfigurer();
	}

	@Bean(name = "simpleRabbitListenerContainerFactoryConfigurer")
	@ConditionalOnMissingBean
	@ConditionalOnThreading(Threading.VIRTUAL)
	SimpleRabbitListenerContainerFactoryConfigurer simpleRabbitListenerContainerFactoryConfigurerVirtualThreads() {
		SimpleRabbitListenerContainerFactoryConfigurer configurer = simpleListenerConfigurer();
		configurer.setTaskExecutor(new VirtualThreadTaskExecutor());
		return configurer;
	}

	@Bean(name = "rabbitListenerContainerFactory")
	@ConditionalOnMissingBean(name = "rabbitListenerContainerFactory")
	@ConditionalOnProperty(prefix = "spring.rabbitmq.listener", name = "type", havingValue = "simple",
			matchIfMissing = true)
	SimpleRabbitListenerContainerFactory simpleRabbitListenerContainerFactory(
			SimpleRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory,
			ObjectProvider<ContainerCustomizer<SimpleMessageListenerContainer>> simpleContainerCustomizer) {
		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		configurer.configure(factory, connectionFactory);
		simpleContainerCustomizer.ifUnique(factory::setContainerCustomizer);
		return factory;
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnThreading(Threading.PLATFORM)
	DirectRabbitListenerContainerFactoryConfigurer directRabbitListenerContainerFactoryConfigurer() {
		return directListenerConfigurer();
	}

	@Bean(name = "directRabbitListenerContainerFactoryConfigurer")
	@ConditionalOnMissingBean
	@ConditionalOnThreading(Threading.VIRTUAL)
	DirectRabbitListenerContainerFactoryConfigurer directRabbitListenerContainerFactoryConfigurerVirtualThreads() {
		DirectRabbitListenerContainerFactoryConfigurer configurer = directListenerConfigurer();
		configurer.setTaskExecutor(new VirtualThreadTaskExecutor());
		return configurer;
	}

	@Bean(name = "rabbitListenerContainerFactory")
	@ConditionalOnMissingBean(name = "rabbitListenerContainerFactory")
	@ConditionalOnProperty(prefix = "spring.rabbitmq.listener", name = "type", havingValue = "direct")
	DirectRabbitListenerContainerFactory directRabbitListenerContainerFactory(
			DirectRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory,
			ObjectProvider<ContainerCustomizer<DirectMessageListenerContainer>> directContainerCustomizer) {
		DirectRabbitListenerContainerFactory factory = new DirectRabbitListenerContainerFactory();
		configurer.configure(factory, connectionFactory);
		directContainerCustomizer.ifUnique(factory::setContainerCustomizer);
		return factory;
	}

	private SimpleRabbitListenerContainerFactoryConfigurer simpleListenerConfigurer() {
		SimpleRabbitListenerContainerFactoryConfigurer configurer = new SimpleRabbitListenerContainerFactoryConfigurer(
				this.properties);
		configurer.setMessageConverter(this.messageConverter.getIfUnique());
		configurer.setMessageRecoverer(this.messageRecoverer.getIfUnique());
		configurer.setRetryTemplateCustomizers(this.retryTemplateCustomizers.orderedStream().toList());
		return configurer;
	}

	private DirectRabbitListenerContainerFactoryConfigurer directListenerConfigurer() {
		DirectRabbitListenerContainerFactoryConfigurer configurer = new DirectRabbitListenerContainerFactoryConfigurer(
				this.properties);
		configurer.setMessageConverter(this.messageConverter.getIfUnique());
		configurer.setMessageRecoverer(this.messageRecoverer.getIfUnique());
		configurer.setRetryTemplateCustomizers(this.retryTemplateCustomizers.orderedStream().toList());
		return configurer;
	}

	@Configuration(proxyBeanMethods = false)
	@EnableRabbit
	@ConditionalOnMissingBean(name = RabbitListenerConfigUtils.RABBIT_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME)
	static class EnableRabbitConfiguration {

	}

}
