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

package org.springframework.boot.actuate.endpoint.web;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * A predicate for a request to an operation on a web endpoint.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public final class WebOperationRequestPredicate {

	private static final Pattern PATH_VAR_PATTERN = Pattern.compile("(\\{\\*?).+?}");

	private static final Pattern ALL_REMAINING_PATH_SEGMENTS_VAR_PATTERN = Pattern.compile("^.*\\{\\*(.+?)}$");

	private final String path;

	private final String matchAllRemainingPathSegmentsVariable;

	private final String canonicalPath;

	private final WebEndpointHttpMethod httpMethod;

	private final Collection<String> consumes;

	private final Collection<String> produces;

	/**
	 * Creates a new {@code OperationRequestPredicate}.
	 * @param path the path for the operation
	 * @param httpMethod the HTTP method that the operation supports
	 * @param produces the media types that the operation produces
	 * @param consumes the media types that the operation consumes
	 */
	public WebOperationRequestPredicate(String path, WebEndpointHttpMethod httpMethod, Collection<String> consumes,
			Collection<String> produces) {
		this.path = path;
		this.canonicalPath = extractCanonicalPath(path);
		this.matchAllRemainingPathSegmentsVariable = extractMatchAllRemainingPathSegmentsVariable(path);
		this.httpMethod = httpMethod;
		this.consumes = consumes;
		this.produces = produces;
	}

	private String extractCanonicalPath(String path) {
		Matcher matcher = PATH_VAR_PATTERN.matcher(path);
		return matcher.replaceAll("$1*}");
	}

	private String extractMatchAllRemainingPathSegmentsVariable(String path) {
		Matcher matcher = ALL_REMAINING_PATH_SEGMENTS_VAR_PATTERN.matcher(path);
		return matcher.matches() ? matcher.group(1) : null;
	}

	/**
	 * Returns the path for the operation.
	 * @return the path
	 */
	public String getPath() {
		return this.path;
	}

	/**
	 * Returns the name of the variable used to catch all remaining path segments
	 * {@code null}.
	 * @return the variable name
	 * @since 2.2.0
	 */
	public String getMatchAllRemainingPathSegmentsVariable() {
		return this.matchAllRemainingPathSegmentsVariable;
	}

	/**
	 * Returns the HTTP method for the operation.
	 * @return the HTTP method
	 */
	public WebEndpointHttpMethod getHttpMethod() {
		return this.httpMethod;
	}

	/**
	 * Returns the media types that the operation consumes.
	 * @return the consumed media types
	 */
	public Collection<String> getConsumes() {
		return Collections.unmodifiableCollection(this.consumes);
	}

	/**
	 * Returns the media types that the operation produces.
	 * @return the produced media types
	 */
	public Collection<String> getProduces() {
		return Collections.unmodifiableCollection(this.produces);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		WebOperationRequestPredicate other = (WebOperationRequestPredicate) obj;
		boolean result = true;
		result = result && this.consumes.equals(other.consumes);
		result = result && this.httpMethod == other.httpMethod;
		result = result && this.canonicalPath.equals(other.canonicalPath);
		result = result && this.produces.equals(other.produces);
		return result;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.consumes.hashCode();
		result = prime * result + this.httpMethod.hashCode();
		result = prime * result + this.canonicalPath.hashCode();
		result = prime * result + this.produces.hashCode();
		return result;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder(this.httpMethod + " to path '" + this.path + "'");
		if (!CollectionUtils.isEmpty(this.consumes)) {
			result.append(" consumes: ").append(StringUtils.collectionToCommaDelimitedString(this.consumes));
		}
		if (!CollectionUtils.isEmpty(this.produces)) {
			result.append(" produces: ").append(StringUtils.collectionToCommaDelimitedString(this.produces));
		}
		return result.toString();
	}

}
