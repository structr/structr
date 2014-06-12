package org.structr.cloud;

import org.structr.cloud.message.Message;
import java.io.ObjectInputStream;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 *
 * @author Christian Morgner
 */
public class Receiver extends Thread {

	private final Queue<Message> inputQueue = new ArrayBlockingQueue<>(10000);
	private ObjectInputStream inputStream   = null;
	private CloudConnection connection      = null;

	public Receiver(final CloudConnection connection, final ObjectInputStream inputStream) {

		super("Receiver of " + connection.getName());
		this.setDaemon(true);

		this.inputStream = inputStream;
		this.connection  = connection;
	}

	@Override
	public void run() {

		while (connection.isConnected()) {

			try {

				final Message message = (Message)inputStream.readObject();
				if (message != null) {

					inputQueue.add(message);
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
