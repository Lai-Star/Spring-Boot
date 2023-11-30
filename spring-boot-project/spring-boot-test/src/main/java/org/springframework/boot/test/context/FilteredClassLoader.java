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

package org.springframework.boot.test.context;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.function.Predicate;

import org.springframework.core.SmartClassLoader;
import org.springframework.core.io.ClassPathResource;

/**
 * Test {@link URLClassLoader} that can filter the classes and resources it can load.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Roy Jacobs
 * @since 2.0.0
 */
public class FilteredClassLoader extends URLClassLoader implements SmartClassLoader {

	private final Collection<Predicate<String>> classesFilters;

	private final Collection<Predicate<String>> resourcesFilters;

	/**
	 * Create a {@link FilteredClassLoader} that hides the given classes.
	 * @param hiddenClasses the classes to hide
	 */
	public FilteredClassLoader(Class<?>... hiddenClasses) {
		this(Collections.singleton(ClassFilter.of(hiddenClasses)), Collections.emptyList());
	}

	/**
	 * Create a {@link FilteredClassLoader} that hides classes from the given packages.
	 * @param hiddenPackages the packages to hide
	 */
	public FilteredClassLoader(String... hiddenPackages) {
		this(Collections.singleton(PackageFilter.of(hiddenPackages)), Collections.emptyList());
	}

	/**
	 * Create a {@link FilteredClassLoader} that hides resources from the given
	 * {@link ClassPathResource classpath resources}.
	 * @param hiddenResources the resources to hide
	 * @since 2.1.0
	 */
	public FilteredClassLoader(ClassPathResource... hiddenResources) {
		this(Collections.emptyList(), Collections.singleton(ClassPathResourceFilter.of(hiddenResources)));
	}

	/**
	 * Create a {@link FilteredClassLoader} that filters based on the given predicate.
	 * @param filters a set of filters to determine when a class name or resource should
	 * be hidden. A {@link Predicate#test(Object) result} of {@code true} indicates a
	 * filtered class or resource. The input of the predicate can either be the binary
	 * name of a class or a resource name.
	 */
	@SafeVarargs
	@SuppressWarnings("varargs")
	public FilteredClassLoader(Predicate<String>... filters) {
		this(Arrays.asList(filters), Arrays.asList(filters));
	}

	private FilteredClassLoader(Collection<Predicate<String>> classesFilters,
			Collection<Predicate<String>> resourcesFilters) {
		super(new URL[0], FilteredClassLoader.class.getClassLoader());
		this.classesFilters = classesFilters;
		this.resourcesFilters = resourcesFilters;
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		for (Predicate<String> filter : this.classesFilters) {
			if (filter.test(name)) {
				throw new ClassNotFoundException();
			}
		}
		return super.loadClass(name, resolve);
	}

	@Override
	public URL getResource(String name) {
		for (Predicate<String> filter : this.resourcesFilters) {
			if (filter.test(name)) {
				return null;
			}
		}
		return super.getResource(name);
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		for (Predicate<String> filter : this.resourcesFilters) {
			if (filter.test(name)) {
				return Collections.emptyEnumeration();
			}
		}
		return super.getResources(name);
	}

	@Override
	public InputStream getResourceAsStream(String name) {
		for (Predicate<String> filter : this.resourcesFilters) {
			if (filter.test(name)) {
				return null;
			}
		}
		return super.getResourceAsStream(name);
	}

	@Override
	public Class<?> publicDefineClass(String name, byte[] b, ProtectionDomain protectionDomain) {
		for (Predicate<String> filter : this.classesFilters) {
			if (filter.test(name)) {
				throw new IllegalArgumentException(String.format("Defining class with name %s is not supported", name));
			}
		}
		return defineClass(name, b, 0, b.length, protectionDomain);
	}

	/**
	 * Filter to restrict the classes that can be loaded.
	 */
	public static final class ClassFilter implements Predicate<String> {

		private final Class<?>[] hiddenClasses;

		private ClassFilter(Class<?>[] hiddenClasses) {
			this.hiddenClasses = hiddenClasses;
		}

		@Override
		public boolean test(String className) {
			for (Class<?> hiddenClass : this.hiddenClasses) {
				if (className.equals(hiddenClass.getName())) {
					return true;
				}
			}
			return false;
		}

		public static ClassFilter of(Class<?>... hiddenClasses) {
			return new ClassFilter(hiddenClasses);
		}

	}

	/**
	 * Filter to restrict the packages that can be loaded.
	 */
	public static final class PackageFilter implements Predicate<String> {

		private final String[] hiddenPackages;

		private PackageFilter(String[] hiddenPackages) {
			this.hiddenPackages = hiddenPackages;
		}

		@Override
		public boolean test(String className) {
			for (String hiddenPackage : this.hiddenPackages) {
				if (className.startsWith(hiddenPackage)) {
					return true;
				}
			}
			return false;
		}

		public static PackageFilter of(String... hiddenPackages) {
			return new PackageFilter(hiddenPackages);
		}

	}

	/**
	 * Filter to restrict the resources that can be loaded.
	 *
	 * @since 2.1.0
	 */
	public static final class ClassPathResourceFilter implements Predicate<String> {

		private final ClassPathResource[] hiddenResources;

		private ClassPathResourceFilter(ClassPathResource[] hiddenResources) {
			this.hiddenResources = hiddenResources;
		}

		@Override
		public boolean test(String resourceName) {
			for (ClassPathResource hiddenResource : this.hiddenResources) {
				if (hiddenResource.getFilename() != null && resourceName.equals(hiddenResource.getPath())) {
					return true;
				}
			}
			return false;
		}

		public static ClassPathResourceFilter of(ClassPathResource... hiddenResources) {
			return new ClassPathResourceFilter(hiddenResources);
		}

	}

}
