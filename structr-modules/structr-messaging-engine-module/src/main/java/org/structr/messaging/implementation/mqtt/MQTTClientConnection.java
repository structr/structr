/*
 * Copyright (C) 2010-2024 Structr GmbH
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
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;

import java.util.ArrayList;
import java.util.List;

public class MQTTClientConnection implements MqttCallback, MqttCallbackExtended {
	private final MemoryPersistence persistence = new MemoryPersistence();
	private final MqttConnectOptions connOpts;
	private final MqttClient client;
	private final MQTTInfo info;

	private static final Logger	logger = LoggerFactory.getLogger(MQTTClientConnection.class.getName());

	public MQTTClientConnection(MQTTInfo info) throws MqttException{

		this.info = info;

		client = new MqttClient(info.getMainBrokerURL(), info.getUuid(), persistence);
		client.setCallback(this);
		connOpts = new MqttConnectOptions();

		if (info.getFallbackBrokerURLs() != null) {

			List<String> fallBackBrokers;
			if (info.getMainBrokerURL() != null) {

				List<String> mergedBrokers = new ArrayList<>();
				mergedBrokers.add(info.getMainBrokerURL());
				mergedBrokers.addAll(info.getFallbackBrokerURLs());
				fallBackBrokers = mergedBrokers;

			} else {

				fallBackBrokers = info.getFallbackBrokerURLs();
			}

			connOpts.setServerURIs(fallBackBrokers.toArray(new String[0]));
		}

		connOpts.setCleanSession(true);

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

	public void sendMessage(String topic, String message) throws FrameworkException {

		try {

			if (client.isConnected()){

				MqttMessage msg = new MqttMessage(message.getBytes());

				msg.setQos(info.getQos());

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

			Thread workerThread = new Thread(new CallbackWorker(info, topic, msg.toString()));
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
		private final String message;

		public CallbackWorker(MQTTInfo info, String topic, String message){
			this.info = info;
			this.topic = topic;
			this.message = message;
		}

		@Override
		public void run() {

			try {

				if (!Services.getInstance().isShuttingDown() && !Services.getInstance().isShutdownDone()) {
					info.messageCallback(topic, message);
				}

			} catch (FrameworkException e) {

				logger.error("Error during MQTT message callback: " + e.getMessage());
			}
		}
	}
}
