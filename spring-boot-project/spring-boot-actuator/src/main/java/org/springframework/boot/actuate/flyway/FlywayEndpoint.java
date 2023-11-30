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

package org.springframework.boot.actuate.flyway;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;

import org.springframework.boot.actuate.endpoint.OperationResponseBody;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.context.ApplicationContext;

/**
 * {@link Endpoint @Endpoint} to expose flyway info.
 *
 * @author Eddú Meléndez
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Artsiom Yudovin
 * @since 2.0.0
 */
@Endpoint(id = "flyway")
public class FlywayEndpoint {

	private final ApplicationContext context;

	public FlywayEndpoint(ApplicationContext context) {
		this.context = context;
	}

	@ReadOperation
	public FlywayBeansDescriptor flywayBeans() {
		ApplicationContext target = this.context;
		Map<String, ContextFlywayBeansDescriptor> contextFlywayBeans = new HashMap<>();
		while (target != null) {
			Map<String, FlywayDescriptor> flywayBeans = new HashMap<>();
			target.getBeansOfType(Flyway.class)
				.forEach((name, flyway) -> flywayBeans.put(name, new FlywayDescriptor(flyway.info().all())));
			ApplicationContext parent = target.getParent();
			contextFlywayBeans.put(target.getId(),
					new ContextFlywayBeansDescriptor(flywayBeans, (parent != null) ? parent.getId() : null));
			target = parent;
		}
		return new FlywayBeansDescriptor(contextFlywayBeans);
	}

	/**
	 * Description of an application's {@link Flyway} beans.
	 */
	public static final class FlywayBeansDescriptor implements OperationResponseBody {

		private final Map<String, ContextFlywayBeansDescriptor> contexts;

		private FlywayBeansDescriptor(Map<String, ContextFlywayBeansDescriptor> contexts) {
			this.contexts = contexts;
		}

		public Map<String, ContextFlywayBeansDescriptor> getContexts() {
			return this.contexts;
		}

	}

	/**
	 * Description of an application context's {@link Flyway} beans.
	 */
	public static final class ContextFlywayBeansDescriptor {

		private final Map<String, FlywayDescriptor> flywayBeans;

		private final String parentId;

		private ContextFlywayBeansDescriptor(Map<String, FlywayDescriptor> flywayBeans, String parentId) {
			this.flywayBeans = flywayBeans;
			this.parentId = parentId;
		}

		public Map<String, FlywayDescriptor> getFlywayBeans() {
			return this.flywayBeans;
		}

		public String getParentId() {
			return this.parentId;
		}

	}

	/**
	 * Description of a {@link Flyway} bean.
	 */
	public static class FlywayDescriptor {

		private final List<FlywayMigrationDescriptor> migrations;

		private FlywayDescriptor(MigrationInfo[] migrations) {
			this.migrations = Stream.of(migrations).map(FlywayMigrationDescriptor::new).toList();
		}

		public FlywayDescriptor(List<FlywayMigrationDescriptor> migrations) {
			this.migrations = migrations;
		}

		public List<FlywayMigrationDescriptor> getMigrations() {
			return this.migrations;
		}

	}

	/**
	 * Description of a migration performed by Flyway.
	 */
	public static final class FlywayMigrationDescriptor {

		private final String type;

		private final Integer checksum;

		private final String version;

		private final String description;

		private final String script;

		private final MigrationState state;

		private final String installedBy;

		private final Instant installedOn;

		private final Integer installedRank;

		private final Integer executionTime;

		private FlywayMigrationDescriptor(MigrationInfo info) {
			this.type = info.getType().name();
			this.checksum = info.getChecksum();
			this.version = nullSafeToString(info.getVersion());
			this.description = info.getDescription();
			this.script = info.getScript();
			this.state = info.getState();
			this.installedBy = info.getInstalledBy();
			this.installedRank = info.getInstalledRank();
			this.executionTime = info.getExecutionTime();
			this.installedOn = nullSafeToInstant(info.getInstalledOn());
		}

		private String nullSafeToString(Object obj) {
			return (obj != null) ? obj.toString() : null;
		}

		private Instant nullSafeToInstant(Date date) {
			return (date != null) ? Instant.ofEpochMilli(date.getTime()) : null;
		}

		public String getType() {
			return this.type;
		}

		public Integer getChecksum() {
			return this.checksum;
		}

		public String getVersion() {
			return this.version;
		}

		public String getDescription() {
			return this.description;
		}

		public String getScript() {
			return this.script;
		}

		public MigrationState getState() {
			return this.state;
		}

		public String getInstalledBy() {
			return this.installedBy;
		}

		public Instant getInstalledOn() {
			return this.installedOn;
		}

		public Integer getInstalledRank() {
			return this.installedRank;
		}

		public Integer getExecutionTime() {
			return this.executionTime;
		}

	}

}
