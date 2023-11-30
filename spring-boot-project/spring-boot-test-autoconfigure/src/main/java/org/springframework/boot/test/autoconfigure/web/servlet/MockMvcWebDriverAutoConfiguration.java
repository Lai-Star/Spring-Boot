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

package org.springframework.boot.test.autoconfigure.web.servlet;

import java.util.concurrent.Executors;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.web.htmlunit.webdriver.LocalHostWebConnectionHtmlUnitDriver;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.htmlunit.webdriver.MockMvcHtmlUnitDriverBuilder;
import org.springframework.util.ClassUtils;

/**
 * Auto-configuration for Selenium {@link WebDriver} MockMVC integration.
 *
 * @author Phillip Webb
 * @since 1.4.0
 */
@AutoConfiguration(after = MockMvcAutoConfiguration.class)
@ConditionalOnClass(HtmlUnitDriver.class)
@ConditionalOnProperty(prefix = "spring.test.mockmvc.webdriver", name = "enabled", matchIfMissing = true)
public class MockMvcWebDriverAutoConfiguration {

	private static final String SECURITY_CONTEXT_EXECUTOR = "org.springframework.security.concurrent.DelegatingSecurityContextExecutor";

	@Bean
	@ConditionalOnMissingBean({ WebDriver.class, MockMvcHtmlUnitDriverBuilder.class })
	@ConditionalOnBean(MockMvc.class)
	public MockMvcHtmlUnitDriverBuilder mockMvcHtmlUnitDriverBuilder(MockMvc mockMvc, Environment environment) {
		return MockMvcHtmlUnitDriverBuilder.mockMvcSetup(mockMvc)
			.withDelegate(new LocalHostWebConnectionHtmlUnitDriver(environment, BrowserVersion.CHROME));
	}

	@Bean
	@ConditionalOnMissingBean(WebDriver.class)
	@ConditionalOnBean(MockMvcHtmlUnitDriverBuilder.class)
	public HtmlUnitDriver htmlUnitDriver(MockMvcHtmlUnitDriverBuilder builder) {
		HtmlUnitDriver driver = builder.build();
		if (ClassUtils.isPresent(SECURITY_CONTEXT_EXECUTOR, getClass().getClassLoader())) {
			driver.setExecutor(new DelegatingSecurityContextExecutor(Executors.newSingleThreadExecutor()));
		}
		return driver;
	}

}
