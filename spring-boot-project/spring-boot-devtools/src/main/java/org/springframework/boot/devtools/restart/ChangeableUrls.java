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

package org.springframework.boot.devtools.restart;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;

import org.springframework.boot.devtools.logger.DevToolsLogFactory;
import org.springframework.boot.devtools.settings.DevToolsSettings;
import org.springframework.core.log.LogMessage;
import org.springframework.util.StringUtils;

/**
 * A filtered collection of URLs which can change after the application has started.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
final class ChangeableUrls implements Iterable<URL> {

	private static final Log logger = DevToolsLogFactory.getLog(ChangeableUrls.class);

	private final List<URL> urls;

	private ChangeableUrls(URL... urls) {
		DevToolsSettings settings = DevToolsSettings.get();
		List<URL> reloadableUrls = new ArrayList<>(urls.length);
		for (URL url : urls) {
			if ((settings.isRestartInclude(url) || isDirectoryUrl(url.toString())) && !settings.isRestartExclude(url)) {
				reloadableUrls.add(url);
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Matching URLs for reloading : " + reloadableUrls);
		}
		this.urls = Collections.unmodifiableList(reloadableUrls);
	}

	private boolean isDirectoryUrl(String urlString) {
		return urlString.startsWith("file:") && urlString.endsWith("/");
	}

	@Override
	public Iterator<URL> iterator() {
		return this.urls.iterator();
	}

	int size() {
		return this.urls.size();
	}

	URL[] toArray() {
		return this.urls.toArray(new URL[0]);
	}

	List<URL> toList() {
		return Collections.unmodifiableList(this.urls);
	}

	@Override
	public String toString() {
		return this.urls.toString();
	}

	static ChangeableUrls fromClassLoader(ClassLoader classLoader) {
		List<URL> urls = new ArrayList<>();
		for (URL url : urlsFromClassLoader(classLoader)) {
			urls.add(url);
			urls.addAll(getUrlsFromClassPathOfJarManifestIfPossible(url));
		}
		return fromUrls(urls);
	}

	private static URL[] urlsFromClassLoader(ClassLoader classLoader) {
		if (classLoader instanceof URLClassLoader urlClassLoader) {
			return urlClassLoader.getURLs();
		}
		return Stream.of(ManagementFactory.getRuntimeMXBean().getClassPath().split(File.pathSeparator))
			.map(ChangeableUrls::toURL)
			.toArray(URL[]::new);
	}

	private static URL toURL(String classPathEntry) {
		try {
			return new File(classPathEntry).toURI().toURL();
		}
		catch (MalformedURLException ex) {
			throw new IllegalArgumentException("URL could not be created from '" + classPathEntry + "'", ex);
		}
	}

	private static List<URL> getUrlsFromClassPathOfJarManifestIfPossible(URL url) {
		try {
			File file = new File(url.toURI());
			if (file.isFile()) {
				try (JarFile jarFile = new JarFile(file)) {
					try {
						return getUrlsFromManifestClassPathAttribute(url, jarFile);
					}
					catch (IOException ex) {
						throw new IllegalStateException(
								"Failed to read Class-Path attribute from manifest of jar " + url, ex);
					}
				}
			}
		}
		catch (Exception ex) {
			// Assume it's not a jar and continue
		}
		return Collections.emptyList();
	}

	private static List<URL> getUrlsFromManifestClassPathAttribute(URL jarUrl, JarFile jarFile) throws IOException {
		Manifest manifest = jarFile.getManifest();
		if (manifest == null) {
			return Collections.emptyList();
		}
		String classPath = manifest.getMainAttributes().getValue(Attributes.Name.CLASS_PATH);
		if (!StringUtils.hasText(classPath)) {
			return Collections.emptyList();
		}
		String[] entries = StringUtils.delimitedListToStringArray(classPath, " ");
		List<URL> urls = new ArrayList<>(entries.length);
		List<URL> nonExistentEntries = new ArrayList<>();
		for (String entry : entries) {
			try {
				URL referenced = new URL(jarUrl, entry);
				if (new File(referenced.getFile()).exists()) {
					urls.add(referenced);
				}
				else {
					referenced = new URL(jarUrl, URLDecoder.decode(entry, "UTF-8"));
					if (new File(referenced.getFile()).exists()) {
						urls.add(referenced);
					}
					else {
						nonExistentEntries.add(referenced);
					}
				}
			}
			catch (MalformedURLException ex) {
				throw new IllegalStateException("Class-Path attribute contains malformed URL", ex);
			}
		}
		if (!nonExistentEntries.isEmpty()) {
			logger.info(LogMessage.of(() -> "The Class-Path manifest attribute in " + jarFile.getName()
					+ " referenced one or more files that do not exist: "
					+ StringUtils.collectionToCommaDelimitedString(nonExistentEntries)));
		}
		return urls;
	}

	static ChangeableUrls fromUrls(Collection<URL> urls) {
		return fromUrls(new ArrayList<>(urls).toArray(new URL[urls.size()]));
	}

	static ChangeableUrls fromUrls(URL... urls) {
		return new ChangeableUrls(urls);
	}

}
