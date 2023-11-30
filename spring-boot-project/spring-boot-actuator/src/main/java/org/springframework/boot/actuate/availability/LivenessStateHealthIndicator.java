/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.actuate.availability;

import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.AvailabilityState;
import org.springframework.boot.availability.LivenessState;

/**
 * A {@link HealthIndicator} that checks the {@link LivenessState} of the application.
 *
 * @author Brian Clozel
 * @since 2.3.0
 */
public class LivenessStateHealthIndicator extends AvailabilityStateHealthIndicator {

	public LivenessStateHealthIndicator(ApplicationAvailability availability) {
		super(availability, LivenessState.class, (statusMappings) -> {
			statusMappings.add(LivenessState.CORRECT, Status.UP);
			statusMappings.add(LivenessState.BROKEN, Status.DOWN);
		});
	}

	@Override
	protected AvailabilityState getState(ApplicationAvailability applicationAvailability) {
		return applicationAvailability.getLivenessState();
	}

}
