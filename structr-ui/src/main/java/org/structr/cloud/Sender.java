/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.cloud;

import java.io.DataOutputStream;
import org.structr.cloud.message.Message;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 *
 *
 */
public class Sender extends Thread {

	private final BlockingQueue<Message> outputQueue = new ArrayBlockingQueue<>(10000);
	private DataOutputStream outputStream            = null;
	private CloudConnection connection               = null;

	public Sender(final CloudConnection connection, final DataOutputStream outputStream) {

		super("Sender of " + connection.getName());
		this.setDaemon(true);

		this.outputStream = outputStream;
		this.connection   = connection;

		// flush stream to avoid ObjectInputStream to be waiting indefinitely
		try {

			outputStream.flush();

		} catch (IOException ioex) {
			ioex.printStackTrace();
		}
	}

	@Override
	public void run() {

		while (connection.isConnected()) {

			try {

				while (!outputQueue.isEmpty()) {

					final Message message = outputQueue.poll();
					if (message != null) {

						message.serialize(outputStream);
						message.afterSend(connection);
					}
				}

				outputStream.flush();

			} catch (Throwable t) {

				connection.close();
			}

			try { Thread.yield(); } catch (Throwable t) {}
		}
	}

	public void send(final Message message) {

		try {
			outputQueue.put(message);

		} catch (InterruptedException iex) {
			iex.printStackTrace();
		}
	}
}
