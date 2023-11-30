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

package org.springframework.boot.actuate.autoconfigure.session;

import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.session.SessionsEndpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link SessionsEndpoint}.
 *
 * @author Vedran Pavic
 * @since 2.0.0
 */
@AutoConfiguration(after = SessionAutoConfiguration.class)
@ConditionalOnClass(FindByIndexNameSessionRepository.class)
@ConditionalOnAvailableEndpoint(endpoint = SessionsEndpoint.class)
public class SessionsEndpointAutoConfiguration {

	@Bean
	@ConditionalOnBean(FindByIndexNameSessionRepository.class)
	@ConditionalOnMissingBean
	public SessionsEndpoint sessionEndpoint(FindByIndexNameSessionRepository<? extends Session> sessionRepository) {
		return new SessionsEndpoint(sessionRepository);
	}

}
