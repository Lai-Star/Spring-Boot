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

package org.springframework.boot.actuate.autoconfigure.tracing.zipkin;

import java.time.Duration;

import reactor.core.publisher.Mono;
import zipkin2.Call;
import zipkin2.Callback;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * An {@link HttpSender} which uses {@link WebClient} for HTTP communication.
 *
 * @author Stefan Bratanov
 * @author Moritz Halbritter
 */
class ZipkinWebClientSender extends HttpSender {

	private final String endpoint;

	private final WebClient webClient;

	private final Duration timeout;

	ZipkinWebClientSender(String endpoint, WebClient webClient, Duration timeout) {
		this.endpoint = endpoint;
		this.webClient = webClient;
		this.timeout = timeout;
	}

	@Override
	public HttpPostCall sendSpans(byte[] batchedEncodedSpans) {
		return new WebClientHttpPostCall(this.endpoint, batchedEncodedSpans, this.webClient, this.timeout);
	}

	private static class WebClientHttpPostCall extends HttpPostCall {

		private final String endpoint;

		private final WebClient webClient;

		private final Duration timeout;

		WebClientHttpPostCall(String endpoint, byte[] body, WebClient webClient, Duration timeout) {
			super(body);
			this.endpoint = endpoint;
			this.webClient = webClient;
			this.timeout = timeout;
		}

		@Override
		public Call<Void> clone() {
			return new WebClientHttpPostCall(this.endpoint, getUncompressedBody(), this.webClient, this.timeout);
		}

		@Override
		protected Void doExecute() {
			sendRequest().block();
			return null;
		}

		@Override
		protected void doEnqueue(Callback<Void> callback) {
			sendRequest().subscribe((entity) -> callback.onSuccess(null), callback::onError);
		}

		private Mono<ResponseEntity<Void>> sendRequest() {
			return this.webClient.post()
				.uri(this.endpoint)
				.headers(this::addDefaultHeaders)
				.bodyValue(getBody())
				.retrieve()
				.toBodilessEntity()
				.timeout(this.timeout);
		}

		private void addDefaultHeaders(HttpHeaders headers) {
			headers.addAll(getDefaultHeaders());
		}

	}

}
