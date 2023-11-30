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

package org.springframework.boot.actuate.autoconfigure.couchbase;

import java.util.Map;

import com.couchbase.client.java.Cluster;
import reactor.core.publisher.Flux;

import org.springframework.boot.actuate.autoconfigure.health.CompositeReactiveHealthContributorConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.couchbase.CouchbaseReactiveHealthIndicator;
import org.springframework.boot.actuate.health.ReactiveHealthContributor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link CouchbaseReactiveHealthIndicator}.
 *
 * @author Mikalai Lushchytski
 * @author Stephane Nicoll
 * @since 2.1.0
 */
@AutoConfiguration(after = CouchbaseAutoConfiguration.class)
@ConditionalOnClass({ Cluster.class, Flux.class })
@ConditionalOnBean(Cluster.class)
@ConditionalOnEnabledHealthIndicator("couchbase")
public class CouchbaseReactiveHealthContributorAutoConfiguration
		extends CompositeReactiveHealthContributorConfiguration<CouchbaseReactiveHealthIndicator, Cluster> {

	public CouchbaseReactiveHealthContributorAutoConfiguration() {
		super(CouchbaseReactiveHealthIndicator::new);
	}

	@Bean
	@ConditionalOnMissingBean(name = { "couchbaseHealthIndicator", "couchbaseHealthContributor" })
	public ReactiveHealthContributor couchbaseHealthContributor(Map<String, Cluster> clusters) {
		return createContributor(clusters);
	}

}
