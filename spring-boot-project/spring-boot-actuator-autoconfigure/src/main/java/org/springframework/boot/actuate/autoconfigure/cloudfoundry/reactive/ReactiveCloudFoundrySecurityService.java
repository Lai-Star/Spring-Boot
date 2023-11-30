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

package org.springframework.boot.actuate.autoconfigure.cloudfoundry.reactive;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.http.Http11SslContextSpec;
import reactor.netty.http.client.HttpClient;

import org.springframework.boot.actuate.autoconfigure.cloudfoundry.AccessLevel;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.CloudFoundryAuthorizationException;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.CloudFoundryAuthorizationException.Reason;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Reactive Cloud Foundry security service to handle REST calls to the cloud controller
 * and UAA.
 *
 * @author Madhura Bhave
 */
class ReactiveCloudFoundrySecurityService {

	private static final ParameterizedTypeReference<Map<String, Object>> STRING_OBJECT_MAP = new ParameterizedTypeReference<>() {
	};

	private final WebClient webClient;

	private final String cloudControllerUrl;

	ReactiveCloudFoundrySecurityService(WebClient.Builder webClientBuilder, String cloudControllerUrl,
			boolean skipSslValidation) {
		Assert.notNull(webClientBuilder, "WebClient must not be null");
		Assert.notNull(cloudControllerUrl, "CloudControllerUrl must not be null");
		if (skipSslValidation) {
			webClientBuilder.clientConnector(buildTrustAllSslConnector());
		}
		this.webClient = webClientBuilder.build();
		this.cloudControllerUrl = cloudControllerUrl;
	}

	protected ReactorClientHttpConnector buildTrustAllSslConnector() {
		HttpClient client = HttpClient.create().secure((spec) -> spec.sslContext(createSslContextSpec()));
		return new ReactorClientHttpConnector(client);
	}

	private Http11SslContextSpec createSslContextSpec() {
		return Http11SslContextSpec.forClient()
			.configure((builder) -> builder.sslProvider(SslProvider.JDK)
				.trustManager(InsecureTrustManagerFactory.INSTANCE));
	}

	/**
	 * Return a Mono of the access level that should be granted to the given token.
	 * @param token the token
	 * @param applicationId the cloud foundry application ID
	 * @return a Mono of the access level that should be granted
	 * @throws CloudFoundryAuthorizationException if the token is not authorized
	 */
	Mono<AccessLevel> getAccessLevel(String token, String applicationId) throws CloudFoundryAuthorizationException {
		String uri = getPermissionsUri(applicationId);
		return this.webClient.get()
			.uri(uri)
			.header("Authorization", "bearer " + token)
			.retrieve()
			.bodyToMono(Map.class)
			.map(this::getAccessLevel)
			.onErrorMap(this::mapError);
	}

	private Throwable mapError(Throwable throwable) {
		if (throwable instanceof WebClientResponseException webClientResponseException) {
			HttpStatusCode statusCode = webClientResponseException.getStatusCode();
			if (statusCode.equals(HttpStatus.FORBIDDEN)) {
				return new CloudFoundryAuthorizationException(Reason.ACCESS_DENIED, "Access denied");
			}
			if (statusCode.is4xxClientError()) {
				return new CloudFoundryAuthorizationException(Reason.INVALID_TOKEN, "Invalid token", throwable);
			}
		}
		return new CloudFoundryAuthorizationException(Reason.SERVICE_UNAVAILABLE, "Cloud controller not reachable");
	}

	private AccessLevel getAccessLevel(Map<?, ?> body) {
		if (Boolean.TRUE.equals(body.get("read_sensitive_data"))) {
			return AccessLevel.FULL;
		}
		return AccessLevel.RESTRICTED;
	}

	private String getPermissionsUri(String applicationId) {
		return this.cloudControllerUrl + "/v2/apps/" + applicationId + "/permissions";
	}

	/**
	 * Return a Mono of all token keys known by the UAA.
	 * @return a Mono of token keys
	 */
	Mono<Map<String, String>> fetchTokenKeys() {
		return getUaaUrl().flatMap(this::fetchTokenKeys);
	}

	private Mono<? extends Map<String, String>> fetchTokenKeys(String url) {
		RequestHeadersSpec<?> uri = this.webClient.get().uri(url + "/token_keys");
		return uri.retrieve()
			.bodyToMono(STRING_OBJECT_MAP)
			.map(this::extractTokenKeys)
			.onErrorMap(((ex) -> new CloudFoundryAuthorizationException(Reason.SERVICE_UNAVAILABLE, ex.getMessage())));
	}

	private Map<String, String> extractTokenKeys(Map<String, Object> response) {
		Map<String, String> tokenKeys = new HashMap<>();
		for (Object key : (List<?>) response.get("keys")) {
			Map<?, ?> tokenKey = (Map<?, ?>) key;
			tokenKeys.put((String) tokenKey.get("kid"), (String) tokenKey.get("value"));
		}
		return tokenKeys;
	}

	/**
	 * Return a Mono of URL of the UAA.
	 * @return the UAA url Mono
	 */
	Mono<String> getUaaUrl() {
		return this.webClient.get()
			.uri(this.cloudControllerUrl + "/info")
			.retrieve()
			.bodyToMono(Map.class)
			.map((response) -> (String) response.get("token_endpoint"))
			.cache()
			.onErrorMap((ex) -> new CloudFoundryAuthorizationException(Reason.SERVICE_UNAVAILABLE,
					"Unable to fetch token keys from UAA."));
	}

}
