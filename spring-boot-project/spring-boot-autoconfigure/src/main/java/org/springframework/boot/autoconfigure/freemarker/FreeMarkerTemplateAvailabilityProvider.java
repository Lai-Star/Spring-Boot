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

package org.springframework.boot.autoconfigure.freemarker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.boot.autoconfigure.template.PathBasedTemplateAvailabilityProvider;
import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProvider;
import org.springframework.boot.context.properties.bind.BindableRuntimeHintsRegistrar;
import org.springframework.util.ClassUtils;

/**
 * {@link TemplateAvailabilityProvider} that provides availability information for
 * FreeMarker view templates.
 *
 * @author Andy Wilkinson
 * @since 1.1.0
 */
public class FreeMarkerTemplateAvailabilityProvider extends PathBasedTemplateAvailabilityProvider {

	private static final String REQUIRED_CLASS_NAME = "freemarker.template.Configuration";

	public FreeMarkerTemplateAvailabilityProvider() {
		super(REQUIRED_CLASS_NAME, FreeMarkerTemplateAvailabilityProperties.class, "spring.freemarker");
	}

	protected static final class FreeMarkerTemplateAvailabilityProperties extends TemplateAvailabilityProperties {

		private List<String> templateLoaderPath = new ArrayList<>(
				Arrays.asList(FreeMarkerProperties.DEFAULT_TEMPLATE_LOADER_PATH));

		FreeMarkerTemplateAvailabilityProperties() {
			super(FreeMarkerProperties.DEFAULT_PREFIX, FreeMarkerProperties.DEFAULT_SUFFIX);
		}

		@Override
		protected List<String> getLoaderPath() {
			return this.templateLoaderPath;
		}

		public List<String> getTemplateLoaderPath() {
			return this.templateLoaderPath;
		}

		public void setTemplateLoaderPath(List<String> templateLoaderPath) {
			this.templateLoaderPath = templateLoaderPath;
		}

	}

	static class FreeMarkerTemplateAvailabilityRuntimeHints extends BindableRuntimeHintsRegistrar {

		FreeMarkerTemplateAvailabilityRuntimeHints() {
			super(FreeMarkerTemplateAvailabilityProperties.class);
		}

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			if (ClassUtils.isPresent(REQUIRED_CLASS_NAME, classLoader)) {
				super.registerHints(hints, classLoader);
			}
		}

	}

}
