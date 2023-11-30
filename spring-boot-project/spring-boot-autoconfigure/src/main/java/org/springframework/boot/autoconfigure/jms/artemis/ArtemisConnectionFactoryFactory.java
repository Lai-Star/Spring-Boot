/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.jms.artemis;

import java.lang.reflect.Constructor;

import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnectorFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Factory to create an Artemis {@link ActiveMQConnectionFactory} instance from properties
 * defined in {@link ArtemisProperties}.
 *
 * @author Eddú Meléndez
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Justin Bertram
 */
class ArtemisConnectionFactoryFactory {

	private static final String DEFAULT_BROKER_URL = "tcp://localhost:61616";

	static final String[] EMBEDDED_JMS_CLASSES = { "org.apache.activemq.artemis.jms.server.embedded.EmbeddedJMS",
			"org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ" };

	private final ArtemisProperties properties;

	private final ListableBeanFactory beanFactory;

	ArtemisConnectionFactoryFactory(ListableBeanFactory beanFactory, ArtemisProperties properties) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		Assert.notNull(properties, "Properties must not be null");
		this.beanFactory = beanFactory;
		this.properties = properties;
	}

	<T extends ActiveMQConnectionFactory> T createConnectionFactory(Class<T> factoryClass) {
		try {
			startEmbeddedJms();
			return doCreateConnectionFactory(factoryClass);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to create ActiveMQConnectionFactory", ex);
		}
	}

	private void startEmbeddedJms() {
		for (String embeddedJmsClass : EMBEDDED_JMS_CLASSES) {
			if (ClassUtils.isPresent(embeddedJmsClass, null)) {
				try {
					this.beanFactory.getBeansOfType(Class.forName(embeddedJmsClass));
				}
				catch (Exception ex) {
					// Ignore
				}
			}
		}
	}

	private <T extends ActiveMQConnectionFactory> T doCreateConnectionFactory(Class<T> factoryClass) throws Exception {
		ArtemisMode mode = this.properties.getMode();
		if (mode == null) {
			mode = deduceMode();
		}
		if (mode == ArtemisMode.EMBEDDED) {
			return createEmbeddedConnectionFactory(factoryClass);
		}
		return createNativeConnectionFactory(factoryClass);
	}

	/**
	 * Deduce the {@link ArtemisMode} to use if none has been set.
	 * @return the mode
	 */
	private ArtemisMode deduceMode() {
		if (this.properties.getEmbedded().isEnabled() && isEmbeddedJmsClassPresent()) {
			return ArtemisMode.EMBEDDED;
		}
		return ArtemisMode.NATIVE;
	}

	private boolean isEmbeddedJmsClassPresent() {
		for (String embeddedJmsClass : EMBEDDED_JMS_CLASSES) {
			if (ClassUtils.isPresent(embeddedJmsClass, null)) {
				return true;
			}
		}
		return false;
	}

	private <T extends ActiveMQConnectionFactory> T createEmbeddedConnectionFactory(Class<T> factoryClass)
			throws Exception {
		try {
			TransportConfiguration transportConfiguration = new TransportConfiguration(
					InVMConnectorFactory.class.getName(), this.properties.getEmbedded().generateTransportParameters());
			ServerLocator serviceLocator = ActiveMQClient.createServerLocatorWithoutHA(transportConfiguration);
			return factoryClass.getConstructor(ServerLocator.class).newInstance(serviceLocator);
		}
		catch (NoClassDefFoundError ex) {
			throw new IllegalStateException("Unable to create InVM "
					+ "Artemis connection, ensure that artemis-jms-server.jar is in the classpath", ex);
		}
	}

	private <T extends ActiveMQConnectionFactory> T createNativeConnectionFactory(Class<T> factoryClass)
			throws Exception {
		T connectionFactory = newNativeConnectionFactory(factoryClass);
		String user = this.properties.getUser();
		if (StringUtils.hasText(user)) {
			connectionFactory.setUser(user);
			connectionFactory.setPassword(this.properties.getPassword());
		}
		return connectionFactory;
	}

	private <T extends ActiveMQConnectionFactory> T newNativeConnectionFactory(Class<T> factoryClass) throws Exception {
		String brokerUrl = StringUtils.hasText(this.properties.getBrokerUrl()) ? this.properties.getBrokerUrl()
				: DEFAULT_BROKER_URL;
		Constructor<T> constructor = factoryClass.getConstructor(String.class);
		return constructor.newInstance(brokerUrl);

	}

}
