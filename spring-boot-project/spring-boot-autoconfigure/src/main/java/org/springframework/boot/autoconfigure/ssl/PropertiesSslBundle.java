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

import java.io.IOException;
import java.io.UncheckedIOException;

import org.springframework.boot.autoconfigure.ssl.SslBundleProperties.Key;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleKey;
import org.springframework.boot.ssl.SslManagerBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.boot.ssl.jks.JksSslStoreBundle;
import org.springframework.boot.ssl.jks.JksSslStoreDetails;
import org.springframework.boot.ssl.pem.PemSslStore;
import org.springframework.boot.ssl.pem.PemSslStoreBundle;
import org.springframework.boot.ssl.pem.PemSslStoreDetails;
import org.springframework.util.Assert;

/**
 * {@link SslBundle} backed by {@link JksSslBundleProperties} or
 * {@link PemSslBundleProperties}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @since 3.1.0
 */
public final class PropertiesSslBundle implements SslBundle {

	private final SslStoreBundle stores;

	private final SslBundleKey key;

	private final SslOptions options;

	private final String protocol;

	private final SslManagerBundle managers;

	private PropertiesSslBundle(SslStoreBundle stores, SslBundleProperties properties) {
		this.stores = stores;
		this.key = asSslKeyReference(properties.getKey());
		this.options = asSslOptions(properties.getOptions());
		this.protocol = properties.getProtocol();
		this.managers = SslManagerBundle.from(this.stores, this.key);
	}

	private static SslBundleKey asSslKeyReference(Key key) {
		return (key != null) ? SslBundleKey.of(key.getPassword(), key.getAlias()) : SslBundleKey.NONE;
	}

	private static SslOptions asSslOptions(SslBundleProperties.Options options) {
		return (options != null) ? SslOptions.of(options.getCiphers(), options.getEnabledProtocols()) : SslOptions.NONE;
	}

	@Override
	public SslStoreBundle getStores() {
		return this.stores;
	}

	@Override
	public SslBundleKey getKey() {
		return this.key;
	}

	@Override
	public SslOptions getOptions() {
		return this.options;
	}

	@Override
	public String getProtocol() {
		return this.protocol;
	}

	@Override
	public SslManagerBundle getManagers() {
		return this.managers;
	}

	/**
	 * Get an {@link SslBundle} for the given {@link PemSslBundleProperties}.
	 * @param properties the source properties
	 * @return an {@link SslBundle} instance
	 */
	public static SslBundle get(PemSslBundleProperties properties) {
		try {
			PemSslStore keyStore = getPemSslStore("keystore", properties.getKeystore());
			if (keyStore != null) {
				keyStore = keyStore.withAlias(properties.getKey().getAlias())
					.withPassword(properties.getKey().getPassword());
			}
			PemSslStore trustStore = getPemSslStore("truststore", properties.getTruststore());
			SslStoreBundle storeBundle = new PemSslStoreBundle(keyStore, trustStore);
			return new PropertiesSslBundle(storeBundle, properties);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static PemSslStore getPemSslStore(String propertyName, PemSslBundleProperties.Store properties)
			throws IOException {
		PemSslStore pemSslStore = PemSslStore.load(asPemSslStoreDetails(properties));
		if (properties.isVerifyKeys()) {
			CertificateMatcher certificateMatcher = new CertificateMatcher(pemSslStore.privateKey());
			Assert.state(certificateMatcher.matchesAny(pemSslStore.certificates()),
					"Private key in %s matches none of the certificates in the chain".formatted(propertyName));
		}
		return pemSslStore;
	}

	private static PemSslStoreDetails asPemSslStoreDetails(PemSslBundleProperties.Store properties) {
		return new PemSslStoreDetails(properties.getType(), properties.getCertificate(), properties.getPrivateKey(),
				properties.getPrivateKeyPassword());
	}

	/**
	 * Get an {@link SslBundle} for the given {@link JksSslBundleProperties}.
	 * @param properties the source properties
	 * @return an {@link SslBundle} instance
	 */
	public static SslBundle get(JksSslBundleProperties properties) {
		SslStoreBundle storeBundle = asSslStoreBundle(properties);
		return new PropertiesSslBundle(storeBundle, properties);
	}

	private static SslStoreBundle asSslStoreBundle(JksSslBundleProperties properties) {
		JksSslStoreDetails keyStoreDetails = asStoreDetails(properties.getKeystore());
		JksSslStoreDetails trustStoreDetails = asStoreDetails(properties.getTruststore());
		return new JksSslStoreBundle(keyStoreDetails, trustStoreDetails);
	}

	private static JksSslStoreDetails asStoreDetails(JksSslBundleProperties.Store properties) {
		return new JksSslStoreDetails(properties.getType(), properties.getProvider(), properties.getLocation(),
				properties.getPassword());
	}

}
