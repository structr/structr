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

	private final Queue<Message> inputQueue = new ArrayBlockingQueue<>(1000);
	private ObjectInputStream inputStream   = null;
	private CloudConnection connection      = null;
	private Throwable errorMessage          = null;

	public Receiver(final CloudConnection connection, final ObjectInputStream inputStream) {
		this.inputStream = inputStream;
		this.connection  = connection;
	}

	@Override
	public void run() {

		while (connection.isConnected()) {

			try {

				final Message message = (Message)inputStream.readObject();
				if (message != null) {

					if (CloudService.DEBUG) {
						System.out.println("Receiver: " + message);
					}

					inputQueue.add(message);
				}

			} catch (Throwable t) {

				errorMessage = t;

				connection.shutdown();
			}
		}
	}

	public Throwable getErrorMessage() {
		return errorMessage;
	}

	public Message receive() {
		return inputQueue.poll();
	}
}
