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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import zipkin2.Callback;
import zipkin2.reporter.ClosedSenderException;
import zipkin2.reporter.Sender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Abstract base test class which is used for testing the different implementations of the
 * {@link HttpSender}.
 *
 * @author Stefan Bratanov
 */
abstract class ZipkinHttpSenderTests {

	protected Sender sender;

	abstract Sender createSender();

	@BeforeEach
	void beforeEach() throws Exception {
		this.sender = createSender();
	}

	@AfterEach
	void afterEach() throws IOException {
		this.sender.close();
	}

	@Test
	void sendSpansShouldThrowIfCloseWasCalled() throws IOException {
		this.sender.close();
		assertThatExceptionOfType(ClosedSenderException.class)
			.isThrownBy(() -> this.sender.sendSpans(Collections.emptyList()));
	}

	protected void makeRequest(List<byte[]> encodedSpans, boolean async) throws IOException {
		if (async) {
			CallbackResult callbackResult = makeAsyncRequest(encodedSpans);
			assertThat(callbackResult.success()).isTrue();
		}
		else {
			makeSyncRequest(encodedSpans);
		}
	}

	protected CallbackResult makeAsyncRequest(List<byte[]> encodedSpans) {
		return makeAsyncRequest(this.sender, encodedSpans);
	}

	protected CallbackResult makeAsyncRequest(Sender sender, List<byte[]> encodedSpans) {
		AtomicReference<CallbackResult> callbackResult = new AtomicReference<>();
		sender.sendSpans(encodedSpans).enqueue(new Callback<>() {
			@Override
			public void onSuccess(Void value) {
				callbackResult.set(new CallbackResult(true, null));
			}

			@Override
			public void onError(Throwable t) {
				callbackResult.set(new CallbackResult(false, t));
			}
		});
		return Awaitility.await().atMost(Duration.ofSeconds(5)).until(callbackResult::get, Objects::nonNull);
	}

	protected void makeSyncRequest(List<byte[]> encodedSpans) throws IOException {
		makeSyncRequest(this.sender, encodedSpans);
	}

	protected void makeSyncRequest(Sender sender, List<byte[]> encodedSpans) throws IOException {
		sender.sendSpans(encodedSpans).execute();
	}

	protected byte[] toByteArray(String input) {
		return input.getBytes(StandardCharsets.UTF_8);
	}

	record CallbackResult(boolean success, Throwable error) {
	}

}
