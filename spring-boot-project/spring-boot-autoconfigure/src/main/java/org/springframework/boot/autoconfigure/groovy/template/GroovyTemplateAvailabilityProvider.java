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

package org.springframework.boot.autoconfigure.groovy.template;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.boot.autoconfigure.template.PathBasedTemplateAvailabilityProvider;
import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProvider;
import org.springframework.boot.context.properties.bind.BindableRuntimeHintsRegistrar;
import org.springframework.util.ClassUtils;

/**
 * {@link TemplateAvailabilityProvider} that provides availability information for Groovy
 * view templates.
 *
 * @author Dave Syer
 * @since 1.1.0
 */
public class GroovyTemplateAvailabilityProvider extends PathBasedTemplateAvailabilityProvider {

	private static final String REQUIRED_CLASS_NAME = "groovy.text.TemplateEngine";

	public GroovyTemplateAvailabilityProvider() {
		super(REQUIRED_CLASS_NAME, GroovyTemplateAvailabilityProperties.class, "spring.groovy.template");
	}

	protected static final class GroovyTemplateAvailabilityProperties extends TemplateAvailabilityProperties {

		private List<String> resourceLoaderPath = new ArrayList<>(
				Arrays.asList(GroovyTemplateProperties.DEFAULT_RESOURCE_LOADER_PATH));

		GroovyTemplateAvailabilityProperties() {
			super(GroovyTemplateProperties.DEFAULT_PREFIX, GroovyTemplateProperties.DEFAULT_SUFFIX);
		}

		@Override
		protected List<String> getLoaderPath() {
			return this.resourceLoaderPath;
		}

		public List<String> getResourceLoaderPath() {
			return this.resourceLoaderPath;
		}

		public void setResourceLoaderPath(List<String> resourceLoaderPath) {
			this.resourceLoaderPath = resourceLoaderPath;
		}

	}

	static class GroovyTemplateAvailabilityRuntimeHints extends BindableRuntimeHintsRegistrar {

		GroovyTemplateAvailabilityRuntimeHints() {
			super(GroovyTemplateAvailabilityProperties.class);
		}

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			if (ClassUtils.isPresent(REQUIRED_CLASS_NAME, classLoader)) {
				super.registerHints(hints, classLoader);
			}
		}

	}

}
