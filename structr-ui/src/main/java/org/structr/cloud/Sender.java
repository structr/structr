package org.structr.cloud;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 *
 * @author Christian Morgner
 */
public class Sender extends Thread {

	private final Queue<Message> outputQueue = new ArrayBlockingQueue<>(1000);
	private ObjectOutputStream outputStream  = null;
	private boolean connected                = false;

	public Sender(final ObjectOutputStream outputStream) {
		this.outputStream = outputStream;
	}

	@Override
	public void run() {

		connected = true;

		while (connected) {

			try {

				final Message message = outputQueue.poll();
				if (message != null) {

					outputStream.writeObject(message);
					outputStream.flush();
				}

			} catch (IOException ioex) {

				connected = false;
			}
		}
	}

	public boolean isConnected() {
		return connected;
	}

	public void send(final Message message) {
		outputQueue.add(message);
	}

	public void finish() {

		try {
			outputStream.close();

		} catch (IOException ioex) {
			ioex.printStackTrace();
		}

		connected = false;
	}
}
