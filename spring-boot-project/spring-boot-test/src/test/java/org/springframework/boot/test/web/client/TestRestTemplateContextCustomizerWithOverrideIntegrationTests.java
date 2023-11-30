/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.test.web.client;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.GenericServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link TestRestTemplateContextCustomizer} with a custom
 * {@link TestRestTemplate} bean.
 *
 * @author Phillip Webb
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
class TestRestTemplateContextCustomizerWithOverrideIntegrationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void test() {
		assertThat(this.restTemplate).isInstanceOf(CustomTestRestTemplate.class);
	}

	@Configuration(proxyBeanMethods = false)
	@Import({ TestServlet.class, NoTestRestTemplateBeanChecker.class })
	static class TestConfig {

		@Bean
		TomcatServletWebServerFactory webServerFactory() {
			return new TomcatServletWebServerFactory(0);
		}

		@Bean
		TestRestTemplate template() {
			return new CustomTestRestTemplate();
		}

	}

	static class TestServlet extends GenericServlet {

		@Override
		public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
			try (PrintWriter writer = response.getWriter()) {
				writer.println("hello");
			}
		}

	}

	static class CustomTestRestTemplate extends TestRestTemplate {

	}

}
