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

package org.springframework.boot.actuate.autoconfigure.endpoint.web.documentation;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.logging.LogFileWebEndpoint;
import org.springframework.boot.logging.LogFile;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for generating documentation describing the {@link LogFileWebEndpoint}.
 *
 * @author Andy Wilkinson
 */
class LogFileWebEndpointDocumentationTests extends MockMvcEndpointDocumentationTests {

	@Test
	void logFile() throws Exception {
		this.mockMvc.perform(get("/actuator/logfile"))
			.andExpect(status().isOk())
			.andDo(MockMvcRestDocumentation.document("logfile/entire"));
	}

	@Test
	void logFileRange() throws Exception {
		this.mockMvc.perform(get("/actuator/logfile").header("Range", "bytes=0-1023"))
			.andExpect(status().isPartialContent())
			.andDo(MockMvcRestDocumentation.document("logfile/range"));
	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseDocumentationConfiguration.class)
	static class TestConfiguration {

		@Bean
		LogFileWebEndpoint endpoint() {
			MockEnvironment environment = new MockEnvironment();
			environment.setProperty("logging.file.name",
					"src/test/resources/org/springframework/boot/actuate/autoconfigure/endpoint/web/documentation/sample.log");
			return new LogFileWebEndpoint(LogFile.get(environment), null);
		}

	}

}
