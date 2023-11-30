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

package org.springframework.boot.autoconfigure.session;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.session.ReactiveSessionRepository;
import org.springframework.session.config.ReactiveSessionRepositoryCustomizer;
import org.springframework.session.data.mongo.ReactiveMongoSessionRepository;
import org.springframework.session.data.mongo.config.annotation.web.reactive.ReactiveMongoWebSessionConfiguration;

/**
 * Mongo-backed reactive session configuration.
 *
 * @author Andy Wilkinson
 * @author Weix Sun
 * @author Vedran Pavic
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ ReactiveMongoOperations.class, ReactiveMongoSessionRepository.class })
@ConditionalOnMissingBean(ReactiveSessionRepository.class)
@ConditionalOnBean(ReactiveMongoOperations.class)
@EnableConfigurationProperties(MongoSessionProperties.class)
@Import(ReactiveMongoWebSessionConfiguration.class)
class MongoReactiveSessionConfiguration {

	@Bean
	ReactiveSessionRepositoryCustomizer<ReactiveMongoSessionRepository> springBootSessionRepositoryCustomizer(
			SessionProperties sessionProperties, MongoSessionProperties mongoSessionProperties,
			ServerProperties serverProperties) {
		return (sessionRepository) -> {
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(sessionProperties.determineTimeout(() -> serverProperties.getReactive().getSession().getTimeout()))
				.to(sessionRepository::setDefaultMaxInactiveInterval);
			map.from(mongoSessionProperties::getCollectionName).to(sessionRepository::setCollectionName);
		};
	}

}
