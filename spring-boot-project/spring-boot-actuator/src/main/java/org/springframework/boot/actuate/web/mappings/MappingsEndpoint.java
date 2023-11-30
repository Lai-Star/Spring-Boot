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

package org.springframework.boot.actuate.web.mappings;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.actuate.endpoint.OperationResponseBody;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.context.ApplicationContext;

/**
 * {@link Endpoint @Endpoint} to expose HTTP request mappings.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@Endpoint(id = "mappings")
public class MappingsEndpoint {

	private final Collection<MappingDescriptionProvider> descriptionProviders;

	private final ApplicationContext context;

	public MappingsEndpoint(Collection<MappingDescriptionProvider> descriptionProviders, ApplicationContext context) {
		this.descriptionProviders = descriptionProviders;
		this.context = context;
	}

	@ReadOperation
	public ApplicationMappingsDescriptor mappings() {
		ApplicationContext target = this.context;
		Map<String, ContextMappingsDescriptor> contextMappings = new HashMap<>();
		while (target != null) {
			contextMappings.put(target.getId(), mappingsForContext(target));
			target = target.getParent();
		}
		return new ApplicationMappingsDescriptor(contextMappings);
	}

	private ContextMappingsDescriptor mappingsForContext(ApplicationContext applicationContext) {
		Map<String, Object> mappings = new HashMap<>();
		this.descriptionProviders.forEach(
				(provider) -> mappings.put(provider.getMappingName(), provider.describeMappings(applicationContext)));
		return new ContextMappingsDescriptor(mappings,
				(applicationContext.getParent() != null) ? applicationContext.getId() : null);
	}

	/**
	 * Description of an application's request mappings.
	 */
	public static final class ApplicationMappingsDescriptor implements OperationResponseBody {

		private final Map<String, ContextMappingsDescriptor> contextMappings;

		private ApplicationMappingsDescriptor(Map<String, ContextMappingsDescriptor> contextMappings) {
			this.contextMappings = contextMappings;
		}

		public Map<String, ContextMappingsDescriptor> getContexts() {
			return this.contextMappings;
		}

	}

	/**
	 * Description of an application context's request mappings.
	 */
	public static final class ContextMappingsDescriptor {

		private final Map<String, Object> mappings;

		private final String parentId;

		private ContextMappingsDescriptor(Map<String, Object> mappings, String parentId) {
			this.mappings = mappings;
			this.parentId = parentId;
		}

		public String getParentId() {
			return this.parentId;
		}

		public Map<String, Object> getMappings() {
			return this.mappings;
		}

	}

}
