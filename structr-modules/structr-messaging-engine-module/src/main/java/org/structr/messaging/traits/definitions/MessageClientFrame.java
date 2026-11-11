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
package org.structr.messaging.traits.definitions;

import org.structr.common.trace.EntryFrame;

import java.util.Map;

/**
 * The evaluation of a messaging client's own setting, e.g. the group id expression of a Kafka client,
 * which its consumer thread evaluates outside of any message.
 */
public class MessageClientFrame extends EntryFrame {

	private final String clientType;
	private final String clientId;

	public MessageClientFrame(final String setting, final String clientType, final String clientId) {

		super(setting, null);

		this.clientType = clientType;
		this.clientId   = clientId;
	}

	@Override
	public String kind() {
		return "MESSAGE_CLIENT";
	}

	@Override
	public String describe() {
		return getName() + " of " + clientType + "[" + clientId + "]";
	}

	@Override
	public Map<String, Object> toMap() {

		final Map<String, Object> map = super.toMap();

		map.put("client", clientId);

		return map;
	}
}
