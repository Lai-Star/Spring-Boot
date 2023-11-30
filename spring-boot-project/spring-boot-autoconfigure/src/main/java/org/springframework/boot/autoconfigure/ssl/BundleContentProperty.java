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

package org.springframework.boot.autoconfigure.ssl;

import java.io.FileNotFoundException;
import java.net.URL;
import java.nio.file.Path;

import org.springframework.boot.ssl.pem.PemContent;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * Helper utility to manage a single bundle content configuration property. May possibly
 * contain PEM content, a location or a directory search pattern.
 *
 * @param name the configuration property name (excluding any prefix)
 * @param value the configuration property value
 * @author Phillip Webb
 */
record BundleContentProperty(String name, String value) {

	/**
	 * Return if the property value is PEM content.
	 * @return if the value is PEM content
	 */
	boolean isPemContent() {
		return PemContent.isPresentInText(this.value);
	}

	/**
	 * Return if there is any property value present.
	 * @return if the value is present
	 */
	boolean hasValue() {
		return StringUtils.hasText(this.value);
	}

	Path toWatchPath() {
		return toPath();
	}

	private Path toPath() {
		try {
			URL url = toUrl();
			Assert.state(isFileUrl(url), () -> "Value '%s' is not a file URL".formatted(url));
			return Path.of(url.toURI()).toAbsolutePath();
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to convert value of property '%s' to a path".formatted(this.name),
					ex);
		}
	}

	private URL toUrl() throws FileNotFoundException {
		Assert.state(!isPemContent(), "Value contains PEM content");
		return ResourceUtils.getURL(this.value);
	}

	private boolean isFileUrl(URL url) {
		return "file".equalsIgnoreCase(url.getProtocol());
	}

}
