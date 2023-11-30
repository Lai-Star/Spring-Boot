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

import com.rabbitmq.client.ConnectionFactory;
import org.junit.jupiter.api.Test;

import org.springframework.amqp.rabbit.config.DirectRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.DirectMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link RabbitProperties}.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Rafael Carvalho
 * @author Scott Frederick
 */
class RabbitPropertiesTests {

	private final RabbitProperties properties = new RabbitProperties();

	@Test
	void hostDefaultsToLocalhost() {
		assertThat(this.properties.getHost()).isEqualTo("localhost");
	}

	@Test
	void customHost() {
		this.properties.setHost("rabbit.example.com");
		assertThat(this.properties.getHost()).isEqualTo("rabbit.example.com");
	}

	@Test
	void hostIsDeterminedFromFirstAddress() {
		this.properties.setAddresses("rabbit1.example.com:1234,rabbit2.example.com:2345");
		assertThat(this.properties.determineHost()).isEqualTo("rabbit1.example.com");
	}

	@Test
	void determineHostReturnsHostPropertyWhenNoAddresses() {
		this.properties.setHost("rabbit.example.com");
		assertThat(this.properties.determineHost()).isEqualTo("rabbit.example.com");
	}

	@Test
	void portDefaultsToNull() {
		assertThat(this.properties.getPort()).isNull();
	}

	@Test
	void customPort() {
		this.properties.setPort(1234);
		assertThat(this.properties.getPort()).isEqualTo(1234);
	}

	@Test
	void determinePortReturnsPortOfFirstAddress() {
		this.properties.setAddresses("rabbit1.example.com:1234,rabbit2.example.com:2345");
		assertThat(this.properties.determinePort()).isEqualTo(1234);
	}

	@Test
	void determinePortReturnsDefaultPortWhenNoAddresses() {
		assertThat(this.properties.determinePort()).isEqualTo(5672);
	}

	@Test
	void determinePortWithSslReturnsDefaultSslPortWhenNoAddresses() {
		this.properties.getSsl().setEnabled(true);
		assertThat(this.properties.determinePort()).isEqualTo(5671);
	}

	@Test
	void determinePortReturnsPortPropertyWhenNoAddresses() {
		this.properties.setPort(1234);
		assertThat(this.properties.determinePort()).isEqualTo(1234);
	}

	@Test
	void determinePortReturnsDefaultAmqpPortWhenFirstAddressHasNoExplicitPort() {
		this.properties.setPort(1234);
		this.properties.setAddresses("rabbit1.example.com,rabbit2.example.com:2345");
		assertThat(this.properties.determinePort()).isEqualTo(5672);
	}

	@Test
	void determinePortUsingAmqpReturnsPortOfFirstAddress() {
		this.properties.setAddresses("amqp://root:password@otherhost,amqps://root:password2@otherhost2");
		assertThat(this.properties.determinePort()).isEqualTo(5672);
	}

	@Test
	void determinePortUsingAmqpsReturnsPortOfFirstAddress() {
		this.properties.setAddresses("amqps://root:password@otherhost,amqp://root:password2@otherhost2");
		assertThat(this.properties.determinePort()).isEqualTo(5671);
	}

	@Test
	void determinePortReturnsDefaultAmqpsPortWhenFirstAddressHasNoExplicitPortButSslEnabled() {
		this.properties.getSsl().setEnabled(true);
		this.properties.setPort(1234);
		this.properties.setAddresses("rabbit1.example.com,rabbit2.example.com:2345");
		assertThat(this.properties.determinePort()).isEqualTo(5671);
	}

	@Test
	void virtualHostDefaultsToNull() {
		assertThat(this.properties.getVirtualHost()).isNull();
	}

	@Test
	void customVirtualHost() {
		this.properties.setVirtualHost("alpha");
		assertThat(this.properties.getVirtualHost()).isEqualTo("alpha");
	}

	@Test
	void virtualHostRetainsALeadingSlash() {
		this.properties.setVirtualHost("/alpha");
		assertThat(this.properties.getVirtualHost()).isEqualTo("/alpha");
	}

	@Test
	void determineVirtualHostReturnsVirtualHostOfFirstAddress() {
		this.properties.setAddresses("rabbit1.example.com:1234/alpha,rabbit2.example.com:2345/bravo");
		assertThat(this.properties.determineVirtualHost()).isEqualTo("alpha");
	}

	@Test
	void determineVirtualHostReturnsPropertyWhenNoAddresses() {
		this.properties.setVirtualHost("alpha");
		assertThat(this.properties.determineVirtualHost()).isEqualTo("alpha");
	}

	@Test
	void determineVirtualHostReturnsPropertyWhenFirstAddressHasNoVirtualHost() {
		this.properties.setVirtualHost("alpha");
		this.properties.setAddresses("rabbit1.example.com:1234,rabbit2.example.com:2345/bravo");
		assertThat(this.properties.determineVirtualHost()).isEqualTo("alpha");
	}

