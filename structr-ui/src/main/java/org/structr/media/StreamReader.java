package org.structr.media;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author Christian Morgner
 */
public class StreamReader extends Thread {

	private final Queue<String> queue = new ConcurrentLinkedQueue<>();
	private BufferedReader reader     = null;
	private AtomicBoolean running     = null;

	public StreamReader(final InputStream is, final AtomicBoolean running) {

		super("StreamReader");

		this.reader  = new BufferedReader(new InputStreamReader(is));
		this.running = running;

		this.setDaemon(true);
	}

	public String getBuffer() {

		final StringBuilder buf = new StringBuilder();
		for (final String line : queue) {

			buf.append(line);
			buf.append("\n");
		}

		return buf.toString();
	}

	@Override
	public void run() {

		while (running.get()) {

			try {

				String line = null;

				do {
					line = reader.readLine();
					if (line != null) {

						queue.add(line);
					}

				} while (line != null);

				Thread.sleep(10);

			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}
}
