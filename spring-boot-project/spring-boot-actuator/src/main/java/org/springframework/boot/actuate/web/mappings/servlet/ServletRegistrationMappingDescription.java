/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.web.mappings.servlet;

import java.util.Collection;

import jakarta.servlet.ServletRegistration;

/**
 * A mapping description derived from a {@link ServletRegistration}.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class ServletRegistrationMappingDescription extends RegistrationMappingDescription<ServletRegistration> {

	/**
	 * Creates a new {@code ServletRegistrationMappingDescription} derived from the given
	 * {@code servletRegistration}.
	 * @param servletRegistration the servlet registration
	 */
	public ServletRegistrationMappingDescription(ServletRegistration servletRegistration) {
		super(servletRegistration);
	}

	/**
	 * Returns the mappings for the registered servlet.
	 * @return the mappings
	 */
	public Collection<String> getMappings() {
		return getRegistration().getMappings();
	}

}