	@Test
	void determineVirtualHostIsSlashWhenAddressHasTrailingSlash() {
		this.properties.setAddresses("amqp://root:password@otherhost:1111/");
		assertThat(this.properties.determineVirtualHost()).isEqualTo("/");
	}

	@Test
	void emptyVirtualHostIsCoercedToASlash() {
		this.properties.setVirtualHost("");
		assertThat(this.properties.getVirtualHost()).isEqualTo("/");
	}

	@Test
	void usernameDefaultsToGuest() {
		assertThat(this.properties.getUsername()).isEqualTo("guest");
	}

	@Test
	void customUsername() {
		this.properties.setUsername("user");
		assertThat(this.properties.getUsername()).isEqualTo("user");
	}

	@Test
	void determineUsernameReturnsUsernameOfFirstAddress() {
		this.properties.setAddresses("user:secret@rabbit1.example.com:1234/alpha,rabbit2.example.com:2345/bravo");
		assertThat(this.properties.determineUsername()).isEqualTo("user");
	}

	@Test
	void determineUsernameReturnsPropertyWhenNoAddresses() {
		this.properties.setUsername("alice");
		assertThat(this.properties.determineUsername()).isEqualTo("alice");
	}

	@Test
	void determineUsernameReturnsPropertyWhenFirstAddressHasNoUsername() {
		this.properties.setUsername("alice");
		this.properties.setAddresses("rabbit1.example.com:1234/alpha,user:secret@rabbit2.example.com:2345/bravo");
		assertThat(this.properties.determineUsername()).isEqualTo("alice");
	}

	@Test
	void passwordDefaultsToGuest() {
		assertThat(this.properties.getPassword()).isEqualTo("guest");
	}

	@Test
	void customPassword() {
		this.properties.setPassword("secret");
		assertThat(this.properties.getPassword()).isEqualTo("secret");
	}

	@Test
	void determinePasswordReturnsPasswordOfFirstAddress() {
		this.properties.setAddresses("user:secret@rabbit1.example.com:1234/alpha,rabbit2.example.com:2345/bravo");
		assertThat(this.properties.determinePassword()).isEqualTo("secret");
	}

	@Test
	void determinePasswordReturnsPropertyWhenNoAddresses() {
		this.properties.setPassword("secret");
		assertThat(this.properties.determinePassword()).isEqualTo("secret");
	}

	@Test
	void determinePasswordReturnsPropertyWhenFirstAddressHasNoPassword() {
		this.properties.setPassword("12345678");
		this.properties.setAddresses("rabbit1.example.com:1234/alpha,user:secret@rabbit2.example.com:2345/bravo");
		assertThat(this.properties.determinePassword()).isEqualTo("12345678");
	}

	@Test
	void addressesDefaultsToNull() {
		assertThat(this.properties.getAddresses()).isNull();
	}

	@Test
	void customAddresses() {
		this.properties.setAddresses("user:secret@rabbit1.example.com:1234/alpha,rabbit2.example.com");
		assertThat(this.properties.getAddresses())
			.isEqualTo("user:secret@rabbit1.example.com:1234/alpha,rabbit2.example.com");
	}

	@Test
	void ipv6Address() {
		this.properties.setAddresses("amqp://foo:bar@[aaaa:bbbb:cccc::d]:1234");
		assertThat(this.properties.determineHost()).isEqualTo("[aaaa:bbbb:cccc::d]");
		assertThat(this.properties.determinePort()).isEqualTo(1234);
	}

	@Test
	void ipv6AddressDefaultPort() {
		this.properties.setAddresses("amqp://foo:bar@[aaaa:bbbb:cccc::d]");
		assertThat(this.properties.determineHost()).isEqualTo("[aaaa:bbbb:cccc::d]");
		assertThat(this.properties.determinePort()).isEqualTo(5672);
	}

	@Test
	void determineAddressesReturnsAddressesWithJustHostAndPort() {
		this.properties.setAddresses("user:secret@rabbit1.example.com:1234/alpha,rabbit2.example.com");
		assertThat(this.properties.determineAddresses()).isEqualTo("rabbit1.example.com:1234,rabbit2.example.com:5672");
	}

	@Test
	void determineAddressesUsesDefaultWhenNoAddressesSet() {
		assertThat(this.properties.determineAddresses()).isEqualTo("localhost:5672");
	}

	@Test
	void determineAddressesWithSslUsesDefaultWhenNoAddressesSet() {
		this.properties.getSsl().setEnabled(true);
		assertThat(this.properties.determineAddresses()).isEqualTo("localhost:5671");
	}

