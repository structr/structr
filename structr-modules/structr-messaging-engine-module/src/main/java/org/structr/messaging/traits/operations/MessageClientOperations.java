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
package org.structr.messaging.traits.operations;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.messaging.engine.entities.MessageClient;
import org.structr.rest.RestMethodResult;

public abstract class MessageClientOperations extends FrameworkMethod<MessageClientOperations> {

	public abstract RestMethodResult sendMessage(final SecurityContext securityContext, final MessageClient client, final String topic, final String message) throws FrameworkException;
	public abstract RestMethodResult subscribeTopic(final SecurityContext securityContext, final MessageClient client, String topic) throws FrameworkException;
	public abstract RestMethodResult unsubscribeTopic(final SecurityContext securityContext, final MessageClient client, String topic) throws FrameworkException;
}
