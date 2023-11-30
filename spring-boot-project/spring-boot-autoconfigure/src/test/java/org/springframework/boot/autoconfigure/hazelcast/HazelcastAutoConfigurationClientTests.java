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

package org.springframework.boot.autoconfigure.hazelcast;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.nio.file.Files;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.impl.clientside.HazelcastClientProxy;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HazelcastAutoConfiguration} specific to the client.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 */
class HazelcastAutoConfigurationClientTests {

	/**
	 * Servers the test clients will connect to.
	 */
	private static HazelcastInstance hazelcastServer;

	private static String endpointAddress;

	@BeforeAll
	static void init() {
		Config config = Config.load();
		config.getNetworkConfig().setPort(0);
		hazelcastServer = Hazelcast.newHazelcastInstance(config);
		InetSocketAddress inetSocketAddress = (InetSocketAddress) hazelcastServer.getLocalEndpoint().getSocketAddress();
		endpointAddress = inetSocketAddress.getHostString() + ":" + inetSocketAddress.getPort();
	}

	@AfterAll
	static void close() {
		if (hazelcastServer != null) {
			hazelcastServer.shutdown();
		}
	}

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(HazelcastAutoConfiguration.class));

	@Test
	void systemPropertyWithXml() {
		File config = prepareConfiguration("src/test/resources/org/springframework/"
				+ "boot/autoconfigure/hazelcast/hazelcast-client-specific.xml");
		this.contextRunner
			.withSystemProperties(HazelcastClientConfiguration.CONFIG_SYSTEM_PROPERTY + "=" + config.getAbsolutePath())
			.run(assertSpecificHazelcastClient("explicit-xml"));
	}

	@Test
	void systemPropertyWithYaml() {
		File config = prepareConfiguration("src/test/resources/org/springframework/"
				+ "boot/autoconfigure/hazelcast/hazelcast-client-specific.yaml");
		this.contextRunner
			.withSystemProperties(HazelcastClientConfiguration.CONFIG_SYSTEM_PROPERTY + "=" + config.getAbsolutePath())
			.run(assertSpecificHazelcastClient("explicit-yaml"));
	}

	@Test
	void systemPropertyWithYml() {
		File config = prepareConfiguration("src/test/resources/org/springframework/"
				+ "boot/autoconfigure/hazelcast/hazelcast-client-specific.yml");
		this.contextRunner
			.withSystemProperties(HazelcastClientConfiguration.CONFIG_SYSTEM_PROPERTY + "=" + config.getAbsolutePath())
			.run(assertSpecificHazelcastClient("explicit-yml"));
	}

	@Test
	void explicitConfigUrlWithXml() throws MalformedURLException {
		File config = prepareConfiguration("src/test/resources/org/springframework/"
				+ "boot/autoconfigure/hazelcast/hazelcast-client-specific.xml");
		this.contextRunner.withPropertyValues("spring.hazelcast.config=" + config.toURI().toURL())
			.run(assertSpecificHazelcastClient("explicit-xml"));
	}

	@Test
	void explicitConfigUrlWithYaml() throws MalformedURLException {
		File config = prepareConfiguration("src/test/resources/org/springframework/"
				+ "boot/autoconfigure/hazelcast/hazelcast-client-specific.yaml");
		this.contextRunner.withPropertyValues("spring.hazelcast.config=" + config.toURI().toURL())
			.run(assertSpecificHazelcastClient("explicit-yaml"));
	}

	@Test
	void explicitConfigUrlWithYml() throws MalformedURLException {
		File config = prepareConfiguration("src/test/resources/org/springframework/"
				+ "boot/autoconfigure/hazelcast/hazelcast-client-specific.yml");
		this.contextRunner.withPropertyValues("spring.hazelcast.config=" + config.toURI().toURL())
			.run(assertSpecificHazelcastClient("explicit-yml"));
	}

	@Test
	void unknownConfigFile() {
		this.contextRunner.withPropertyValues("spring.hazelcast.config=foo/bar/unknown.xml")
			.run((context) -> assertThat(context).getFailure()
				.isInstanceOf(BeanCreationException.class)
				.hasMessageContaining("foo/bar/unknown.xml"));
	}

	@Test
	void clientConfigTakesPrecedence() {
		this.contextRunner.withUserConfiguration(HazelcastServerAndClientConfig.class)
			.withPropertyValues("spring.hazelcast.config=this-is-ignored.xml")
			.run((context) -> assertThat(context).getBean(HazelcastInstance.class)
				.isInstanceOf(HazelcastClientProxy.class));
	}

	@Test
	void clientConfigWithInstanceNameCreatesClientIfNecessary() throws MalformedURLException {
		assertThat(HazelcastClient.getHazelcastClientByName("spring-boot")).isNull();
		File config = prepareConfiguration("src/test/resources/org/springframework/"
				+ "boot/autoconfigure/hazelcast/hazelcast-client-instance.xml");
		this.contextRunner.withPropertyValues("spring.hazelcast.config=" + config.toURI().toURL())
			.run((context) -> assertThat(context).getBean(HazelcastInstance.class)
				.extracting(HazelcastInstance::getName)
				.isEqualTo("spring-boot"));
	}

	@Test
	void autoConfiguredClientConfigUsesApplicationClassLoader() throws MalformedURLException {
		File config = prepareConfiguration("src/test/resources/org/springframework/"
				+ "boot/autoconfigure/hazelcast/hazelcast-client-specific.xml");
		this.contextRunner.withPropertyValues("spring.hazelcast.config=" + config.toURI().toURL()).run((context) -> {
			HazelcastInstance hazelcast = context.getBean(HazelcastInstance.class);
			assertThat(hazelcast).isInstanceOf(HazelcastClientProxy.class);
			ClientConfig clientConfig = ((HazelcastClientProxy) hazelcast).getClientConfig();
			assertThat(clientConfig.getClassLoader()).isSameAs(context.getSourceApplicationContext().getClassLoader());
		});
	}

	private ContextConsumer<AssertableApplicationContext> assertSpecificHazelcastClient(String label) {
		return (context) -> assertThat(context).getBean(HazelcastInstance.class)
			.isInstanceOf(HazelcastInstance.class)
			.has(labelEqualTo(label));
	}

	private static Condition<HazelcastInstance> labelEqualTo(String label) {
		return new Condition<>((o) -> ((HazelcastClientProxy) o).getClientConfig()
			.getLabels()
			.stream()
			.anyMatch((e) -> e.equals(label)), "Label equals to " + label);
	}

	private File prepareConfiguration(String input) {
		File configFile = new File(input);
		try {
			String config = FileCopyUtils.copyToString(new FileReader(configFile));
			config = config.replace("${address}", endpointAddress);
			System.out.println(config);
			File outputFile = new File(Files.createTempDirectory(getClass().getSimpleName()).toFile(),
					configFile.getName());
			FileCopyUtils.copy(config, new FileWriter(outputFile));
			return outputFile;
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class HazelcastServerAndClientConfig {

		@Bean
		Config config() {
			return new Config();
		}

		@Bean
		ClientConfig clientConfig() {
			ClientConfig config = new ClientConfig();
			config.getConnectionStrategyConfig().getConnectionRetryConfig().setClusterConnectTimeoutMillis(60000);
			config.getNetworkConfig().getAddresses().add(endpointAddress);
			return config;
		}

	}

}
