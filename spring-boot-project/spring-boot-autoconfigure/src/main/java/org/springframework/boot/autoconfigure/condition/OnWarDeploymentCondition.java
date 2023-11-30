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

package org.springframework.boot.autoconfigure.condition;

import jakarta.servlet.ServletContext;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.web.context.WebApplicationContext;

/**
 * {@link Condition} that checks if the application is running as a traditional war
 * deployment.
 *
 * @author Madhura Bhave
 * @see ConditionalOnWarDeployment
 * @see ConditionalOnNotWarDeployment
 */
class OnWarDeploymentCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		boolean required = metadata.isAnnotated(ConditionalOnWarDeployment.class.getName());
		ResourceLoader resourceLoader = context.getResourceLoader();
		if (resourceLoader instanceof WebApplicationContext applicationContext) {
			ServletContext servletContext = applicationContext.getServletContext();
			if (servletContext != null) {
				return new ConditionOutcome(required, "Application is deployed as a WAR file.");
			}
		}
		return new ConditionOutcome(!required, ConditionMessage.forCondition(ConditionalOnWarDeployment.class)
			.because("the application is not deployed as a WAR file."));
	}

}
