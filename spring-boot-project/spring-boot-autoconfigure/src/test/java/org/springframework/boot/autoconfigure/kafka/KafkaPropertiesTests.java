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

package org.springframework.boot.autoconfigure.kafka;

import java.util.Collections;
import java.util.Map;

import org.apache.kafka.common.config.SslConfigs;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.kafka.KafkaProperties.Admin;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties.Cleanup;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties.IsolationLevel;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties.Listener;
import org.springframework.boot.context.properties.source.MutuallyExclusiveConfigurationPropertiesException;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.core.CleanupConfig;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.listener.ContainerProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link KafkaProperties}.
 *
 * @author Stephane Nicoll
 * @author Madhura Bhave
 * @author Scott Frederick
 */
class KafkaPropertiesTests {

	private final SslBundle sslBundle = mock(SslBundle.class);

	@Test
	void isolationLevelEnumConsistentWithKafkaVersion() {
		org.apache.kafka.common.IsolationLevel[] original = org.apache.kafka.common.IsolationLevel.values();
		assertThat(original).extracting(Enum::name)
			.containsExactly(IsolationLevel.READ_UNCOMMITTED.name(), IsolationLevel.READ_COMMITTED.name());
		assertThat(original).extracting("id")
			.containsExactly(IsolationLevel.READ_UNCOMMITTED.id(), IsolationLevel.READ_COMMITTED.id());
		assertThat(original).hasSameSizeAs(IsolationLevel.values());
	}

	@Test
	void adminDefaultValuesAreConsistent() {
		KafkaAdmin admin = new KafkaAdmin(Collections.emptyMap());
		Admin adminProperties = new KafkaProperties().getAdmin();
		assertThat(admin).hasFieldOrPropertyWithValue("fatalIfBrokerNotAvailable", adminProperties.isFailFast());
		assertThat(admin).hasFieldOrPropertyWithValue("modifyTopicConfigs", adminProperties.isModifyTopicConfigs());
	}

	@Test
	void listenerDefaultValuesAreConsistent() {
		ContainerProperties container = new ContainerProperties("test");
		Listener listenerProperties = new KafkaProperties().getListener();
		assertThat(listenerProperties.isMissingTopicsFatal()).isEqualTo(container.isMissingTopicsFatal());
	}

	@Test
	void sslPemConfiguration() {
		KafkaProperties properties = new KafkaProperties();
		properties.getSsl().setKeyStoreKey("-----BEGINkey");
		properties.getSsl().setTrustStoreCertificates("-----BEGINtrust");
		properties.getSsl().setKeyStoreCertificateChain("-----BEGINchain");
		Map<String, Object> consumerProperties = properties.buildConsumerProperties(null);
		assertThat(consumerProperties).containsEntry(SslConfigs.SSL_KEYSTORE_KEY_CONFIG, "-----BEGINkey");
		assertThat(consumerProperties).containsEntry(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG, "-----BEGINtrust");
		assertThat(consumerProperties).containsEntry(SslConfigs.SSL_KEYSTORE_CERTIFICATE_CHAIN_CONFIG,
				"-----BEGINchain");
	}

	@Test
	void sslBundleConfiguration() {
		KafkaProperties properties = new KafkaProperties();
		properties.getSsl().setBundle("myBundle");
		Map<String, Object> consumerProperties = properties
			.buildConsumerProperties(new DefaultSslBundleRegistry("myBundle", this.sslBundle));
		assertThat(consumerProperties).containsEntry(SslConfigs.SSL_ENGINE_FACTORY_CLASS_CONFIG,
				SslBundleSslEngineFactory.class.getName());
	}

	@Test
	void sslPropertiesWhenKeyStoreLocationAndKeySetShouldThrowException() {
		KafkaProperties properties = new KafkaProperties();
		properties.getSsl().setKeyStoreKey("-----BEGIN");
		properties.getSsl().setKeyStoreLocation(new ClassPathResource("ksLoc"));
		assertThatExceptionOfType(MutuallyExclusiveConfigurationPropertiesException.class)
			.isThrownBy(() -> properties.buildConsumerProperties(null));
	}

	@Test
	void sslPropertiesWhenTrustStoreLocationAndCertificatesSetShouldThrowException() {
		KafkaProperties properties = new KafkaProperties();
		properties.getSsl().setTrustStoreLocation(new ClassPathResource("tsLoc"));
		properties.getSsl().setTrustStoreCertificates("-----BEGIN");
		assertThatExceptionOfType(MutuallyExclusiveConfigurationPropertiesException.class)
			.isThrownBy(() -> properties.buildConsumerProperties(null));
	}

	@Test
	void sslPropertiesWhenKeyStoreLocationAndBundleSetShouldThrowException() {
		KafkaProperties properties = new KafkaProperties();
		properties.getSsl().setBundle("myBundle");
		properties.getSsl().setKeyStoreLocation(new ClassPathResource("ksLoc"));
		assertThatExceptionOfType(MutuallyExclusiveConfigurationPropertiesException.class).isThrownBy(
				() -> properties.buildConsumerProperties(new DefaultSslBundleRegistry("myBundle", this.sslBundle)));
	}

	@Test
	void sslPropertiesWhenKeyStoreKeyAndBundleSetShouldThrowException() {
		KafkaProperties properties = new KafkaProperties();
		properties.getSsl().setBundle("myBundle");
		properties.getSsl().setKeyStoreKey("-----BEGIN");
		assertThatExceptionOfType(MutuallyExclusiveConfigurationPropertiesException.class).isThrownBy(
				() -> properties.buildConsumerProperties(new DefaultSslBundleRegistry("myBundle", this.sslBundle)));
	}

	@Test
	void sslPropertiesWhenTrustStoreLocationAndBundleSetShouldThrowException() {
		KafkaProperties properties = new KafkaProperties();
		properties.getSsl().setBundle("myBundle");
		properties.getSsl().setTrustStoreLocation(new ClassPathResource("tsLoc"));
		assertThatExceptionOfType(MutuallyExclusiveConfigurationPropertiesException.class).isThrownBy(
				() -> properties.buildConsumerProperties(new DefaultSslBundleRegistry("myBundle", this.sslBundle)));
	}

	@Test
	void sslPropertiesWhenTrustStoreCertificatesAndBundleSetShouldThrowException() {
		KafkaProperties properties = new KafkaProperties();
		properties.getSsl().setBundle("myBundle");
		properties.getSsl().setTrustStoreCertificates("-----BEGIN");
		assertThatExceptionOfType(MutuallyExclusiveConfigurationPropertiesException.class).isThrownBy(
				() -> properties.buildConsumerProperties(new DefaultSslBundleRegistry("myBundle", this.sslBundle)));
	}

	@Test
	void cleanupConfigDefaultValuesAreConsistent() {
		CleanupConfig cleanupConfig = new CleanupConfig();
		Cleanup cleanup = new KafkaProperties().getStreams().getCleanup();
		assertThat(cleanup.isOnStartup()).isEqualTo(cleanupConfig.cleanupOnStart());
		assertThat(cleanup.isOnShutdown()).isEqualTo(cleanupConfig.cleanupOnStop());
	}

}
