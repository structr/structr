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

import org.structr.common.error.FrameworkException;

public interface MQTTInfo {

	String getMainBrokerURL();
	String[] getFallbackBrokerURLs();
	int getQos();

	/**
	 * Whether the broker discards the session of this client when it connects. A persistent session (false) keeps the
	 * subscriptions and the messages with QoS 1 or 2 that arrive while the client is disconnected or that it has not
	 * acknowledged yet, so that the broker delivers them after a reconnect or a restart of Structr.
	 */
	boolean getCleanSession();
	String getUuid();
	String getUsername();
	String getPassword();

	String[] getTopics();

	void messageCallback(String topic, String message) throws FrameworkException;
	void connectionStatusCallback(boolean connected);
}