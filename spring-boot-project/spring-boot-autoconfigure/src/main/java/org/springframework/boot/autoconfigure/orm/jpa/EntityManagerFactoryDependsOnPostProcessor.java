/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.orm.jpa;

import jakarta.persistence.EntityManagerFactory;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AbstractDependsOnBeanFactoryPostProcessor;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;

/**
 * {@link BeanFactoryPostProcessor} that can be used to dynamically declare that all
 * {@link EntityManagerFactory} beans should "depend on" one or more specific beans.
 *
 * @author Marcel Overdijk
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Andrii Hrytsiuk
 * @since 2.5.0
 * @see BeanDefinition#setDependsOn(String[])
 */
public class EntityManagerFactoryDependsOnPostProcessor extends AbstractDependsOnBeanFactoryPostProcessor {

	/**
	 * Creates a new {@code EntityManagerFactoryDependsOnPostProcessor} that will set up
	 * dependencies upon beans with the given names.
	 * @param dependsOn names of the beans to depend upon
	 */
	public EntityManagerFactoryDependsOnPostProcessor(String... dependsOn) {
		super(EntityManagerFactory.class, AbstractEntityManagerFactoryBean.class, dependsOn);
	}

	/**
	 * Creates a new {@code EntityManagerFactoryDependsOnPostProcessor} that will set up
	 * dependencies upon beans with the given types.
	 * @param dependsOn types of the beans to depend upon
	 */
	public EntityManagerFactoryDependsOnPostProcessor(Class<?>... dependsOn) {
		super(EntityManagerFactory.class, AbstractEntityManagerFactoryBean.class, dependsOn);
	}

}
