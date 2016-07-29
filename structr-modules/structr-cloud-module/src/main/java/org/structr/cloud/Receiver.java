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

import java.io.DataInputStream;
import org.structr.cloud.message.Message;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 *
 *
 */
public class Receiver extends Thread {

	private final BlockingQueue<Message> inputQueue = new ArrayBlockingQueue<>(10000);
	private DataInputStream inputStream             = null;
	private CloudConnection connection              = null;

	public Receiver(final CloudConnection connection, final DataInputStream inputStream) {

		super("Receiver of " + connection.getName());
		this.setDaemon(true);

		this.inputStream = inputStream;
		this.connection  = connection;
	}

	@Override
	public void run() {

		while (connection.isConnected()) {

			try {

				final Message message = Message.deserialize(inputStream);
				if (message != null) {

					inputQueue.put(message);
				}

			} catch (Throwable t) {
				connection.close();
			}
		}
	}

	public Message receive() {
		return inputQueue.poll();
	}
}
