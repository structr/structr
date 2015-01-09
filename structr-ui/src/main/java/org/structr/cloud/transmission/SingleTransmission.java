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
package org.structr.cloud.transmission;

import java.io.IOException;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.CloudTransmission;
import org.structr.cloud.message.Message;
import org.structr.common.error.FrameworkException;

/**
 *
 * @author Christian Morgner
 */
public class SingleTransmission<T> implements CloudTransmission<T> {

	private Message<T> packet = null;

	public SingleTransmission(final Message<T> packet) {

		this.packet = packet;
	}

	@Override
	public int getTotalSize() {
		return 1;
	}

	@Override
	public T doRemote(final CloudConnection<T> client) throws IOException, FrameworkException {

		client.send(packet);

		client.waitForTransmission();

		return client.getPayload();
	}
}
