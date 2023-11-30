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

package org.springframework.boot.devtools.restart.classloader;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.Enumeration;

import org.springframework.boot.devtools.restart.classloader.ClassLoaderFile.Kind;
import org.springframework.core.SmartClassLoader;
import org.springframework.util.Assert;

/**
 * Disposable {@link ClassLoader} used to support application restarting. Provides parent
 * last loading for the specified URLs.
 *
 * @author Andy Clement
 * @author Phillip Webb
 * @since 1.3.0
 */
public class RestartClassLoader extends URLClassLoader implements SmartClassLoader {

	private final ClassLoaderFileRepository updatedFiles;

	/**
	 * Create a new {@link RestartClassLoader} instance.
	 * @param parent the parent classloader
	 * @param urls the urls managed by the classloader
	 */
	public RestartClassLoader(ClassLoader parent, URL[] urls) {
		this(parent, urls, ClassLoaderFileRepository.NONE);
	}

	/**
	 * Create a new {@link RestartClassLoader} instance.
	 * @param parent the parent classloader
	 * @param updatedFiles any files that have been updated since the JARs referenced in
	 * URLs were created.
	 * @param urls the urls managed by the classloader
	 */
	public RestartClassLoader(ClassLoader parent, URL[] urls, ClassLoaderFileRepository updatedFiles) {
		super(urls, parent);
		Assert.notNull(parent, "Parent must not be null");
		Assert.notNull(updatedFiles, "UpdatedFiles must not be null");
		this.updatedFiles = updatedFiles;
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		// Use the parent since we're shadowing resource and we don't want duplicates
		Enumeration<URL> resources = getParent().getResources(name);
		ClassLoaderFile file = this.updatedFiles.getFile(name);
		if (file != null) {
			// Assume that we're replacing just the first item
			if (resources.hasMoreElements()) {
				resources.nextElement();
			}
			if (file.getKind() != Kind.DELETED) {
				return new CompoundEnumeration<>(createFileUrl(name, file), resources);
			}
		}
		return resources;
	}

	@Override
	public URL getResource(String name) {
		ClassLoaderFile file = this.updatedFiles.getFile(name);
		if (file != null && file.getKind() == Kind.DELETED) {
			return null;
		}
		URL resource = findResource(name);
		if (resource != null) {
			return resource;
		}
		return getParent().getResource(name);
	}

	@Override
	public URL findResource(String name) {
		final ClassLoaderFile file = this.updatedFiles.getFile(name);
		if (file == null) {
			return super.findResource(name);
		}
		if (file.getKind() == Kind.DELETED) {
			return null;
		}
		return createFileUrl(name, file);
	}

	@Override
	public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		String path = name.replace('.', '/').concat(".class");
		ClassLoaderFile file = this.updatedFiles.getFile(path);
		if (file != null && file.getKind() == Kind.DELETED) {
			throw new ClassNotFoundException(name);
		}
		synchronized (getClassLoadingLock(name)) {
			Class<?> loadedClass = findLoadedClass(name);
			if (loadedClass == null) {
				try {
					loadedClass = findClass(name);
				}
				catch (ClassNotFoundException ex) {
					loadedClass = Class.forName(name, false, getParent());
				}
			}
			if (resolve) {
				resolveClass(loadedClass);
			}
			return loadedClass;
		}
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		String path = name.replace('.', '/').concat(".class");
		final ClassLoaderFile file = this.updatedFiles.getFile(path);
		if (file == null) {
			return super.findClass(name);
		}
		if (file.getKind() == Kind.DELETED) {
			throw new ClassNotFoundException(name);
		}
		byte[] bytes = file.getContents();
		return defineClass(name, bytes, 0, bytes.length);
	}

	@Override
	public Class<?> publicDefineClass(String name, byte[] b, ProtectionDomain protectionDomain) {
		return defineClass(name, b, 0, b.length, protectionDomain);
	}

	@Override
	public ClassLoader getOriginalClassLoader() {
		return getParent();
	}

	private URL createFileUrl(String name, ClassLoaderFile file) {
		try {
			return new URL("reloaded", null, -1, "/" + name, new ClassLoaderFileURLStreamHandler(file));
		}
		catch (MalformedURLException ex) {
			throw new IllegalStateException(ex);
		}
	}

	@Override
	public boolean isClassReloadable(Class<?> classType) {
		return (classType.getClassLoader() instanceof RestartClassLoader);
	}

	/**
	 * Compound {@link Enumeration} that adds an additional item to the front.
	 */
	private static class CompoundEnumeration<E> implements Enumeration<E> {

		private E firstElement;

		private final Enumeration<E> enumeration;

		CompoundEnumeration(E firstElement, Enumeration<E> enumeration) {
			this.firstElement = firstElement;
			this.enumeration = enumeration;
		}

		@Override
		public boolean hasMoreElements() {
			return (this.firstElement != null || this.enumeration.hasMoreElements());
		}

		@Override
		public E nextElement() {
			if (this.firstElement == null) {
				return this.enumeration.nextElement();
			}
			E element = this.firstElement;
			this.firstElement = null;
			return element;
		}

	}

}
