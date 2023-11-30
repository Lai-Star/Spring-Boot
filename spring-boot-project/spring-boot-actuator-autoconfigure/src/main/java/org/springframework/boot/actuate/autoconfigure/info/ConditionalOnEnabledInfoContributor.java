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

package org.springframework.boot.actuate.autoconfigure.info;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * {@link Conditional @Conditional} that checks whether an info contributor is enabled.
 * Matches if the value of the {@code management.info.<name>.enabled} property is
 * {@code true}. Otherwise, use the specific {@link #fallback() fallback} method.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@Conditional(OnEnabledInfoContributorCondition.class)
public @interface ConditionalOnEnabledInfoContributor {

	/**
	 * The name of the info contributor.
	 * @return the name of the info contributor
	 */
	String value();

	/**
	 * Fallback behavior when {@code management.info.<name>.enabled} has not been set.
	 * @return the fallback behavior
	 */
	InfoContributorFallback fallback() default InfoContributorFallback.USE_DEFAULTS_PROPERTY;

}
