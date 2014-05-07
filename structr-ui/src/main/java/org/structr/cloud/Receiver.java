package org.structr.cloud;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 *
 * @author Christian Morgner
 */
public class Receiver extends Thread {

	private final Queue<Message> inputQueue = new ArrayBlockingQueue<>(1000);
	private ObjectInputStream inputStream   = null;
	private Throwable errorMessage          = null;
	private Socket socket                   = null;

	public Receiver(final Socket socket, final ObjectInputStream inputStream) {

		this.inputStream = inputStream;
		this.socket      = socket;
	}

	@Override
	public void run() {

		while (isConnected()) {

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

				finish();
			}
		}
	}

	public Throwable getErrorMessage() {
		return errorMessage;
	}

	public boolean isConnected() {
		return socket.isConnected() && !socket.isClosed();
	}

	public Message receive() {
		return inputQueue.poll();
	}

	public void finish() {

		try {

			socket.close();

		} catch (IOException ioex) { }
	}
}
