/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.servlet;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JspTemplateAvailabilityProvider}.
 *
 * @author Yunkun Huang
 */
class JspTemplateAvailabilityProviderTests {

	private final JspTemplateAvailabilityProvider provider = new JspTemplateAvailabilityProvider();

	private final ResourceLoader resourceLoader = new DefaultResourceLoader();

	private final MockEnvironment environment = new MockEnvironment();

	@Test
	void availabilityOfTemplateThatDoesNotExist() {
		assertThat(isTemplateAvailable("whatever")).isFalse();
	}

	@Test
	void availabilityOfTemplateWithCustomPrefix() {
		this.environment.setProperty("spring.mvc.view.prefix", "classpath:/custom-templates/");
		assertThat(isTemplateAvailable("custom.jsp")).isTrue();
	}

	@Test
	void availabilityOfTemplateWithCustomSuffix() {
		this.environment.setProperty("spring.mvc.view.prefix", "classpath:/custom-templates/");
		this.environment.setProperty("spring.mvc.view.suffix", ".jsp");
		assertThat(isTemplateAvailable("suffixed")).isTrue();
	}

	private boolean isTemplateAvailable(String view) {
		return this.provider.isTemplateAvailable(view, this.environment, getClass().getClassLoader(),
				this.resourceLoader);
	}

}
