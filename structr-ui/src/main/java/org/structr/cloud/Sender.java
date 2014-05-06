package org.structr.cloud;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 *
 * @author Christian Morgner
 */
public class Sender extends Thread {

	private final Queue<Message> outputQueue = new ArrayBlockingQueue<>(1000);
	private ObjectOutputStream outputStream  = null;
	private Throwable errorMessage           = null;
	private Socket socket                    = null;

	public Sender(final Socket socket, final ObjectOutputStream outputStream) {

		this.socket       = socket;
		this.outputStream = outputStream;

		// flush stream to avoid ObjectInputStream to be waiting indefinitely
		try {
			outputStream.flush();

		} catch (IOException ioex) {
			ioex.printStackTrace();
		}
	}

	@Override
	public void run() {

		while (isConnected()) {

			try {

				final Message message = outputQueue.poll();
				if (message != null) {

					outputStream.writeObject(message);
					outputStream.flush();
				}

			} catch (IOException ioex) {

				errorMessage = ioex;

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

	public void send(final Message message) {
		outputQueue.add(message);
	}

	public void finish() {

		try {
			outputStream.close();

		} catch (IOException ioex) { }
	}
}
