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

package org.springframework.boot.autoconfigure.elasticsearch;

import org.elasticsearch.client.RestClientBuilder;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientConfigurations.RestClientBuilderConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientConfigurations.RestClientConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientConfigurations.RestClientSnifferConfiguration;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Elasticsearch REST clients.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @since 2.1.0
 */
@AutoConfiguration(after = SslAutoConfiguration.class)
@ConditionalOnClass(RestClientBuilder.class)
@EnableConfigurationProperties(ElasticsearchProperties.class)
@Import({ RestClientBuilderConfiguration.class, RestClientConfiguration.class, RestClientSnifferConfiguration.class })
public class ElasticsearchRestClientAutoConfiguration {

}
