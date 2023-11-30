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

package org.springframework.boot.autoconfigure.security.oauth2.server.servlet;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

/**
 * {@link Configuration @Configuration} used to map
 * {@link OAuth2AuthorizationServerProperties} to registered clients and settings.
 *
 * @author Steve Riesenberg
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OAuth2AuthorizationServerProperties.class)
class OAuth2AuthorizationServerConfiguration {

	private final OAuth2AuthorizationServerPropertiesMapper propertiesMapper;

	OAuth2AuthorizationServerConfiguration(OAuth2AuthorizationServerProperties properties) {
		this.propertiesMapper = new OAuth2AuthorizationServerPropertiesMapper(properties);
	}

	@Bean
	@ConditionalOnMissingBean
	@Conditional(RegisteredClientsConfiguredCondition.class)
	RegisteredClientRepository registeredClientRepository() {
		return new InMemoryRegisteredClientRepository(this.propertiesMapper.asRegisteredClients());
	}

	@Bean
	@ConditionalOnMissingBean
	AuthorizationServerSettings authorizationServerSettings() {
		return this.propertiesMapper.asAuthorizationServerSettings();
	}

}
