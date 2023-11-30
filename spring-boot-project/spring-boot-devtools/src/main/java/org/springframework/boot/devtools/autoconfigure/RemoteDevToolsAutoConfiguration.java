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

package org.springframework.boot.devtools.autoconfigure;

import java.util.Collection;

import jakarta.servlet.Filter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties.Servlet;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.devtools.remote.server.AccessManager;
import org.springframework.boot.devtools.remote.server.Dispatcher;
import org.springframework.boot.devtools.remote.server.DispatcherFilter;
import org.springframework.boot.devtools.remote.server.Handler;
import org.springframework.boot.devtools.remote.server.HandlerMapper;
import org.springframework.boot.devtools.remote.server.HttpHeaderAccessManager;
import org.springframework.boot.devtools.remote.server.HttpStatusHandler;
import org.springframework.boot.devtools.remote.server.UrlHandlerMapper;
import org.springframework.boot.devtools.restart.server.DefaultSourceDirectoryUrlFilter;
import org.springframework.boot.devtools.restart.server.HttpRestartServer;
import org.springframework.boot.devtools.restart.server.HttpRestartServerHandler;
import org.springframework.boot.devtools.restart.server.SourceDirectoryUrlFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.log.LogMessage;
import org.springframework.http.server.ServerHttpRequest;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for remote development support.
 *
 * @author Phillip Webb
 * @author Rob Winch
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @since 1.3.0
 */
@AutoConfiguration(after = SecurityAutoConfiguration.class)
@Conditional(OnEnabledDevToolsCondition.class)
@ConditionalOnProperty(prefix = "spring.devtools.remote", name = "secret")
@ConditionalOnClass({ Filter.class, ServerHttpRequest.class })
@Import(RemoteDevtoolsSecurityConfiguration.class)
@EnableConfigurationProperties({ ServerProperties.class, DevToolsProperties.class })
public class RemoteDevToolsAutoConfiguration {

	private static final Log logger = LogFactory.getLog(RemoteDevToolsAutoConfiguration.class);

	private final DevToolsProperties properties;

	public RemoteDevToolsAutoConfiguration(DevToolsProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	public AccessManager remoteDevToolsAccessManager() {
		RemoteDevToolsProperties remoteProperties = this.properties.getRemote();
		return new HttpHeaderAccessManager(remoteProperties.getSecretHeaderName(), remoteProperties.getSecret());
	}

	@Bean
	public HandlerMapper remoteDevToolsHealthCheckHandlerMapper(ServerProperties serverProperties) {
		Handler handler = new HttpStatusHandler();
		Servlet servlet = serverProperties.getServlet();
		String servletContextPath = (servlet.getContextPath() != null) ? servlet.getContextPath() : "";
		return new UrlHandlerMapper(servletContextPath + this.properties.getRemote().getContextPath(), handler);
	}

	@Bean
	@ConditionalOnMissingBean
	public DispatcherFilter remoteDevToolsDispatcherFilter(AccessManager accessManager,
			Collection<HandlerMapper> mappers) {
		Dispatcher dispatcher = new Dispatcher(accessManager, mappers);
		return new DispatcherFilter(dispatcher);
	}

	/**
	 * Configuration for remote update and restarts.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(prefix = "spring.devtools.remote.restart", name = "enabled", matchIfMissing = true)
	static class RemoteRestartConfiguration {

		@Bean
		@ConditionalOnMissingBean
		SourceDirectoryUrlFilter remoteRestartSourceDirectoryUrlFilter() {
			return new DefaultSourceDirectoryUrlFilter();
		}

		@Bean
		@ConditionalOnMissingBean
		HttpRestartServer remoteRestartHttpRestartServer(SourceDirectoryUrlFilter sourceDirectoryUrlFilter) {
			return new HttpRestartServer(sourceDirectoryUrlFilter);
		}

		@Bean
		@ConditionalOnMissingBean(name = "remoteRestartHandlerMapper")
		UrlHandlerMapper remoteRestartHandlerMapper(HttpRestartServer server, ServerProperties serverProperties,
				DevToolsProperties properties) {
			Servlet servlet = serverProperties.getServlet();
			RemoteDevToolsProperties remote = properties.getRemote();
			String servletContextPath = (servlet.getContextPath() != null) ? servlet.getContextPath() : "";
			String url = servletContextPath + remote.getContextPath() + "/restart";
			logger.warn(LogMessage.format("Listening for remote restart updates on %s", url));
			Handler handler = new HttpRestartServerHandler(server);
			return new UrlHandlerMapper(url, handler);
		}

	}

}
