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

package org.springframework.boot.actuate.autoconfigure.ldap;

import java.util.Map;

import org.springframework.boot.actuate.autoconfigure.health.CompositeHealthContributorConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.ldap.LdapHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.ldap.core.LdapOperations;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link LdapHealthIndicator}.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@AutoConfiguration(after = LdapAutoConfiguration.class)
@ConditionalOnClass(LdapOperations.class)
@ConditionalOnBean(LdapOperations.class)
@ConditionalOnEnabledHealthIndicator("ldap")
public class LdapHealthContributorAutoConfiguration
		extends CompositeHealthContributorConfiguration<LdapHealthIndicator, LdapOperations> {

	public LdapHealthContributorAutoConfiguration() {
		super(LdapHealthIndicator::new);
	}

	@Bean
	@ConditionalOnMissingBean(name = { "ldapHealthIndicator", "ldapHealthContributor" })
	public HealthContributor ldapHealthContributor(Map<String, LdapOperations> ldapOperations) {
		return createContributor(ldapOperations);
	}

}
