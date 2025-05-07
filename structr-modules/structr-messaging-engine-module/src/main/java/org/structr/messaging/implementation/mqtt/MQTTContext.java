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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.messaging.implementation.mqtt.entity.MQTTClient;

import java.util.HashMap;
import java.util.Map;

public abstract class MQTTContext {

	private static final Logger logger                                 = LoggerFactory.getLogger(MQTTContext.class.getName());
	private static final Map<String, MQTTClientConnection> connections = new HashMap<>();

	static {

		Services.getInstance().registerInitializationCallback(() -> {

			if (Traits.exists(StructrTraits.MQTT_CLIENT)) {

				new Thread(new SubscriptionWorker()).start();
			}

		});
	}

	public static MQTTClientConnection getClientForId(final String id){
		return connections.get(id);
	}

	public static void disconnect(final String uuid) throws FrameworkException {

		MQTTClientConnection con = getClientForId(uuid);

		if (con != null){

			connections.remove(uuid);

			if (con.isConnected()) {

				con.disconnect();
			}
		}
	}

	public static void disconnect(final MQTTInfo info) throws FrameworkException {

		MQTTClientConnection con = getClientForId(info.getUuid());

		if (con != null){

			connections.remove(info.getUuid());

			if (con.isConnected()) {

				con.disconnect();
			}
		}
	}

	/**
	 * same as disconnect, but calls connection.delete to prevent updating the client while it is being deleted
	 */
	public static void delete(final String uuid) throws FrameworkException {

		MQTTClientConnection con = getClientForId(uuid);

		if (con != null){

			connections.remove(uuid);

			con.delete();
		}
	}

	public static void connect(final MQTTInfo info) throws FrameworkException {

		try {

			MQTTClientConnection con = getClientForId(info.getUuid());

			if (con == null){

				con = new MQTTClientConnection(info);
				connections.put(info.getUuid(), con);
				con.connect();

			} else {

				if (!con.isConnected()){

					con.connect();
				}
			}

		} catch (MqttException ex) {

			throw new FrameworkException(422, "Error while connecting to MQTT broker");
		}
	}

	public static void subscribeAllTopics(final MQTTInfo info) throws FrameworkException {

		MQTTClientConnection con = getClientForId(info.getUuid());

		for (String topic : info.getTopics()) {

			if (!StringUtils.isEmpty(topic)){

				con.subscribeTopic(topic);
			}
		}
	}

	private static class SubscriptionWorker implements Runnable {

		@Override
		public void run() {

			// wait for service layer to be initialized
			while (!Services.getInstance().isInitialized()) {
				try { Thread.sleep(1000); } catch(InterruptedException iex) { }
			}

			final App app = StructrApp.getInstance();

			try (final Tx tx = app.tx()) {

				for (final NodeInterface clientNode : app.nodeQuery(StructrTraits.MQTT_CLIENT).getAsList()) {

					MQTTClient client = clientNode.as(MQTTClient.class);

					client.setIsConnected(false);

					// enable clients on startup
					if (client.getEnabled()) {

						try {

							MQTTContext.connect(client);
							MQTTContext.subscribeAllTopics(client);
							client.connectionStatusCallback(true);

						} catch (FrameworkException ex) {

							client.setEnabled(false);
						}
					}
				}

				tx.success();

			} catch (Throwable t) {

				logger.warn("Could not connect to MQTT broker: {}", t.getMessage());
			}
		}
	}
}