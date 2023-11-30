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

package org.springframework.boot.autoconfigure.freemarker;

import java.util.Properties;

import org.springframework.ui.freemarker.FreeMarkerConfigurationFactory;

/**
 * Base class for shared FreeMarker configuration.
 *
 * @author Brian Clozel
 */
abstract class AbstractFreeMarkerConfiguration {

	private final FreeMarkerProperties properties;

	protected AbstractFreeMarkerConfiguration(FreeMarkerProperties properties) {
		this.properties = properties;
	}

	protected final FreeMarkerProperties getProperties() {
		return this.properties;
	}

	protected void applyProperties(FreeMarkerConfigurationFactory factory) {
		factory.setTemplateLoaderPaths(this.properties.getTemplateLoaderPath());
		factory.setPreferFileSystemAccess(this.properties.isPreferFileSystemAccess());
		factory.setDefaultEncoding(this.properties.getCharsetName());
		Properties settings = new Properties();
		settings.put("recognize_standard_file_extensions", "true");
		settings.putAll(this.properties.getSettings());
		factory.setFreemarkerSettings(settings);
	}

}
