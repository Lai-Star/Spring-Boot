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

package org.springframework.boot.actuate.jms;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.ConnectionMetaData;
import jakarta.jms.JMSException;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JmsHealthIndicator}.
 *
 * @author Stephane Nicoll
 */
class JmsHealthIndicatorTests {

	@Test
	void jmsBrokerIsUp() throws JMSException {
		ConnectionMetaData connectionMetaData = mock(ConnectionMetaData.class);
		given(connectionMetaData.getJMSProviderName()).willReturn("JMS test provider");
		Connection connection = mock(Connection.class);
		given(connection.getMetaData()).willReturn(connectionMetaData);
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		given(connectionFactory.createConnection()).willReturn(connection);
		JmsHealthIndicator indicator = new JmsHealthIndicator(connectionFactory);
		Health health = indicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("provider", "JMS test provider");
		then(connection).should().close();
	}

	@Test
	void jmsBrokerIsDown() throws JMSException {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		given(connectionFactory.createConnection()).willThrow(new JMSException("test", "123"));
		JmsHealthIndicator indicator = new JmsHealthIndicator(connectionFactory);
		Health health = indicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).doesNotContainKey("provider");
	}

	@Test
	void jmsBrokerCouldNotRetrieveProviderMetadata() throws JMSException {
		ConnectionMetaData connectionMetaData = mock(ConnectionMetaData.class);
		given(connectionMetaData.getJMSProviderName()).willThrow(new JMSException("test", "123"));
		Connection connection = mock(Connection.class);
		given(connection.getMetaData()).willReturn(connectionMetaData);
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		given(connectionFactory.createConnection()).willReturn(connection);
		JmsHealthIndicator indicator = new JmsHealthIndicator(connectionFactory);
		Health health = indicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).doesNotContainKey("provider");
		then(connection).should().close();
	}

	@Test
	void jmsBrokerUsesFailover() throws JMSException {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		ConnectionMetaData connectionMetaData = mock(ConnectionMetaData.class);
		given(connectionMetaData.getJMSProviderName()).willReturn("JMS test provider");
		Connection connection = mock(Connection.class);
		given(connection.getMetaData()).willReturn(connectionMetaData);
		willThrow(new JMSException("Could not start", "123")).given(connection).start();
		given(connectionFactory.createConnection()).willReturn(connection);
		JmsHealthIndicator indicator = new JmsHealthIndicator(connectionFactory);
		Health health = indicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).doesNotContainKey("provider");
	}

	@Test
	void whenConnectionStartIsUnresponsiveStatusIsDown() throws JMSException {
		ConnectionMetaData connectionMetaData = mock(ConnectionMetaData.class);
		given(connectionMetaData.getJMSProviderName()).willReturn("JMS test provider");
		Connection connection = mock(Connection.class);
		UnresponsiveStartAnswer unresponsiveStartAnswer = new UnresponsiveStartAnswer();
		willAnswer(unresponsiveStartAnswer).given(connection).start();
		willAnswer((invocation) -> {
			unresponsiveStartAnswer.connectionClosed();
			return null;
		}).given(connection).close();
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		given(connectionFactory.createConnection()).willReturn(connection);
		JmsHealthIndicator indicator = new JmsHealthIndicator(connectionFactory);
		Health health = indicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat((String) health.getDetails().get("error")).contains("Connection closed");
	}

	private static final class UnresponsiveStartAnswer implements Answer<Void> {

		private boolean connectionClosed = false;

		private final Object monitor = new Object();

		@Override
		public Void answer(InvocationOnMock invocation) throws Throwable {
			synchronized (this.monitor) {
				while (!this.connectionClosed) {
					this.monitor.wait();
				}
			}
			throw new JMSException("Connection closed");
		}

		private void connectionClosed() {
			synchronized (this.monitor) {
				this.connectionClosed = true;
				this.monitor.notifyAll();
			}
		}

	}

}
