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

package org.springframework.boot.autoconfigure.cassandra;

import java.time.Duration;
import java.util.List;

import com.datastax.oss.driver.api.core.DefaultConsistencyLevel;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * Configuration properties for Cassandra.
 *
 * @author Julien Dubois
 * @author Phillip Webb
 * @author Mark Paluch
 * @author Stephane Nicoll
 * @author Scott Frederick
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = "spring.cassandra")
public class CassandraProperties {

	/**
	 * Location of the configuration file to use.
	 */
	private Resource config;

	/**
	 * Keyspace name to use.
	 */
	private String keyspaceName;

	/**
	 * Name of the Cassandra session.
	 */
	private String sessionName;

	/**
	 * Cluster node addresses in the form 'host:port', or a simple 'host' to use the
	 * configured port.
	 */
	private List<String> contactPoints;

	/**
	 * Port to use if a contact point does not specify one.
	 */
	private int port = 9042;

	/**
	 * Datacenter that is considered "local". Contact points should be from this
	 * datacenter.
	 */
	private String localDatacenter;

	/**
	 * Login user of the server.
	 */
	private String username;

	/**
	 * Login password of the server.
	 */
	private String password;

	/**
	 * Compression supported by the Cassandra binary protocol.
	 */
	private Compression compression;

	/**
	 * Schema action to take at startup.
	 */
	private String schemaAction = "none";

	/**
	 * SSL configuration.
	 */
	private Ssl ssl = new Ssl();

	/**
	 * Connection configuration.
	 */
	private final Connection connection = new Connection();

	/**
	 * Pool configuration.
	 */
	private final Pool pool = new Pool();

	/**
	 * Request configuration.
	 */
	private final Request request = new Request();

	/**
	 * Control connection configuration.
	 */
	private final Controlconnection controlconnection = new Controlconnection();

	public Resource getConfig() {
		return this.config;
	}

	public void setConfig(Resource config) {
		this.config = config;
	}

	public String getKeyspaceName() {
		return this.keyspaceName;
	}

	public void setKeyspaceName(String keyspaceName) {
		this.keyspaceName = keyspaceName;
	}

	public String getSessionName() {
		return this.sessionName;
	}

	public void setSessionName(String sessionName) {
		this.sessionName = sessionName;
	}

	public List<String> getContactPoints() {
		return this.contactPoints;
	}

	public void setContactPoints(List<String> contactPoints) {
		this.contactPoints = contactPoints;
	}

