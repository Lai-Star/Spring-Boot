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

package org.springframework.boot.devtools.livereload;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;

/**
 * A <a href="https://github.com/livereload">livereload</a> server.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
public class LiveReloadServer {

	/**
	 * The default live reload server port.
	 */
	public static final int DEFAULT_PORT = 35729;

	private static final Log logger = LogFactory.getLog(LiveReloadServer.class);

	private static final int READ_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(4);

	private final ExecutorService executor = Executors.newCachedThreadPool(new WorkerThreadFactory());

	private final List<Connection> connections = new ArrayList<>();

	private final Object monitor = new Object();

	private final int port;

	private final ThreadFactory threadFactory;

	private ServerSocket serverSocket;

	private Thread listenThread;

	/**
	 * Create a new {@link LiveReloadServer} listening on the default port.
	 */
	public LiveReloadServer() {
		this(DEFAULT_PORT);
	}

	/**
	 * Create a new {@link LiveReloadServer} listening on the default port with a specific
	 * {@link ThreadFactory}.
	 * @param threadFactory the thread factory
	 */
	public LiveReloadServer(ThreadFactory threadFactory) {
		this(DEFAULT_PORT, threadFactory);
	}

	/**
	 * Create a new {@link LiveReloadServer} listening on the specified port.
	 * @param port the listen port
	 */
	public LiveReloadServer(int port) {
		this(port, Thread::new);
	}

	/**
	 * Create a new {@link LiveReloadServer} listening on the specified port with a
	 * specific {@link ThreadFactory}.
	 * @param port the listen port
	 * @param threadFactory the thread factory
	 */
	public LiveReloadServer(int port, ThreadFactory threadFactory) {
		this.port = port;
		this.threadFactory = threadFactory;
	}

	/**
	 * Start the livereload server and accept incoming connections.
	 * @return the port on which the server is listening
	 * @throws IOException in case of I/O errors
	 */
	public int start() throws IOException {
		synchronized (this.monitor) {
			Assert.state(!isStarted(), "Server already started");
			logger.debug(LogMessage.format("Starting live reload server on port %s", this.port));
			this.serverSocket = new ServerSocket(this.port);
			int localPort = this.serverSocket.getLocalPort();
			this.listenThread = this.threadFactory.newThread(this::acceptConnections);
			this.listenThread.setDaemon(true);
			this.listenThread.setName("Live Reload Server");
			this.listenThread.start();
			return localPort;
		}
	}

	/**
	 * Return if the server has been started.
	 * @return {@code true} if the server is running
	 */
	public boolean isStarted() {
		synchronized (this.monitor) {
			return this.listenThread != null;
		}
	}

	/**
	 * Return the port that the server is listening on.
	 * @return the server port
	 */
	public int getPort() {
		return this.port;
	}

	private void acceptConnections() {
		do {
			try {
				Socket socket = this.serverSocket.accept();
				socket.setSoTimeout(READ_TIMEOUT);
				this.executor.execute(new ConnectionHandler(socket));
			}
			catch (SocketTimeoutException ex) {
				// Ignore
			}
			catch (Exception ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("LiveReload server error", ex);
				}
			}
		}
		while (!this.serverSocket.isClosed());
	}

	/**
	 * Gracefully stop the livereload server.
	 * @throws IOException in case of I/O errors
	 */
	public void stop() throws IOException {
		synchronized (this.monitor) {
			if (this.listenThread != null) {
				closeAllConnections();
				try {
					this.executor.shutdown();
					this.executor.awaitTermination(1, TimeUnit.MINUTES);
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
				this.serverSocket.close();
				try {
					this.listenThread.join();
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
				this.listenThread = null;
				this.serverSocket = null;
			}
		}
	}

	private void closeAllConnections() throws IOException {
		synchronized (this.connections) {
			for (Connection connection : this.connections) {
				connection.close();
			}
		}
	}

	/**
	 * Trigger livereload of all connected clients.
	 */
	public void triggerReload() {
		synchronized (this.monitor) {
			synchronized (this.connections) {
				for (Connection connection : this.connections) {
					try {
						connection.triggerReload();
					}
					catch (Exception ex) {
						logger.debug("Unable to send reload message", ex);
					}
				}
			}
		}
	}

	private void addConnection(Connection connection) {
		synchronized (this.connections) {
			this.connections.add(connection);
		}
	}

	private void removeConnection(Connection connection) {
		synchronized (this.connections) {
			this.connections.remove(connection);
		}
	}

	/**
	 * Factory method used to create the {@link Connection}.
	 * @param socket the source socket
	 * @param inputStream the socket input stream
	 * @param outputStream the socket output stream
	 * @return a connection
	 * @throws IOException in case of I/O errors
	 */
	protected Connection createConnection(Socket socket, InputStream inputStream, OutputStream outputStream)
			throws IOException {
		return new Connection(socket, inputStream, outputStream);
	}

	/**
	 * {@link Runnable} to handle a single connection.
	 *
	 * @see Connection
	 */
	private class ConnectionHandler implements Runnable {

		private final Socket socket;

		private final InputStream inputStream;

		ConnectionHandler(Socket socket) throws IOException {
			this.socket = socket;
			this.inputStream = socket.getInputStream();
		}

		@Override
		public void run() {
			try {
				handle();
			}
			catch (ConnectionClosedException ex) {
				logger.debug("LiveReload connection closed");
			}
			catch (Exception ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("LiveReload error", ex);
				}
			}
		}

		private void handle() throws Exception {
			try {
				try (OutputStream outputStream = this.socket.getOutputStream()) {
					Connection connection = createConnection(this.socket, this.inputStream, outputStream);
					runConnection(connection);
				}
				finally {
					this.inputStream.close();
				}
			}
			finally {
				this.socket.close();
			}
		}

		private void runConnection(Connection connection) throws Exception {
			try {
				addConnection(connection);
				connection.run();
			}
			finally {
				removeConnection(connection);
			}
		}

	}

	/**
	 * {@link ThreadFactory} to create the worker threads.
	 */
	private static class WorkerThreadFactory implements ThreadFactory {

		private final AtomicInteger threadNumber = new AtomicInteger(1);

		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r);
			thread.setDaemon(true);
			thread.setName("Live Reload #" + this.threadNumber.getAndIncrement());
			return thread;
		}

	}

}
