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

package org.springframework.boot.autoconfigure.jmx;

import java.util.Hashtable;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jmx.export.metadata.JmxAttributeSource;
import org.springframework.jmx.export.naming.MetadataNamingStrategy;
import org.springframework.jmx.support.JmxUtils;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.util.ObjectUtils;

/**
 * Extension of {@link MetadataNamingStrategy} that supports a parent
 * {@link ApplicationContext}.
 *
 * @author Dave Syer
 * @since 1.1.1
 */
public class ParentAwareNamingStrategy extends MetadataNamingStrategy implements ApplicationContextAware {

	private ApplicationContext applicationContext;

	private boolean ensureUniqueRuntimeObjectNames;

	public ParentAwareNamingStrategy(JmxAttributeSource attributeSource) {
		super(attributeSource);
	}

	/**
	 * Set if unique runtime object names should be ensured.
	 * @param ensureUniqueRuntimeObjectNames {@code true} if unique names should be
	 * ensured.
	 */
	public void setEnsureUniqueRuntimeObjectNames(boolean ensureUniqueRuntimeObjectNames) {
		this.ensureUniqueRuntimeObjectNames = ensureUniqueRuntimeObjectNames;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public ObjectName getObjectName(Object managedBean, String beanKey) throws MalformedObjectNameException {
		ObjectName name = super.getObjectName(managedBean, beanKey);
		if (this.ensureUniqueRuntimeObjectNames) {
			return JmxUtils.appendIdentityToObjectName(name, managedBean);
		}
		if (parentContextContainsSameBean(this.applicationContext, beanKey)) {
			return appendToObjectName(name, "context", ObjectUtils.getIdentityHexString(this.applicationContext));
		}
		return name;
	}

	private boolean parentContextContainsSameBean(ApplicationContext context, String beanKey) {
		if (context.getParent() == null) {
			return false;
		}
		try {
			this.applicationContext.getParent().getBean(beanKey);
			return true;
		}
		catch (BeansException ex) {
			return parentContextContainsSameBean(context.getParent(), beanKey);
		}
	}

	private ObjectName appendToObjectName(ObjectName name, String key, String value)
			throws MalformedObjectNameException {
		Hashtable<String, String> keyProperties = name.getKeyPropertyList();
		keyProperties.put(key, value);
		return ObjectNameManager.getInstance(name.getDomain(), keyProperties);
	}

}
