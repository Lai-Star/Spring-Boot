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

package org.springframework.boot.autoconfigure.web.servlet.error;

import java.time.Clock;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.error.ErrorAttributeOptions.Include;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.handler.DispatcherServletWebRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ErrorMvcAutoConfiguration}.
 *
 * @author Brian Clozel
 * @author Scott Frederick
 */
@ExtendWith(OutputCaptureExtension.class)
class ErrorMvcAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(DispatcherServletAutoConfiguration.class, ErrorMvcAutoConfiguration.class));

	@Test
	void renderContainsViewWithExceptionDetails() {
		this.contextRunner.run((context) -> {
			View errorView = context.getBean("error", View.class);
			ErrorAttributes errorAttributes = context.getBean(ErrorAttributes.class);
			DispatcherServletWebRequest webRequest = createWebRequest(new IllegalStateException("Exception message"),
					false);
			errorView.render(errorAttributes.getErrorAttributes(webRequest, withAllOptions()), webRequest.getRequest(),
					webRequest.getResponse());
			assertThat(webRequest.getResponse().getContentType()).isEqualTo("text/html;charset=UTF-8");
			String responseString = ((MockHttpServletResponse) webRequest.getResponse()).getContentAsString();
			assertThat(responseString).contains(
					"<p>This application has no explicit mapping for /error, so you are seeing this as a fallback.</p>")
				.contains("<div>Exception message</div>")
				.contains("<div style='white-space:pre-wrap;'>java.lang.IllegalStateException");
		});
	}

	@Test
	void renderCanUseJavaTimeTypeAsTimestamp() { // gh-23256
		this.contextRunner.run((context) -> {
			View errorView = context.getBean("error", View.class);
			ErrorAttributes errorAttributes = context.getBean(ErrorAttributes.class);
			DispatcherServletWebRequest webRequest = createWebRequest(new IllegalStateException("Exception message"),
					false);
			Map<String, Object> attributes = errorAttributes.getErrorAttributes(webRequest, withAllOptions());
			attributes.put("timestamp", Clock.systemUTC().instant());
			errorView.render(attributes, webRequest.getRequest(), webRequest.getResponse());
			assertThat(webRequest.getResponse().getContentType()).isEqualTo("text/html;charset=UTF-8");
			String responseString = ((MockHttpServletResponse) webRequest.getResponse()).getContentAsString();
			assertThat(responseString).contains("This application has no explicit mapping for /error");
		});
	}

	@Test
	void renderWhenAlreadyCommittedLogsMessage(CapturedOutput output) {
		this.contextRunner.run((context) -> {
			View errorView = context.getBean("error", View.class);
			ErrorAttributes errorAttributes = context.getBean(ErrorAttributes.class);
			DispatcherServletWebRequest webRequest = createWebRequest(new IllegalStateException("Exception message"),
					true);
			errorView.render(errorAttributes.getErrorAttributes(webRequest, withAllOptions()), webRequest.getRequest(),
					webRequest.getResponse());
			assertThat(output).contains("Cannot render error page for request [/path] "
					+ "and exception [Exception message] as the response has "
					+ "already been committed. As a result, the response may have the wrong status code.");
		});
	}

	private DispatcherServletWebRequest createWebRequest(Exception ex, boolean committed) {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/path");
		MockHttpServletResponse response = new MockHttpServletResponse();
		DispatcherServletWebRequest webRequest = new DispatcherServletWebRequest(request, response);
		webRequest.setAttribute("jakarta.servlet.error.exception", ex, RequestAttributes.SCOPE_REQUEST);
		webRequest.setAttribute("jakarta.servlet.error.request_uri", "/path", RequestAttributes.SCOPE_REQUEST);
		response.setCommitted(committed);
		response.setOutputStreamAccessAllowed(!committed);
		response.setWriterAccessAllowed(!committed);
		return webRequest;
	}

	private ErrorAttributeOptions withAllOptions() {
		return ErrorAttributeOptions.of(Include.EXCEPTION, Include.STACK_TRACE, Include.MESSAGE,
				Include.BINDING_ERRORS);
	}

}
