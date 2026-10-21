/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.messaging;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

/**
 * A Mosquitto MQTT broker in a test container, plus a client for the test side that publishes and receives messages
 * independently of Structr. Started once per test class, hard-fails when Docker is not available.
 */
final class MosquittoTestSupport {

	private static GenericContainer<?> broker = null;
	private static String brokerURL           = null;

	private MosquittoTestSupport() {
	}

	static synchronized void start() {

		if (broker != null) {

			return;
		}

		broker = new GenericContainer<>("eclipse-mosquitto:2.0.20")
			.withExposedPorts(1883)
			.withCommand("mosquitto", "-c", "/mosquitto-no-auth.conf")
			.waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));

		broker.start();

		brokerURL = "tcp://" + broker.getHost() + ":" + broker.getMappedPort(1883);
	}

	static synchronized void stop() {

		if (broker != null) {

			broker.stop();

			broker    = null;
			brokerURL = null;
		}
	}

	static String getBrokerURL() {

		return brokerURL;
	}

	/**
	 * Connects a client for the test side with a clean session, so that it receives only what is published while it is
	 * connected.
	 */
	static MqttClient connectTestClient() throws MqttException {

		final MqttClient client              = new MqttClient(brokerURL, MqttClient.generateClientId(), new MemoryPersistence());
		final MqttConnectOptions connOptions = new MqttConnectOptions();

		connOptions.setCleanSession(true);

		client.connect(connOptions);

		return client;
	}

	/**
	 * @return a message with QoS 1 and the UTF-8 bytes of the given payload
	 */
	static MqttMessage message(final String payload) {

		final MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));

		message.setQos(1);

		return message;
	}

	static String uniqueTopic() {

		return "structr-test/" + UUID.randomUUID();
	}
}
