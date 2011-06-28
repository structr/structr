/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.cloud;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The root listener for all cloud service connections. This listener can be
 * added to any server or client to enable cloud service transmissions on that
 * connection.
 *
 * @author Christian Morgner
 */
public class CloudServiceListener extends Listener {

	private final Map<Connection, ConnectionListener> listeners = Collections.synchronizedMap(new WeakHashMap<Connection, ConnectionListener>());

	@Override
	public void received(Connection connection, Object object) {

		ConnectionListener listener = listeners.get(connection);
		if(listener != null) {
			listener.received(connection, object);
		}
	}

	@Override
	public void connected(Connection connection) {

		// create and start a new connection listener for this connection
		listeners.put(connection, new ConnectionListener(connection));

	}

	@Override
	public void disconnected(Connection connection) {

		ConnectionListener listener = listeners.get(connection);
		if(listener != null) {
			listeners.remove(connection);
		}
	}
}
