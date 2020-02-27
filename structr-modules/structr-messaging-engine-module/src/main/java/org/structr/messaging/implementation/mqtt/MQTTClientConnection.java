/**
 * Copyright (C) 2010-2020 Structr GmbH
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

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;

public class MQTTClientConnection implements MqttCallback {
	private MemoryPersistence persistence = new MemoryPersistence();
	private MqttConnectOptions connOpts;
	private MqttClient client;
	private MQTTInfo info;

	private static final Logger	logger = LoggerFactory.getLogger(MQTTClientConnection.class.getName());

	public MQTTClientConnection(MQTTInfo info) throws MqttException{

		this.info = info;
		String broker = info.getProtocol() + info.getUrl() + ":" + info.getPort();
		client = new MqttClient(broker, info.getUuid(), persistence);
		client.setCallback(this);
		connOpts = new MqttConnectOptions();
		connOpts.setCleanSession(true);
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

		try{

			if(client.isConnected()){

				client.disconnect();
				info.connectionStatusCallback(false);
			}
		} catch (MqttException ex) {

			throw new FrameworkException(422, "Error while disconnecting from MQTT broker.");
		}
	}

	public boolean isConnected(){

		return client.isConnected();
	}

	public void sendMessage(String topic, String message) throws FrameworkException {

		try{

			if(client.isConnected()){

				MqttMessage msg = new MqttMessage(message.getBytes());

				msg.setQos(info.getQos());

				client.publish(topic, msg);
			}
		} catch (MqttException ex) {

			throw new FrameworkException(422, "Error while sending message.");
		}

	}

	public void subscribeTopic(String topic) throws FrameworkException {

		try{

			if(client.isConnected()){

				client.subscribe(topic, info.getQos());

			}
		} catch (MqttException ex) {

			throw new FrameworkException(422, "Error while subscribing to topic.");
		}

	}

	public void unsubscribeTopic(String topic) throws FrameworkException {

		try{

		if(client.isConnected()){

			client.unsubscribe(topic);
		}

		} catch (MqttException ex) {

			throw new FrameworkException(422, "Error while unsubscribing from topic.");
		}

	}

	@Override
	public void messageArrived(String topic, MqttMessage msg) throws Exception {

		Thread workerThread = new Thread(new CallbackWorker(info, topic, msg.toString()));
		workerThread.start();
	}

	@Override
	public void connectionLost(Throwable cause) {
		try{

			info.connectionStatusCallback(false);
			connect();
			MQTTContext.subscribeAllTopics(info);
		} catch (FrameworkException ex) {

			try {

				logger.warn("Removing faulty connection from MQTTContext.");
				MQTTContext.disconnect(info);
			} catch (FrameworkException ex1) {

				logger.error("Could not remove connection from MQTTContext.");
			}
			logger.error("Could not reconnect to MQTT broker.");
		}
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
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
				info.messageCallback(topic, message);
			} catch (FrameworkException e) {
				logger.error("Error during MQTT message callback: " + e.getMessage());
			}

		}

	}

}