	public int getPort() {
		return this.port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getLocalDatacenter() {
		return this.localDatacenter;
	}

	public void setLocalDatacenter(String localDatacenter) {
		this.localDatacenter = localDatacenter;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Compression getCompression() {
		return this.compression;
	}

	public void setCompression(Compression compression) {
		this.compression = compression;
	}

	public Ssl getSsl() {
		return this.ssl;
	}

	public void setSsl(Ssl ssl) {
		this.ssl = ssl;
	}

	public String getSchemaAction() {
		return this.schemaAction;
	}

	public void setSchemaAction(String schemaAction) {
		this.schemaAction = schemaAction;
	}

	public Connection getConnection() {
		return this.connection;
	}

	public Pool getPool() {
		return this.pool;
	}

	public Request getRequest() {
		return this.request;
	}

	public Controlconnection getControlconnection() {
		return this.controlconnection;
	}

	public static class Ssl {

		/**
		 * Whether to enable SSL support.
		 */
		private Boolean enabled;

		/**
		 * SSL bundle name.
		 */
		private String bundle;

		public boolean isEnabled() {
			return (this.enabled != null) ? this.enabled : this.bundle != null;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getBundle() {
			return this.bundle;
		}

		public void setBundle(String bundle) {
			this.bundle = bundle;
		}

	}

	public static class Connection {

		/**
		 * Timeout to use when establishing driver connections.
		 */
		private Duration connectTimeout;

		/**
		 * Timeout to use for internal queries that run as part of the initialization
		 * process, just after a connection is opened.
		 */
		private Duration initQueryTimeout;

		public Duration getConnectTimeout() {
			return this.connectTimeout;
		}

		public void setConnectTimeout(Duration connectTimeout) {
			this.connectTimeout = connectTimeout;
		}

		public Duration getInitQueryTimeout() {
			return this.initQueryTimeout;
		}

		public void setInitQueryTimeout(Duration initQueryTimeout) {
			this.initQueryTimeout = initQueryTimeout;
		}

	}

	public static class Request {

		/**
		 * How long the driver waits for a request to complete.
		 */
		private Duration timeout;

		/**
		 * Queries consistency level.
		 */
		private DefaultConsistencyLevel consistency;

		/**
		 * Queries serial consistency level.
		 */
		private DefaultConsistencyLevel serialConsistency;

		/**
		 * How many rows will be retrieved simultaneously in a single network round-trip.
		 */
		private Integer pageSize;

		private final Throttler throttler = new Throttler();

		public Duration getTimeout() {
			return this.timeout;
		}

		public void setTimeout(Duration timeout) {
			this.timeout = timeout;
		}

		public DefaultConsistencyLevel getConsistency() {
			return this.consistency;
		}

		public void setConsistency(DefaultConsistencyLevel consistency) {
			this.consistency = consistency;
		}

		public DefaultConsistencyLevel getSerialConsistency() {
			return this.serialConsistency;
		}

		public void setSerialConsistency(DefaultConsistencyLevel serialConsistency) {
			this.serialConsistency = serialConsistency;
		}

		public Integer getPageSize() {
			return this.pageSize;
		}

		public void setPageSize(int pageSize) {
			this.pageSize = pageSize;
		}

		public Throttler getThrottler() {
			return this.throttler;
		}

	}

	/**
	 * Pool properties.
	 */
	public static class Pool {

		/**
		 * Idle timeout before an idle connection is removed.
		 */
		private Duration idleTimeout;

		/**
		 * Heartbeat interval after which a message is sent on an idle connection to make
		 * sure it's still alive.
		 */
		private Duration heartbeatInterval;

		public Duration getIdleTimeout() {
			return this.idleTimeout;
		}

		public void setIdleTimeout(Duration idleTimeout) {
			this.idleTimeout = idleTimeout;
		}

		public Duration getHeartbeatInterval() {
			return this.heartbeatInterval;
		}

		public void setHeartbeatInterval(Duration heartbeatInterval) {
			this.heartbeatInterval = heartbeatInterval;
		}

	}

	public static class Controlconnection {

		/**
		 * Timeout to use for control queries.
		 */
		private Duration timeout;

		public Duration getTimeout() {
			return this.timeout;
		}

		public void setTimeout(Duration timeout) {
			this.timeout = timeout;
		}

	}

	public static class Throttler {

		/**
		 * Request throttling type.
		 */
		private ThrottlerType type;

		/**
		 * Maximum number of requests that can be enqueued when the throttling threshold
		 * is exceeded.
		 */
		private Integer maxQueueSize;

		/**
		 * Maximum number of requests that are allowed to execute in parallel.
		 */
		private Integer maxConcurrentRequests;

		/**
		 * Maximum allowed request rate.
		 */
		private Integer maxRequestsPerSecond;

		/**
		 * How often the throttler attempts to dequeue requests. Set this high enough that
		 * each attempt will process multiple entries in the queue, but not delay requests
		 * too much.
		 */
		private Duration drainInterval;

		public ThrottlerType getType() {
			return this.type;
		}

		public void setType(ThrottlerType type) {
			this.type = type;
		}

		public Integer getMaxQueueSize() {
			return this.maxQueueSize;
		}

		public void setMaxQueueSize(int maxQueueSize) {
			this.maxQueueSize = maxQueueSize;
		}

		public Integer getMaxConcurrentRequests() {
			return this.maxConcurrentRequests;
		}

		public void setMaxConcurrentRequests(int maxConcurrentRequests) {
			this.maxConcurrentRequests = maxConcurrentRequests;
		}

		public Integer getMaxRequestsPerSecond() {
			return this.maxRequestsPerSecond;
		}

		public void setMaxRequestsPerSecond(int maxRequestsPerSecond) {
			this.maxRequestsPerSecond = maxRequestsPerSecond;
		}

		public Duration getDrainInterval() {
			return this.drainInterval;
		}

		public void setDrainInterval(Duration drainInterval) {
			this.drainInterval = drainInterval;
		}

	}

	/**
	 * Name of the algorithm used to compress protocol frames.
	 */
	public enum Compression {

		/**
		 * Requires 'net.jpountz.lz4:lz4'.
		 */
		LZ4,

		/**
		 * Requires org.xerial.snappy:snappy-java.
		 */
		SNAPPY,

		/**
		 * No compression.
		 */
		NONE;

	}

	public enum ThrottlerType {

		/**
		 * Limit the number of requests that can be executed in parallel.
		 */
		CONCURRENCY_LIMITING("ConcurrencyLimitingRequestThrottler"),

		/**
		 * Limits the request rate per second.
		 */
		RATE_LIMITING("RateLimitingRequestThrottler"),

		/**
		 * No request throttling.
		 */
		NONE("PassThroughRequestThrottler");

		private final String type;

		ThrottlerType(String type) {
			this.type = type;
		}

		public String type() {
			return this.type;
		}

	}

}
