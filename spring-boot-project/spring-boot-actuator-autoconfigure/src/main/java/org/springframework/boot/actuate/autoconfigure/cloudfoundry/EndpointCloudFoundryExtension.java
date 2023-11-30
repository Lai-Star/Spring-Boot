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

package org.springframework.boot.actuate.autoconfigure.cloudfoundry;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.EndpointExtension;
import org.springframework.core.annotation.AliasFor;

/**
 * Identifies a type as being a Cloud Foundry specific extension for an
 * {@link Endpoint @Endpoint}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.2.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@EndpointExtension(filter = CloudFoundryEndpointFilter.class)
public @interface EndpointCloudFoundryExtension {

	/**
	 * The class of the endpoint to provide a Cloud Foundry specific extension for.
	 * @return the class of the endpoint to extend
	 */
	@AliasFor(annotation = EndpointExtension.class, attribute = "endpoint")
	Class<?> endpoint();

}
