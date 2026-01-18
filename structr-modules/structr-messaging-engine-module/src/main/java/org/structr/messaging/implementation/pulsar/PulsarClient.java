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
package org.structr.messaging.implementation.pulsar;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.messaging.engine.entities.MessageClient;
import org.structr.messaging.traits.wrappers.PulsarClientTraitWrapper;

public interface PulsarClient extends MessageClient {

	boolean getEnabled();
	void setEnabled(final boolean enabled) throws FrameworkException;

	String[] getServers();
	void setup();
	void close();
	void forwardReceivedMessage(final SecurityContext securityContext, final String topic, final String message) throws FrameworkException;
	PulsarClientTraitWrapper.ConsumerWorker getConsumerWorker();
}
