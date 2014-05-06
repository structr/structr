package org.structr.cloud;

import java.io.IOException;
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
	private boolean connected               = false;

	public Receiver(final ObjectInputStream inputStream) {
		this.inputStream = inputStream;
	}

	@Override
	public void run() {

		connected = true;

		while (connected) {

			try {

				final Message message = (Message)inputStream.readObject();
				if (message != null) {

					inputQueue.add(message);
				}

			} catch (ClassNotFoundException cnfex) {

				cnfex.printStackTrace();

			} catch (IOException ioex) {

				connected = false;
			}
		}
	}

	public boolean isConnected() {
		return connected;
	}

	public Message receive() {
		return inputQueue.poll();
	}

	public void finish() {

		try {
			inputStream.close();

		} catch (IOException ioex) {
			ioex.printStackTrace();
		}

		connected = false;
	}
}
