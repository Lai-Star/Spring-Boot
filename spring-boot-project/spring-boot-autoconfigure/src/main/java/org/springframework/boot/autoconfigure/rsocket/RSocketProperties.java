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

package org.springframework.boot.autoconfigure.rsocket;

import java.net.InetAddress;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.rsocket.server.RSocketServer;
import org.springframework.boot.web.server.Ssl;
import org.springframework.util.unit.DataSize;

/**
 * {@link ConfigurationProperties Properties} for RSocket support.
 *
 * @author Brian Clozel
 * @author Chris Bono
 * @since 2.2.0
 */
@ConfigurationProperties("spring.rsocket")
public class RSocketProperties {

	@NestedConfigurationProperty
	private final Server server = new Server();

	public Server getServer() {
		return this.server;
	}

	public static class Server {

		/**
		 * Server port.
		 */
		private Integer port;

		/**
		 * Network address to which the server should bind.
		 */
		private InetAddress address;

		/**
		 * RSocket transport protocol.
		 */
		private RSocketServer.Transport transport = RSocketServer.Transport.TCP;

		/**
		 * Path under which RSocket handles requests (only works with websocket
		 * transport).
		 */
		private String mappingPath;

		/**
		 * Maximum transmission unit. Frames larger than the specified value are
		 * fragmented.
		 */
		private DataSize fragmentSize;

		@NestedConfigurationProperty
		private Ssl ssl;

		private final Spec spec = new Spec();

		public Integer getPort() {
			return this.port;
		}

		public void setPort(Integer port) {
			this.port = port;
		}

		public InetAddress getAddress() {
			return this.address;
		}

		public void setAddress(InetAddress address) {
			this.address = address;
		}

		public RSocketServer.Transport getTransport() {
			return this.transport;
		}

		public void setTransport(RSocketServer.Transport transport) {
			this.transport = transport;
		}

		public String getMappingPath() {
			return this.mappingPath;
		}

		public void setMappingPath(String mappingPath) {
			this.mappingPath = mappingPath;
		}

		public DataSize getFragmentSize() {
			return this.fragmentSize;
		}

		public void setFragmentSize(DataSize fragmentSize) {
			this.fragmentSize = fragmentSize;
		}

		public Ssl getSsl() {
			return this.ssl;
		}

		public void setSsl(Ssl ssl) {
			this.ssl = ssl;
		}

		public Spec getSpec() {
			return this.spec;
		}

		public static class Spec {

			/**
			 * Sub-protocols to use in websocket handshake signature.
			 */
			private String protocols;

			/**
			 * Maximum allowable frame payload length.
			 */
			private DataSize maxFramePayloadLength = DataSize.ofBytes(65536);

			/**
			 * Whether to proxy websocket ping frames or respond to them.
			 */
			private boolean handlePing;

			/**
			 * Whether the websocket compression extension is enabled.
			 */
			private boolean compress;

			public String getProtocols() {
				return this.protocols;
			}

			public void setProtocols(String protocols) {
				this.protocols = protocols;
			}

			public DataSize getMaxFramePayloadLength() {
				return this.maxFramePayloadLength;
			}

			public void setMaxFramePayloadLength(DataSize maxFramePayloadLength) {
				this.maxFramePayloadLength = maxFramePayloadLength;
			}

			public boolean isHandlePing() {
				return this.handlePing;
			}

			public void setHandlePing(boolean handlePing) {
				this.handlePing = handlePing;
			}

			public boolean isCompress() {
				return this.compress;
			}

			public void setCompress(boolean compress) {
				this.compress = compress;
			}

		}

	}

}
