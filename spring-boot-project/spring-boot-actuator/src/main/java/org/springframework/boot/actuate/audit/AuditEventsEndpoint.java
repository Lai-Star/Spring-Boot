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

package org.springframework.boot.actuate.audit;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.boot.actuate.endpoint.OperationResponseBody;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link Endpoint @Endpoint} to expose audit events.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@Endpoint(id = "auditevents")
public class AuditEventsEndpoint {

	private final AuditEventRepository auditEventRepository;

	public AuditEventsEndpoint(AuditEventRepository auditEventRepository) {
		Assert.notNull(auditEventRepository, "AuditEventRepository must not be null");
		this.auditEventRepository = auditEventRepository;
	}

	@ReadOperation
	public AuditEventsDescriptor events(@Nullable String principal, @Nullable OffsetDateTime after,
			@Nullable String type) {
		List<AuditEvent> events = this.auditEventRepository.find(principal, getInstant(after), type);
		return new AuditEventsDescriptor(events);
	}

	private Instant getInstant(OffsetDateTime offsetDateTime) {
		return (offsetDateTime != null) ? offsetDateTime.toInstant() : null;
	}

	/**
	 * Description of an application's {@link AuditEvent audit events}.
	 */
	public static final class AuditEventsDescriptor implements OperationResponseBody {

		private final List<AuditEvent> events;

		private AuditEventsDescriptor(List<AuditEvent> events) {
			this.events = events;
		}

		public List<AuditEvent> getEvents() {
			return this.events;
		}

	}

}
