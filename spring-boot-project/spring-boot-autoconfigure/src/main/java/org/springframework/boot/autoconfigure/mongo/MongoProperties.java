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

package org.springframework.boot.autoconfigure.mongo;

import java.util.List;

import com.mongodb.ConnectionString;
import org.bson.UuidRepresentation;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Mongo.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Josh Long
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Nasko Vasilev
 * @author Mark Paluch
 * @author Artsiom Yudovin
 * @author Safeer Ansari
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "spring.data.mongodb")
public class MongoProperties {

	/**
	 * Default port used when the configured port is {@code null}.
	 */
	public static final int DEFAULT_PORT = 27017;

	/**
	 * Default URI used when the configured URI is {@code null}.
	 */
	public static final String DEFAULT_URI = "mongodb://localhost/test";

	/**
	 * Mongo server host. Cannot be set with URI.
	 */
	private String host;

	/**
	 * Mongo server port. Cannot be set with URI.
	 */
	private Integer port = null;

	/**
	 * Additional server hosts. Cannot be set with URI or if 'host' is not specified.
	 * Additional hosts will use the default mongo port of 27017. If you want to use a
	 * different port you can use the "host:port" syntax.
	 */
	private List<String> additionalHosts;

	/**
	 * Mongo database URI. Overrides host, port, username, and password.
	 */
	private String uri;

	/**
	 * Database name. Overrides database in URI.
	 */
	private String database;

	/**
	 * Authentication database name.
	 */
	private String authenticationDatabase;

	private final Gridfs gridfs = new Gridfs();

	/**
	 * Login user of the mongo server. Cannot be set with URI.
	 */
	private String username;

	/**
	 * Login password of the mongo server. Cannot be set with URI.
	 */
	private char[] password;

	/**
	 * Required replica set name for the cluster. Cannot be set with URI.
	 */
	private String replicaSetName;

	/**
	 * Fully qualified name of the FieldNamingStrategy to use.
	 */
	private Class<?> fieldNamingStrategy;

	/**
	 * Representation to use when converting a UUID to a BSON binary value.
	 */
	private UuidRepresentation uuidRepresentation = UuidRepresentation.JAVA_LEGACY;

	private final Ssl ssl = new Ssl();

	/**
	 * Whether to enable auto-index creation.
	 */
	private Boolean autoIndexCreation;

	public String getHost() {
		return this.host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getDatabase() {
		return this.database;
	}

	public void setDatabase(String database) {
		this.database = database;
	}

	public String getAuthenticationDatabase() {
		return this.authenticationDatabase;
	}

	public void setAuthenticationDatabase(String authenticationDatabase) {
		this.authenticationDatabase = authenticationDatabase;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public char[] getPassword() {
		return this.password;
	}

	public void setPassword(char[] password) {
		this.password = password;
	}

	public String getReplicaSetName() {
		return this.replicaSetName;
	}

	public void setReplicaSetName(String replicaSetName) {
		this.replicaSetName = replicaSetName;
	}

	public Class<?> getFieldNamingStrategy() {
		return this.fieldNamingStrategy;
	}

	public void setFieldNamingStrategy(Class<?> fieldNamingStrategy) {
		this.fieldNamingStrategy = fieldNamingStrategy;
	}

	public UuidRepresentation getUuidRepresentation() {
		return this.uuidRepresentation;
	}

	public void setUuidRepresentation(UuidRepresentation uuidRepresentation) {
		this.uuidRepresentation = uuidRepresentation;
	}

	public String getUri() {
		return this.uri;
	}

	public String determineUri() {
		return (this.uri != null) ? this.uri : DEFAULT_URI;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public Integer getPort() {
		return this.port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public Gridfs getGridfs() {
		return this.gridfs;
	}

	public String getMongoClientDatabase() {
		if (this.database != null) {
			return this.database;
		}
		return new ConnectionString(determineUri()).getDatabase();
	}

	public Boolean isAutoIndexCreation() {
		return this.autoIndexCreation;
	}

	public void setAutoIndexCreation(Boolean autoIndexCreation) {
		this.autoIndexCreation = autoIndexCreation;
	}

	public List<String> getAdditionalHosts() {
		return this.additionalHosts;
	}

	public void setAdditionalHosts(List<String> additionalHosts) {
		this.additionalHosts = additionalHosts;
	}

	public Ssl getSsl() {
		return this.ssl;
	}

	public static class Gridfs {

		/**
		 * GridFS database name.
		 */
		private String database;

		/**
		 * GridFS bucket name.
		 */
		private String bucket;

		public String getDatabase() {
			return this.database;
		}

		public void setDatabase(String database) {
			this.database = database;
		}

		public String getBucket() {
			return this.bucket;
		}

		public void setBucket(String bucket) {
			this.bucket = bucket;
		}

	}

	public static class Ssl {

		/**
		 * Whether to enable SSL support. Enabled automatically if "bundle" is provided
		 * unless specified otherwise.
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

}
