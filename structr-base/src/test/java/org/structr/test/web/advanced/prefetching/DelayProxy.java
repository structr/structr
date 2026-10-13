/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.test.web.advanced.prefetching;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.LockSupport;

/**
 * A TCP proxy that delays everything it forwards, to measure Structr against a database on another host.
 *
 * <p>Every chunk of data is forwarded half the round-trip time after it arrived, in both directions, so a query
 * that takes one round trip takes the configured time longer. Chunks are queued and not forwarded one by one, so
 * the proxy adds latency, not a limit on the throughput: a large result streams through at full speed, just later.
 *
 * <p>The delay can be changed at any time and applies to the data that arrives afterwards, so one database
 * connection can be measured at several latencies.
 */
final class DelayProxy {

	private final ServerSocket serverSocket;
	private final String targetHost;
	private final int targetPort;

	private volatile long oneWayDelayNanos = 0L;

	DelayProxy(final String targetHost, final int targetPort) throws IOException {

		this.targetHost   = targetHost;
		this.targetPort   = targetPort;
		this.serverSocket = new ServerSocket();

		serverSocket.bind(new InetSocketAddress("localhost", 0));

		daemon("DelayProxy accept", this::accept).start();
	}

	int getPort() {

		return serverSocket.getLocalPort();
	}

	/**
	 * Sets the time that is added to each round trip.
	 */
	void setRoundTripMillis(final double millis) {

		oneWayDelayNanos = (long) (millis * 500_000.0);
	}

	// ----- private methods -----
	private void accept() {

		while (!serverSocket.isClosed()) {

			try {

				final Socket client = serverSocket.accept();
				final Socket server = new Socket(targetHost, targetPort);

				client.setTcpNoDelay(true);
				server.setTcpNoDelay(true);

				forward(client, server);
				forward(server, client);

			} catch (IOException ioex) {

				if (!serverSocket.isClosed()) {

					ioex.printStackTrace();
				}
			}
		}
	}

	/**
	 * Forwards everything from one socket to the other, each chunk after the delay that was set when it arrived.
	 */
	private void forward(final Socket from, final Socket to) {

		final BlockingQueue<Chunk> queue = new LinkedBlockingQueue<>();

		daemon("DelayProxy read", () -> {

			try (final InputStream in = from.getInputStream()) {

				final byte[] buffer = new byte[65536];
				int count;

				while ((count = in.read(buffer)) >= 0) {

					final byte[] data = new byte[count];

					System.arraycopy(buffer, 0, data, 0, count);

					queue.add(new Chunk(data, System.nanoTime() + oneWayDelayNanos));
				}

			} catch (IOException ignore) {

				// the connection was closed

			} finally {

				queue.add(Chunk.END);
			}

		}).start();

		daemon("DelayProxy write", () -> {

			try (final OutputStream out = to.getOutputStream()) {

				while (true) {

					final Chunk chunk = queue.take();
					if (chunk == Chunk.END) {

						break;
					}

					long wait;

					while ((wait = chunk.due() - System.nanoTime()) > 0) {

						LockSupport.parkNanos(wait);
					}

					out.write(chunk.data());
					out.flush();
				}

			} catch (IOException | InterruptedException ignore) {

				// the connection was closed

			} finally {

				close(from);
				close(to);
			}

		}).start();
	}

	private static void close(final Socket socket) {

		try {

			socket.close();

		} catch (IOException ignore) {
		}
	}

	private static Thread daemon(final String name, final Runnable runnable) {

		final Thread thread = new Thread(runnable, name);

		thread.setDaemon(true);

		return thread;
	}

	private record Chunk(byte[] data, long due) {

		private static final Chunk END = new Chunk(new byte[0], 0L);
	}
}
