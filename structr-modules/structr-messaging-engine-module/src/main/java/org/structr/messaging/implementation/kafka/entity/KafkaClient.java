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
package org.structr.messaging.implementation.kafka.entity;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.structr.common.error.FrameworkException;
import org.structr.messaging.engine.entities.MessageClient;

import java.util.Properties;

public interface KafkaClient extends MessageClient {

	String getGroupId();
	String[] getServers();
	void setServers(final String[] servers) throws FrameworkException;

	/*
	static {

		Services.getInstance().registerInitializationCallback(() -> {

			final App app = StructrApp.getInstance();

			try (final Tx tx = app.tx()) {

				for (final KafkaClient client : app.nodeQuery("KafkaClient").getAsList()) {
					client.setup();
				}

				tx.success();

			} catch (Throwable t) {
				final Logger logger = LoggerFactory.getLogger(KafkaClient.class);
				logger.error("Unable to initialize Kafka clients. " + t);
			}
		});
	}
	*/

	Producer<String, String> getProducer();
	void setProducer(final KafkaProducer<String, String> producer);
	void setup();
	void close();
	void refreshConfiguration();
	Properties getConfiguration(final Class clazz);
	void forwardReceivedMessage(final String topic, final String message) throws FrameworkException;
}
