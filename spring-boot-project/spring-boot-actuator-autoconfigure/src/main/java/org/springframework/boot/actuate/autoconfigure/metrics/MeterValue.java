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

package org.springframework.boot.actuate.autoconfigure.metrics;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.Meter.Type;

import org.springframework.boot.convert.DurationStyle;

/**
 * A meter value that is used when configuring micrometer. Can be a String representation
 * of either a {@link Double} (applicable to timers and distribution summaries) or a
 * {@link Duration} (applicable to only timers).
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 2.2.0
 */
public final class MeterValue {

	private final Object value;

	MeterValue(double value) {
		this.value = value;
	}

	MeterValue(Duration value) {
		this.value = value;
	}

	/**
	 * Return the underlying value in form suitable to apply to the given meter type.
	 * @param meterType the meter type
	 * @return the value or {@code null} if the value cannot be applied
	 */
	public Double getValue(Type meterType) {
		if (meterType == Type.DISTRIBUTION_SUMMARY) {
			return getDistributionSummaryValue();
		}
		if (meterType == Type.TIMER) {
			Long timerValue = getTimerValue();
			if (timerValue != null) {
				return timerValue.doubleValue();
			}
		}
		return null;
	}

	private Double getDistributionSummaryValue() {
		if (this.value instanceof Double doubleValue) {
			return doubleValue;
		}
		return null;
	}

	private Long getTimerValue() {
		if (this.value instanceof Double doubleValue) {
			return TimeUnit.MILLISECONDS.toNanos(doubleValue.longValue());
		}
		if (this.value instanceof Duration duration) {
			return duration.toNanos();
		}
		return null;
	}

	/**
	 * Return a new {@link MeterValue} instance for the given String value. The value may
	 * contain a simple number, or a {@link DurationStyle duration style string}.
	 * @param value the source value
	 * @return a {@link MeterValue} instance
	 */
	public static MeterValue valueOf(String value) {
		Duration duration = safeParseDuration(value);
		if (duration != null) {
			return new MeterValue(duration);
		}
		return new MeterValue(Double.valueOf(value));
	}

	/**
	 * Return a new {@link MeterValue} instance for the given double value.
	 * @param value the source value
	 * @return a {@link MeterValue} instance
	 * @since 2.3.0
	 */
	public static MeterValue valueOf(double value) {
		return new MeterValue(value);
	}

	private static Duration safeParseDuration(String value) {
		try {
			return DurationStyle.detectAndParse(value);
		}
		catch (IllegalArgumentException ex) {
			return null;
		}
	}

}
