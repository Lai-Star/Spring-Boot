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

package org.springframework.boot.devtools.restart.server;

import java.io.IOException;

import org.springframework.boot.devtools.remote.server.Handler;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.Assert;

/**
 * Adapts {@link HttpRestartServer} to a {@link Handler}.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
public class HttpRestartServerHandler implements Handler {

	private final HttpRestartServer server;

	/**
	 * Create a new {@link HttpRestartServerHandler} instance.
	 * @param server the server to adapt
	 */
	public HttpRestartServerHandler(HttpRestartServer server) {
		Assert.notNull(server, "Server must not be null");
		this.server = server;
	}

	@Override
	public void handle(ServerHttpRequest request, ServerHttpResponse response) throws IOException {
		this.server.handle(request, response);
	}

}
