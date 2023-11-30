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

package org.springframework.boot.actuate.metrics.export.prometheus;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Set;

import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.CollectorRegistry;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;
import org.springframework.lang.Nullable;

/**
 * {@link Endpoint @Endpoint} that outputs metrics in a format that can be scraped by the
 * Prometheus server.
 *
 * @author Jon Schneider
 * @author Johnny Lim
 * @since 2.0.0
 */
@WebEndpoint(id = "prometheus")
public class PrometheusScrapeEndpoint {

	private static final int METRICS_SCRAPE_CHARS_EXTRA = 1024;

	private final CollectorRegistry collectorRegistry;

	private volatile int nextMetricsScrapeSize = 16;

	public PrometheusScrapeEndpoint(CollectorRegistry collectorRegistry) {
		this.collectorRegistry = collectorRegistry;
	}

	@ReadOperation(producesFrom = TextOutputFormat.class)
	public WebEndpointResponse<String> scrape(TextOutputFormat format, @Nullable Set<String> includedNames) {
		try {
			Writer writer = new StringWriter(this.nextMetricsScrapeSize);
			Enumeration<MetricFamilySamples> samples = (includedNames != null)
					? this.collectorRegistry.filteredMetricFamilySamples(includedNames)
					: this.collectorRegistry.metricFamilySamples();
			format.write(writer, samples);

			String scrapePage = writer.toString();
			this.nextMetricsScrapeSize = scrapePage.length() + METRICS_SCRAPE_CHARS_EXTRA;

			return new WebEndpointResponse<>(scrapePage, format);
		}
		catch (IOException ex) {
			// This actually never happens since StringWriter doesn't throw an IOException
			throw new IllegalStateException("Writing metrics failed", ex);
		}
	}

}
