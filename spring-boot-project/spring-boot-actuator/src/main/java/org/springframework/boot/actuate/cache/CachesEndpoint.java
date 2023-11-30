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

package org.springframework.boot.actuate.cache;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import org.springframework.boot.actuate.endpoint.OperationResponseBody;
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;

/**
 * {@link Endpoint @Endpoint} to expose available {@link Cache caches}.
 *
 * @author Johannes Edmeier
 * @author Stephane Nicoll
 * @since 2.1.0
 */
@Endpoint(id = "caches")
public class CachesEndpoint {

	private final Map<String, CacheManager> cacheManagers;

	/**
	 * Create a new endpoint with the {@link CacheManager} instances to use.
	 * @param cacheManagers the cache managers to use, indexed by name
	 */
	public CachesEndpoint(Map<String, CacheManager> cacheManagers) {
		this.cacheManagers = new LinkedHashMap<>(cacheManagers);
	}

	/**
	 * Return a {@link CachesDescriptor} of all available {@link Cache caches}.
	 * @return a caches reports
	 */
	@ReadOperation
	public CachesDescriptor caches() {
		Map<String, Map<String, CacheDescriptor>> descriptors = new LinkedHashMap<>();
		getCacheEntries(matchAll(), matchAll()).forEach((entry) -> {
			String cacheName = entry.getName();
			String cacheManager = entry.getCacheManager();
			Map<String, CacheDescriptor> cacheManagerDescriptors = descriptors.computeIfAbsent(cacheManager,
					(key) -> new LinkedHashMap<>());
			cacheManagerDescriptors.put(cacheName, new CacheDescriptor(entry.getTarget()));
		});
		Map<String, CacheManagerDescriptor> cacheManagerDescriptors = new LinkedHashMap<>();
		descriptors.forEach((name, entries) -> cacheManagerDescriptors.put(name, new CacheManagerDescriptor(entries)));
		return new CachesDescriptor(cacheManagerDescriptors);
	}

	/**
	 * Return a {@link CacheDescriptor} for the specified cache.
	 * @param cache the name of the cache
	 * @param cacheManager the name of the cacheManager (can be {@code null}
	 * @return the descriptor of the cache or {@code null} if no such cache exists
	 * @throws NonUniqueCacheException if more than one cache with that name exists and no
	 * {@code cacheManager} was provided to identify a unique candidate
	 */
	@ReadOperation
	public CacheEntryDescriptor cache(@Selector String cache, @Nullable String cacheManager) {
		return extractUniqueCacheEntry(cache, getCacheEntries((name) -> name.equals(cache), isNameMatch(cacheManager)));
	}

	/**
	 * Clear all the available {@link Cache caches}.
	 */
	@DeleteOperation
	public void clearCaches() {
		getCacheEntries(matchAll(), matchAll()).forEach(this::clearCache);
	}

	/**
	 * Clear the specific {@link Cache}.
	 * @param cache the name of the cache
	 * @param cacheManager the name of the cacheManager (can be {@code null} to match all)
	 * @return {@code true} if the cache was cleared or {@code false} if no such cache
	 * exists
	 * @throws NonUniqueCacheException if more than one cache with that name exists and no
	 * {@code cacheManager} was provided to identify a unique candidate
	 */
	@DeleteOperation
	public boolean clearCache(@Selector String cache, @Nullable String cacheManager) {
		CacheEntryDescriptor entry = extractUniqueCacheEntry(cache,
				getCacheEntries((name) -> name.equals(cache), isNameMatch(cacheManager)));
		return (entry != null && clearCache(entry));
	}

	private List<CacheEntryDescriptor> getCacheEntries(Predicate<String> cacheNamePredicate,
			Predicate<String> cacheManagerNamePredicate) {
		return this.cacheManagers.keySet()
			.stream()
			.filter(cacheManagerNamePredicate)
			.flatMap((cacheManagerName) -> getCacheEntries(cacheManagerName, cacheNamePredicate).stream())
			.toList();
	}

	private List<CacheEntryDescriptor> getCacheEntries(String cacheManagerName, Predicate<String> cacheNamePredicate) {
		CacheManager cacheManager = this.cacheManagers.get(cacheManagerName);
		return cacheManager.getCacheNames()
			.stream()
			.filter(cacheNamePredicate)
			.map(cacheManager::getCache)
			.filter(Objects::nonNull)
			.map((cache) -> new CacheEntryDescriptor(cache, cacheManagerName))
			.toList();
	}

	private CacheEntryDescriptor extractUniqueCacheEntry(String cache, List<CacheEntryDescriptor> entries) {
		if (entries.size() > 1) {
			throw new NonUniqueCacheException(cache,
					entries.stream().map(CacheEntryDescriptor::getCacheManager).distinct().toList());
		}
		return (!entries.isEmpty() ? entries.get(0) : null);
	}

	private boolean clearCache(CacheEntryDescriptor entry) {
		String cacheName = entry.getName();
		String cacheManager = entry.getCacheManager();
		Cache cache = this.cacheManagers.get(cacheManager).getCache(cacheName);
		if (cache != null) {
			cache.clear();
			return true;
		}
		return false;
	}

	private Predicate<String> isNameMatch(String name) {
		return (name != null) ? ((requested) -> requested.equals(name)) : matchAll();
	}

	private Predicate<String> matchAll() {
		return (name) -> true;
	}

	/**
	 * Description of the caches.
	 */
	public static final class CachesDescriptor implements OperationResponseBody {

		private final Map<String, CacheManagerDescriptor> cacheManagers;

		public CachesDescriptor(Map<String, CacheManagerDescriptor> cacheManagers) {
			this.cacheManagers = cacheManagers;
		}

		public Map<String, CacheManagerDescriptor> getCacheManagers() {
			return this.cacheManagers;
		}

	}

	/**
	 * Description of a {@link CacheManager}.
	 */
	public static final class CacheManagerDescriptor {

		private final Map<String, CacheDescriptor> caches;

		public CacheManagerDescriptor(Map<String, CacheDescriptor> caches) {
			this.caches = caches;
		}

		public Map<String, CacheDescriptor> getCaches() {
			return this.caches;
		}

	}

	/**
	 * Description of a {@link Cache}.
	 */
	public static class CacheDescriptor implements OperationResponseBody {

		private final String target;

		public CacheDescriptor(String target) {
			this.target = target;
		}

		/**
		 * Return the fully qualified name of the native cache.
		 * @return the fully qualified name of the native cache
		 */
		public String getTarget() {
			return this.target;
		}

	}

	/**
	 * Description of a {@link Cache} entry.
	 */
	public static final class CacheEntryDescriptor extends CacheDescriptor {

		private final String name;

		private final String cacheManager;

		public CacheEntryDescriptor(Cache cache, String cacheManager) {
			super(cache.getNativeCache().getClass().getName());
			this.name = cache.getName();
			this.cacheManager = cacheManager;
		}

		public String getName() {
			return this.name;
		}

		public String getCacheManager() {
			return this.cacheManager;
		}

	}

}
