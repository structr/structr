/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.cloud.message;

import java.io.IOException;
import java.io.Serializable;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.ExportContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeServiceCommand;

/**
 *
 * @author Christian Morgner
 */
public abstract class Message<T> implements Serializable {

	private String id = NodeServiceCommand.getNextUuid();

	public abstract void onRequest(final CloudConnection serverConnection, final ExportContext context) throws IOException, FrameworkException;
	public abstract void onResponse(final CloudConnection clientConnection, final ExportContext context) throws IOException, FrameworkException;
	public abstract void afterSend(final CloudConnection connection);

	public abstract T getPayload();

	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + getId() + ")";
	}

	protected void setId(final String id) {
		this.id = id;
	}

	protected Ack ack() {

		// create ack for this packet
		final Ack ack = new Ack();
		ack.setId(this.getId());

		return ack;
	}
}
