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
package org.structr.cloud;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Logger;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.Tx;

/**
 *
 * @author Christian Morgner
 */
public class ClientConnection {

	// the logger
	private static final Logger logger = Logger.getLogger(ClientConnection.class.getName());

	// private fields
	private int timeout                 = 2000;
	private Receiver receiver           = null;
	private Sender sender               = null;
	private Socket socket               = null;
	private Tx tx                       = null;

	public ClientConnection(final Socket socket) {
		this.socket = socket;
	}

	public void start() {

		// setup read and write threads for the connection
		if (socket.isConnected() && !socket.isClosed()) {

			try {
				sender   = new Sender(new ObjectOutputStream(socket.getOutputStream()));
				receiver = new Receiver(new ObjectInputStream(socket.getInputStream()));

				receiver.start();
				sender.start();

			} catch (IOException ioex) {

				ioex.printStackTrace();
			}
		}
	}

	public void send(final Message message) {
		sender.send(message);
	}

	public void shutdown() {

		receiver.finish();
		sender.finish();
	}

	public Message waitForMessage() throws FrameworkException {

		final long abortTime = System.currentTimeMillis() + timeout;
		Message message      = null;

		while (message == null) {

			if (System.currentTimeMillis() > abortTime) {
				throw new FrameworkException(504, "Timeout while connecting to remote server.");
			}

			message = receiver.receive();

			try {

				Thread.sleep(1);

			} catch (Throwable t) {}
		}

		return message;
	}

	public void waitForClose(int timeout) {

		final long abortTime = System.currentTimeMillis() + timeout;

		while ((sender.isConnected() || receiver.isConnected()) && System.currentTimeMillis() < abortTime) {

			try {

				Thread.sleep(10);

			} catch (Throwable t) {}
		}

	}
}
