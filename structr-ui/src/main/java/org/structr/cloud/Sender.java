package org.structr.cloud;

import org.structr.cloud.message.Message;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 *
 * @author Christian Morgner
 */
public class Sender extends Thread {

	private final Queue<Message> outputQueue = new ArrayBlockingQueue<>(10000);
	private ObjectOutputStream outputStream  = null;
	private CloudConnection connection       = null;
	private int messagesInFlight             = 0;

	public Sender(final CloudConnection connection, final ObjectOutputStream outputStream) {

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

			if (messagesInFlight < CloudService.LIVE_PACKET_COUNT) {

				try {

					final Message message = outputQueue.poll();
					if (message != null) {

						outputStream.writeObject(message);
						outputStream.flush();

						messagesInFlight++;

						message.afterSend(connection);
					}

				} catch (Throwable t) {

					connection.close();
				}

			} else {

				Thread.yield();
			}
		}
	}

	public void send(final Message message) {
		outputQueue.add(message);
	}

	public void messageReceived() {
		messagesInFlight--;
	}
}