	@Test
	void determineAddressesUsesHostAndPortPropertiesWhenNoAddressesSet() {
		this.properties.setHost("rabbit.example.com");
		this.properties.setPort(1234);
		assertThat(this.properties.determineAddresses()).isEqualTo("rabbit.example.com:1234");
	}

	@Test
	void determineAddressesUsesIpv6HostAndPortPropertiesWhenNoAddressesSet() {
		this.properties.setHost("[::1]");
		this.properties.setPort(32863);
		assertThat(this.properties.determineAddresses()).isEqualTo("[::1]:32863");
	}

	@Test
	void determineSslUsingAmqpsReturnsStateOfFirstAddress() {
		this.properties.setAddresses("amqps://root:password@otherhost,amqp://root:password2@otherhost2");
		assertThat(this.properties.getSsl().determineEnabled()).isTrue();
	}

	@Test
	void sslDetermineEnabledIsTrueWhenAddressHasNoProtocolAndSslIsEnabled() {
		this.properties.getSsl().setEnabled(true);
		this.properties.setAddresses("root:password@otherhost");
		assertThat(this.properties.getSsl().determineEnabled()).isTrue();
	}

	@Test
	void sslDetermineEnabledIsFalseWhenAddressHasNoProtocolAndSslIsDisabled() {
		this.properties.getSsl().setEnabled(false);
		this.properties.setAddresses("root:password@otherhost");
		assertThat(this.properties.getSsl().determineEnabled()).isFalse();
	}

	@Test
	void determineSslUsingAmqpReturnsStateOfFirstAddress() {
		this.properties.setAddresses("amqp://root:password@otherhost,amqps://root:password2@otherhost2");
		assertThat(this.properties.getSsl().determineEnabled()).isFalse();
	}

	@Test
	void determineSslReturnFlagPropertyWhenNoAddresses() {
		this.properties.getSsl().setEnabled(true);
		assertThat(this.properties.getSsl().determineEnabled()).isTrue();
	}

	@Test
	void determineSslEnabledIsTrueWhenBundleIsSetAndNoAddresses() {
		this.properties.getSsl().setBundle("test");
		assertThat(this.properties.getSsl().determineEnabled()).isTrue();
	}

	@Test
	void propertiesUseConsistentDefaultValues() {
		ConnectionFactory connectionFactory = new ConnectionFactory();
		assertThat(connectionFactory).hasFieldOrPropertyWithValue("maxInboundMessageBodySize",
				(int) this.properties.getMaxInboundMessageBodySize().toBytes());
	}

	@Test
	void simpleContainerUseConsistentDefaultValues() {
		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		SimpleMessageListenerContainer container = factory.createListenerContainer();
		RabbitProperties.SimpleContainer simple = this.properties.getListener().getSimple();
		assertThat(simple.isAutoStartup()).isEqualTo(container.isAutoStartup());
		assertThat(container).hasFieldOrPropertyWithValue("missingQueuesFatal", simple.isMissingQueuesFatal());
		assertThat(container).hasFieldOrPropertyWithValue("deBatchingEnabled", simple.isDeBatchingEnabled());
		assertThat(container).hasFieldOrPropertyWithValue("consumerBatchEnabled", simple.isConsumerBatchEnabled());
		assertThat(container).hasFieldOrPropertyWithValue("forceStop", simple.isForceStop());
	}

	@Test
	void directContainerUseConsistentDefaultValues() {
		DirectRabbitListenerContainerFactory factory = new DirectRabbitListenerContainerFactory();
		DirectMessageListenerContainer container = factory.createListenerContainer();
		RabbitProperties.DirectContainer direct = this.properties.getListener().getDirect();
		assertThat(direct.isAutoStartup()).isEqualTo(container.isAutoStartup());
		assertThat(container).hasFieldOrPropertyWithValue("missingQueuesFatal", direct.isMissingQueuesFatal());
		assertThat(container).hasFieldOrPropertyWithValue("deBatchingEnabled", direct.isDeBatchingEnabled());
		assertThat(container).hasFieldOrPropertyWithValue("forceStop", direct.isForceStop());
	}

	@Test
	void determineUsernameWithoutPassword() {
		this.properties.setAddresses("user@rabbit1.example.com:1234/alpha");
		assertThat(this.properties.determineUsername()).isEqualTo("user");
		assertThat(this.properties.determinePassword()).isEqualTo("guest");
	}

	@Test
	void hostPropertyMustBeSingleHost() {
		this.properties.setHost("my-rmq-host.net,my-rmq-host-2.net");
		assertThat(this.properties.getHost()).isEqualTo("my-rmq-host.net,my-rmq-host-2.net");
		assertThatExceptionOfType(InvalidConfigurationPropertyValueException.class)
			.isThrownBy(this.properties::determineAddresses)
			.withMessageContaining("spring.rabbitmq.host");
	}

}
