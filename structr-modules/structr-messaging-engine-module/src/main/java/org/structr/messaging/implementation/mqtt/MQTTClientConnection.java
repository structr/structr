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
package org.structr.messaging.implementation.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.RetryException;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MQTTClientConnection implements MqttCallback, MqttCallbackExtended {

	private MemoryPersistence persistence = new MemoryPersistence();
	private MqttConnectOptions connOpts;
	private MqttClient client;
	private MQTTInfo info;

	private static final Logger	logger = LoggerFactory.getLogger(MQTTClientConnection.class.getName());

	public MQTTClientConnection(MQTTInfo info) throws MqttException{

		this.info = info;

		client = new MqttClient(info.getMainBrokerURL(), info.getUuid(), persistence);
		client.setCallback(this);

		// a message is acknowledged only after its callbacks have committed, see CallbackWorker
		client.setManualAcks(true);

		connOpts = new MqttConnectOptions();

		if (info.getFallbackBrokerURLs() != null) {

			String[] fallBackBrokers;

			if (info.getMainBrokerURL() != null) {

				List<String> mergedBrokers = new ArrayList<>();
				mergedBrokers.add(info.getMainBrokerURL());
				mergedBrokers.addAll(List.of(info.getFallbackBrokerURLs()));
				fallBackBrokers = mergedBrokers.toArray(String[]::new);

			} else {

				fallBackBrokers = info.getFallbackBrokerURLs();
			}

			connOpts.setServerURIs(fallBackBrokers);
		}

		connOpts.setCleanSession(info.getCleanSession());

		connOpts.setAutomaticReconnect(true);
		connOpts.setMaxReconnectDelay(5);

		if (info.getUsername() != null && info.getPassword() != null) {

			connOpts.setUserName(info.getUsername());
			connOpts.setPassword(info.getPassword().toCharArray());
		}
	}

	public void connect() throws FrameworkException {

		try{

			if(!client.isConnected()){

				client.connect(connOpts);
				info.connectionStatusCallback(true);
			}

		} catch (MqttException ex) {

			throw new FrameworkException(422, "Could not connect to MQTT broker.");
		}
	}

	public void disconnect() throws FrameworkException {

		try {

			if (client.isConnected()){

				client.disconnect();
				info.connectionStatusCallback(false);
			}

		} catch (MqttException ex) {

			throw new FrameworkException(422, "Error while disconnecting from MQTT broker.");
		}
	}

	/**
	 * same as disconnect, but does not set the status afterwards because that results in an exception
	 */
	public void delete() throws FrameworkException {

		try {

			if (client.isConnected()){

				client.disconnect();
			}

		} catch (MqttException ex) {

			throw new FrameworkException(422, "Error while disconnecting from MQTT broker.");
		}
	}

	public boolean isConnected(){

		return client.isConnected();
	}

	/**
	 * Publishes a message. The QoS is passed in because this runs after the commit of the transaction that sent the
	 * message, where the client node cannot be read anymore.
	 */
	public void sendMessage(final String topic, final String message, final int qos) throws FrameworkException {

		try {

			if (client.isConnected()){

				MqttMessage msg = new MqttMessage(message.getBytes(StandardCharsets.UTF_8));

				msg.setQos(qos);

				client.publish(topic, msg);
			}

		} catch (MqttException ex) {

			throw new FrameworkException(422, "Error while sending message.");
		}
	}

	public void subscribeTopic(String topic) throws FrameworkException {

		try {

			if (client.isConnected()){

				client.subscribe(topic, info.getQos());
			}

		} catch (MqttException ex) {

			throw new FrameworkException(422, "Error while subscribing to topic.");
		}
	}

	public void unsubscribeTopic(String topic) throws FrameworkException {

		try {

			if (client.isConnected()){

				client.unsubscribe(topic);
			}

		} catch (MqttException ex) {

			throw new FrameworkException(422, "Error while unsubscribing from topic.");
		}
	}

	@Override
	public void messageArrived(String topic, MqttMessage msg) throws Exception {

		if (!Services.getInstance().isShuttingDown() && !Services.getInstance().isShutdownDone()) {

			Thread workerThread = new Thread(new CallbackWorker(info, topic, msg));
			workerThread.start();
		}
	}

	@Override
	public void connectionLost(Throwable cause) {

		info.connectionStatusCallback(false);
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
	}

	@Override
	public void connectComplete(boolean b, String s) {

		info.connectionStatusCallback(b);
	}

	private class CallbackWorker implements Runnable {

		private final MQTTInfo info;
		private final String topic;
		private final MqttMessage message;

		public CallbackWorker(MQTTInfo info, String topic, MqttMessage message){

			this.info = info;
			this.topic = topic;
			this.message = message;
		}

		@Override
		public void run() {

			try {

				if (!Services.getInstance().isShuttingDown() && !Services.getInstance().isShutdownDone()) {

					// the callbacks start the outermost transaction of this thread, so it can be repeated as a whole if
					// the database terminates it, e.g. to resolve a deadlock
					RetryException.repeat(() -> info.messageCallback(topic, new String(message.getPayload(), StandardCharsets.UTF_8)));
				}

			} catch (FrameworkException | RuntimeException e) {

				logger.error("Error during MQTT message callback: " + e.getMessage());
			}

			acknowledge();
		}

		/**
		 * Acknowledges the message once its callbacks have committed or failed for good, i.e. with an error or with a
		 * transaction that is still terminated after the last repetition, since delivering it again would most likely fail
		 * the same way. A message that arrives during the shutdown, or whose callbacks the shutdown interrupts, stays
		 * unacknowledged, so that the broker delivers it again after the restart if the session is persistent and the QoS
		 * is 1 or 2.
		 */
		private void acknowledge() {

			if (Services.getInstance().isShuttingDown() || Services.getInstance().isShutdownDone()) {

				return;
			}

			try {

				client.messageArrivedComplete(message.getId(), message.getQos());

			} catch (MqttException ex) {

				logger.warn("Unable to acknowledge MQTT message on topic {}: {}", topic, ex.getMessage());
			}
		}
	}
}
