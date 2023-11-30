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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleRegistry;

/**
 * A {@link SslBundleRegistrar} that registers SSL bundles based
 * {@link SslProperties#getBundle() configuration properties}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
class SslPropertiesBundleRegistrar implements SslBundleRegistrar {

	private final SslProperties.Bundles properties;

	private final FileWatcher fileWatcher;

	SslPropertiesBundleRegistrar(SslProperties properties, FileWatcher fileWatcher) {
		this.properties = properties.getBundle();
		this.fileWatcher = fileWatcher;
	}

	@Override
	public void registerBundles(SslBundleRegistry registry) {
		registerBundles(registry, this.properties.getPem(), PropertiesSslBundle::get, this::watchedPemPaths);
		registerBundles(registry, this.properties.getJks(), PropertiesSslBundle::get, this::watchedJksPaths);
	}

	private <P extends SslBundleProperties> void registerBundles(SslBundleRegistry registry, Map<String, P> properties,
			Function<P, SslBundle> bundleFactory, Function<P, Set<Path>> watchedPaths) {
		properties.forEach((bundleName, bundleProperties) -> {
			Supplier<SslBundle> bundleSupplier = () -> bundleFactory.apply(bundleProperties);
			try {
				registry.registerBundle(bundleName, bundleSupplier.get());
				if (bundleProperties.isReloadOnUpdate()) {
					Supplier<Set<Path>> pathsSupplier = () -> watchedPaths.apply(bundleProperties);
					watchForUpdates(registry, bundleName, pathsSupplier, bundleSupplier);
				}
			}
			catch (IllegalStateException ex) {
				throw new IllegalStateException("Unable to register SSL bundle '%s'".formatted(bundleName), ex);
			}
		});
	}

	private void watchForUpdates(SslBundleRegistry registry, String bundleName, Supplier<Set<Path>> pathsSupplier,
			Supplier<SslBundle> bundleSupplier) {
		try {
			this.fileWatcher.watch(pathsSupplier.get(), () -> registry.updateBundle(bundleName, bundleSupplier.get()));
		}
		catch (RuntimeException ex) {
			throw new IllegalStateException("Unable to watch for reload on update", ex);
		}
	}

	private Set<Path> watchedJksPaths(JksSslBundleProperties properties) {
		List<BundleContentProperty> watched = new ArrayList<>();
		watched.add(new BundleContentProperty("keystore.location", properties.getKeystore().getLocation()));
		watched.add(new BundleContentProperty("truststore.location", properties.getTruststore().getLocation()));
		return watchedPaths(watched);
	}

	private Set<Path> watchedPemPaths(PemSslBundleProperties properties) {
		List<BundleContentProperty> watched = new ArrayList<>();
		watched.add(new BundleContentProperty("keystore.private-key", properties.getKeystore().getPrivateKey()));
		watched.add(new BundleContentProperty("keystore.certificate", properties.getKeystore().getCertificate()));
		watched.add(new BundleContentProperty("truststore.private-key", properties.getTruststore().getPrivateKey()));
		watched.add(new BundleContentProperty("truststore.certificate", properties.getTruststore().getCertificate()));
		return watchedPaths(watched);
	}

	private Set<Path> watchedPaths(List<BundleContentProperty> properties) {
		return properties.stream()
			.filter(BundleContentProperty::hasValue)
			.map(BundleContentProperty::toWatchPath)
			.collect(Collectors.toSet());
	}

}
