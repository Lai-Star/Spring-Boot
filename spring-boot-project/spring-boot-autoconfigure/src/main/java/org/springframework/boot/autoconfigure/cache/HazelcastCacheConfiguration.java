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

package org.springframework.boot.autoconfigure.cache;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.cache.HazelcastCacheManager;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.hazelcast.HazelcastAutoConfiguration;
import org.springframework.boot.autoconfigure.hazelcast.HazelcastConfigResourceCondition;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * Hazelcast cache configuration. Can either reuse the {@link HazelcastInstance} that has
 * been configured by the general {@link HazelcastAutoConfiguration} or create a separate
 * one if the {@code spring.cache.hazelcast.config} property has been set.
 * <p>
 * If the {@link HazelcastAutoConfiguration} has been disabled, an attempt to configure a
 * default {@link HazelcastInstance} is still made, using the same defaults.
 *
 * @author Stephane Nicoll
 * @see HazelcastConfigResourceCondition
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ HazelcastInstance.class, HazelcastCacheManager.class })
@ConditionalOnMissingBean(CacheManager.class)
@Conditional(CacheCondition.class)
@ConditionalOnSingleCandidate(HazelcastInstance.class)
class HazelcastCacheConfiguration {

	@Bean
	HazelcastCacheManager cacheManager(CacheManagerCustomizers customizers,
			HazelcastInstance existingHazelcastInstance) {
		HazelcastCacheManager cacheManager = new HazelcastCacheManager(existingHazelcastInstance);
		return customizers.customize(cacheManager);
	}

}
