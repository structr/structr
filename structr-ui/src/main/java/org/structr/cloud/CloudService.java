/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.cloud;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.StructrConf;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.RunnableService;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;

/**
 * The cloud service handles networking between structr instances
 *
 * @author axel
 */
public class CloudService extends Thread implements RunnableService {

	private static final Logger logger = Logger.getLogger(CloudService.class.getName());

	public static final int CHUNK_SIZE        = 65536;
	public static final int BUFFER_SIZE       = CHUNK_SIZE * 4;
	public static final int LIVE_PACKET_COUNT = 19;

	public static final boolean DEBUG         = true;
	public static final String STREAM_CIPHER  = "RC4";

	private final static int DefaultTcpPort   = 54555;

	private ServerSocket serverSocket = null;
	private boolean running           = false;
	private int tcpPort               = DefaultTcpPort;

	public CloudService() {
		super("CloudService");
	}

	@Override
	public void injectArguments(Command command) {
	}

	@Override
	public void initialize(StructrConf config) {
		tcpPort = Integer.parseInt(config.getProperty(Services.TCP_PORT, "54555"));
	}

	@Override
	public void shutdown() {
		running = false;
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	public void startService() {

		try {

			serverSocket = new ServerSocket(tcpPort);

			running = true;
			start();

			logger.log(Level.INFO, "CloudService successfully started.");

		} catch (IOException ioex) {

			logger.log(Level.WARNING, "Unable to start CloudService." , ioex);
		}
	}

	@Override
	public void run() {

		while (running) {

			try {

				// start a new thread for the connection
				new ServerConnection(serverSocket.accept()).start();

			} catch (IOException ioex) {

				ioex.printStackTrace();
			}
		}
	}

	@Override
	public void stopService() {
		shutdown();
	}

	@Override
	public boolean runOnStartup() {
		return true;
	}

	// ----- public static methods -----
	public static <T> T doRemote(final CloudTransmission<T> transmission, final CloudListener listener) throws FrameworkException {

		final String userName       = transmission.getUserName();
		final String password       = transmission.getPassword();
		final String remoteHost     = transmission.getRemoteHost();
		final int remoteTcpPort     = transmission.getRemotePort();
		final ExportContext context = new ExportContext(listener, 4);
		ClientConnection client     = null;
		T remoteResult              = null;

		try (final Tx tx = StructrApp.getInstance().tx()) {

			client = new ClientConnection(new Socket(remoteHost, remoteTcpPort));

			// notify context of increased message stack size
			context.increaseTotal(transmission.getTotalSize());

			// notify listener
			context.transmissionStarted();

			client.start();

			// mark start of transaction
			client.send(new BeginPacket());
			context.progress();

			final Message ack = client.waitForMessage();
			if (!(ack instanceof AckPacket)) {
				throw new FrameworkException(504, "Unable to connect to remote server: unknown response.");
			}

			// send authentication container
			client.send(new AuthenticationContainer(userName));
			context.progress();

			// wait for authentication container reply from server
			// to enable encryption
			final Message authMessage = client.waitForMessage();
			if (authMessage instanceof AuthenticationContainer) {

				final AuthenticationContainer auth = (AuthenticationContainer)authMessage;
				client.setEncryptionKey(auth.getEncryptionKey(password));

				// do transmission in an authenticated and encrypted context
				remoteResult = transmission.doRemote(client, context);

			} else {

				// notify listener
				if (context != null) {
					context.transmissionAborted();
				}
			}

			// mark end of transaction
			client.send(new EndPacket());
			context.progress();

			// wait for server to close connection here..
			client.waitForClose(2000);
			client.shutdown();

			// notify listener
			if (context != null) {
				context.transmissionFinished();
			}

		} catch (IOException | InvalidKeyException ioex) {

			throw new FrameworkException(504, "Unable to connect to remote server: " + ioex.getMessage());

		} finally {

			if (client != null) {
				client.shutdown();
			}
		}

		return remoteResult;
	}

	public static byte[] trimToSize(final byte[] source, final int maxKeyLengthBits) {

		if (maxKeyLengthBits < Integer.MAX_VALUE) {
			return Arrays.copyOfRange(source, 0, Math.min(source.length, maxKeyLengthBits / 8));
		}

		return source;
	}
}
