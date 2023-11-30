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

package org.springframework.boot.autoconfigure.web.servlet;

import java.util.Collections;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProvider;
import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProviders;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.AbstractView;
import org.springframework.web.servlet.view.InternalResourceView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link WelcomePageHandlerMapping}.
 *
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 */
@ExtendWith(OutputCaptureExtension.class)
class WelcomePageHandlerMappingTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withUserConfiguration(HandlerMappingConfiguration.class)
		.withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class));

	@Test
	void isOrderedAtLowPriority() {
		this.contextRunner.withUserConfiguration(StaticResourceConfiguration.class).run((context) -> {
			WelcomePageHandlerMapping handler = context.getBean(WelcomePageHandlerMapping.class);
			assertThat(handler.getOrder()).isEqualTo(2);
		});
	}

	@Test
	void handlesRequestForStaticPageThatAcceptsTextHtml() {
		this.contextRunner.withUserConfiguration(StaticResourceConfiguration.class)
			.run((context) -> MockMvcBuilders.webAppContextSetup(context)
				.build()
				.perform(get("/").accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("index.html")));
	}

	@Test
	void handlesRequestForStaticPageThatAcceptsAll() {
		this.contextRunner.withUserConfiguration(StaticResourceConfiguration.class)
			.run((context) -> MockMvcBuilders.webAppContextSetup(context)
				.build()
				.perform(get("/").accept(MediaType.ALL))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("index.html")));
	}

	@Test
	void doesNotHandleRequestThatDoesNotAcceptTextHtml() {
		this.contextRunner.withUserConfiguration(StaticResourceConfiguration.class)
			.run((context) -> MockMvcBuilders.webAppContextSetup(context)
				.build()
				.perform(get("/").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound()));
	}

	@Test
	void handlesRequestWithNoAcceptHeader() {
		this.contextRunner.withUserConfiguration(StaticResourceConfiguration.class)
			.run((context) -> MockMvcBuilders.webAppContextSetup(context)
				.build()
				.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("index.html")));
	}

	@Test
	void handlesRequestWithEmptyAcceptHeader() {
		this.contextRunner.withUserConfiguration(StaticResourceConfiguration.class)
			.run((context) -> MockMvcBuilders.webAppContextSetup(context)
				.build()
				.perform(get("/").header(HttpHeaders.ACCEPT, ""))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("index.html")));
	}

	@Test
	void rootHandlerIsNotRegisteredWhenStaticPathPatternIsNotSlashStarStar() {
		this.contextRunner.withUserConfiguration(StaticResourceConfiguration.class)
			.withPropertyValues("static-path-pattern=/foo/**")
			.run((context) -> assertThat(context.getBean(WelcomePageHandlerMapping.class).getRootHandler()).isNull());
	}

	@Test
	void producesNotFoundResponseWhenThereIsNoWelcomePage() {
		this.contextRunner.run((context) -> MockMvcBuilders.webAppContextSetup(context)
			.build()
			.perform(get("/").accept(MediaType.TEXT_HTML))
			.andExpect(status().isNotFound()));
	}

	@Test
	void handlesRequestForTemplateThatAcceptsTextHtml() {
		this.contextRunner.withUserConfiguration(TemplateConfiguration.class).run((context) -> {
			MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
			mockMvc.perform(get("/").accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(content().string("index template"));
		});
	}

	@Test
	void handlesRequestForTemplateThatAcceptsAll() {
		this.contextRunner.withUserConfiguration(TemplateConfiguration.class).run((context) -> {
			MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
			mockMvc.perform(get("/").accept(MediaType.ALL))
				.andExpect(status().isOk())
				.andExpect(content().string("index template"));
		});
	}

	@Test
	void prefersAStaticResourceToATemplate() {
		this.contextRunner.withUserConfiguration(StaticResourceConfiguration.class, TemplateConfiguration.class)
			.run((context) -> {
				MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
				mockMvc.perform(get("/").accept(MediaType.ALL))
					.andExpect(status().isOk())
					.andExpect(forwardedUrl("index.html"));
			});
	}

	@Test
	void logsInvalidAcceptHeader(CapturedOutput output) {
		this.contextRunner.withUserConfiguration(TemplateConfiguration.class).run((context) -> {
			MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
			mockMvc.perform(get("/").accept("*/*q=0.8"))
				.andExpect(status().isOk())
				.andExpect(content().string("index template"));
		});
		assertThat(output).contains("Received invalid Accept header. Assuming all media types are accepted");
	}

	@Configuration(proxyBeanMethods = false)
	static class HandlerMappingConfiguration {

		@Bean
		WelcomePageHandlerMapping handlerMapping(ApplicationContext applicationContext,
				ObjectProvider<TemplateAvailabilityProviders> templateAvailabilityProviders,
				ObjectProvider<Resource> staticIndexPage,
				@Value("${static-path-pattern:/**}") String staticPathPattern) {
			return new WelcomePageHandlerMapping(
					templateAvailabilityProviders
						.getIfAvailable(() -> new TemplateAvailabilityProviders(applicationContext)),
					applicationContext, staticIndexPage.getIfAvailable(), staticPathPattern);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class StaticResourceConfiguration {

		@Bean
		Resource staticIndexPage() {
			return new FileSystemResource("src/test/resources/welcome-page/index.html");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TemplateConfiguration {

		@Bean
		TemplateAvailabilityProviders templateAvailabilityProviders() {
			return new TestTemplateAvailabilityProviders(
					(view, environment, classLoader, resourceLoader) -> view.equals("index"));
		}

		@Bean
		ViewResolver viewResolver() {
			return (name, locale) -> {
				if (name.startsWith("forward:")) {
					return new InternalResourceView(name.substring("forward:".length()));
				}
				return new AbstractView() {

					@Override
					protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
							HttpServletResponse response) throws Exception {
						response.getWriter().print(name + " template");
					}

				};
			};
		}

	}

	static class TestTemplateAvailabilityProviders extends TemplateAvailabilityProviders {

		TestTemplateAvailabilityProviders(TemplateAvailabilityProvider provider) {
			super(Collections.singletonList(provider));
		}

	}

}
