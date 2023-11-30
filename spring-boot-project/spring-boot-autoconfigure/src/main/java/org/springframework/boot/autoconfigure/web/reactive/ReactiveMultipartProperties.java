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

package org.springframework.boot.autoconfigure.web.reactive;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.codec.multipart.DefaultPartHttpMessageReader;
import org.springframework.http.codec.multipart.PartEventHttpMessageReader;
import org.springframework.util.unit.DataSize;

/**
 * {@link ConfigurationProperties Configuration properties} for configuring multipart
 * support in Spring Webflux. Used to configure the {@link DefaultPartHttpMessageReader}
 * and the {@link PartEventHttpMessageReader}.
 *
 * @author Chris Bono
 * @since 2.6.0
 */
@ConfigurationProperties(prefix = "spring.webflux.multipart")
public class ReactiveMultipartProperties {

	/**
	 * Maximum amount of memory allowed per part before it's written to disk. Set to -1 to
	 * store all contents in memory.
	 */
	private DataSize maxInMemorySize = DataSize.ofKilobytes(256);

	/**
	 * Maximum amount of memory allowed per headers section of each part. Set to -1 to
	 * enforce no limits.
	 */
	private DataSize maxHeadersSize = DataSize.ofKilobytes(10);

	/**
	 * Maximum amount of disk space allowed per part. Default is -1 which enforces no
	 * limits.
	 */
	private DataSize maxDiskUsagePerPart = DataSize.ofBytes(-1);

	/**
	 * Maximum number of parts allowed in a given multipart request. Default is -1 which
	 * enforces no limits.
	 */
	private Integer maxParts = -1;

	/**
	 * Directory used to store file parts larger than 'maxInMemorySize'. Default is a
	 * directory named 'spring-multipart' created under the system temporary directory.
	 * Ignored when using the PartEvent streaming support.
	 */
	private String fileStorageDirectory;

	/**
	 * Character set used to decode headers.
	 */
	private Charset headersCharset = StandardCharsets.UTF_8;

	public DataSize getMaxInMemorySize() {
		return this.maxInMemorySize;
	}

	public void setMaxInMemorySize(DataSize maxInMemorySize) {
		this.maxInMemorySize = maxInMemorySize;
	}

	public DataSize getMaxHeadersSize() {
		return this.maxHeadersSize;
	}

	public void setMaxHeadersSize(DataSize maxHeadersSize) {
		this.maxHeadersSize = maxHeadersSize;
	}

	public DataSize getMaxDiskUsagePerPart() {
		return this.maxDiskUsagePerPart;
	}

	public void setMaxDiskUsagePerPart(DataSize maxDiskUsagePerPart) {
		this.maxDiskUsagePerPart = maxDiskUsagePerPart;
	}

	public Integer getMaxParts() {
		return this.maxParts;
	}

	public void setMaxParts(Integer maxParts) {
		this.maxParts = maxParts;
	}

	public String getFileStorageDirectory() {
		return this.fileStorageDirectory;
	}

	public void setFileStorageDirectory(String fileStorageDirectory) {
		this.fileStorageDirectory = fileStorageDirectory;
	}

	public Charset getHeadersCharset() {
		return this.headersCharset;
	}

	public void setHeadersCharset(Charset headersCharset) {
		this.headersCharset = headersCharset;
	}

}
