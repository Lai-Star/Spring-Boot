/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.web;

import org.springframework.boot.actuate.endpoint.Producible;
import org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension;
import org.springframework.util.MimeType;

/**
 * A {@code WebEndpointResponse} can be returned by an operation on a
 * {@link EndpointWebExtension @EndpointWebExtension} to provide additional, web-specific
 * information such as the HTTP status code.
 *
 * @param <T> the type of the response body
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Vedran Pavic
 * @since 2.0.0
 */
public final class WebEndpointResponse<T> {

	/**
	 * {@code 200 OK}.
	 */
	public static final int STATUS_OK = 200;

	/**
	 * {@code 204 No Content}.
	 */
	public static final int STATUS_NO_CONTENT = 204;

	/**
	 * {@code 400 Bad Request}.
	 */
	public static final int STATUS_BAD_REQUEST = 400;

	/**
	 * {@code 404 Not Found}.
	 */
	public static final int STATUS_NOT_FOUND = 404;

	/**
	 * {@code 429 Too Many Requests}.
	 */
	public static final int STATUS_TOO_MANY_REQUESTS = 429;

	/**
	 * {@code 500 Internal Server Error}.
	 */
	public static final int STATUS_INTERNAL_SERVER_ERROR = 500;

	/**
	 * {@code 503 Service Unavailable}.
	 */
	public static final int STATUS_SERVICE_UNAVAILABLE = 503;

	private final T body;

	private final int status;

	private final MimeType contentType;

	/**
	 * Creates a new {@code WebEndpointResponse} with no body and a 200 (OK) status.
	 */
	public WebEndpointResponse() {
		this(null);
	}

	/**
	 * Creates a new {@code WebEndpointResponse} with no body and the given
	 * {@code status}.
	 * @param status the HTTP status
	 */
	public WebEndpointResponse(int status) {
		this(null, status);
	}

	/**
	 * Creates a new {@code WebEndpointResponse} with the given body and a 200 (OK)
	 * status.
	 * @param body the body
	 */
	public WebEndpointResponse(T body) {
		this(body, STATUS_OK);
	}

	/**
	 * Creates a new {@code WebEndpointResponse} with the given body and content type and
	 * a 200 (OK) status.
	 * @param body the body
	 * @param producible the producible providing the content type
	 * @since 2.5.0
	 */
	public WebEndpointResponse(T body, Producible<?> producible) {
		this(body, STATUS_OK, producible.getProducedMimeType());
	}

	/**
	 * Creates a new {@code WebEndpointResponse} with the given body and content type and
	 * a 200 (OK) status.
	 * @param body the body
	 * @param contentType the content type of the response
	 * @since 2.5.0
	 */
	public WebEndpointResponse(T body, MimeType contentType) {
		this(body, STATUS_OK, contentType);
	}

	/**
	 * Creates a new {@code WebEndpointResponse} with the given body and status.
	 * @param body the body
	 * @param status the HTTP status
	 */
	public WebEndpointResponse(T body, int status) {
		this(body, status, null);
	}

	/**
	 * Creates a new {@code WebEndpointResponse} with the given body and status.
	 * @param body the body
	 * @param status the HTTP status
	 * @param contentType the content type of the response
	 * @since 2.5.0
	 */
	public WebEndpointResponse(T body, int status, MimeType contentType) {
		this.body = body;
		this.status = status;
		this.contentType = contentType;
	}

	/**
	 * Returns the content type of the response.
	 * @return the content type;
	 */
	public MimeType getContentType() {
		return this.contentType;
	}

	/**
	 * Returns the body for the response.
	 * @return the body
	 */
	public T getBody() {
		return this.body;
	}

	/**
	 * Returns the status for the response.
	 * @return the status
	 */
	public int getStatus() {
		return this.status;
	}

}
