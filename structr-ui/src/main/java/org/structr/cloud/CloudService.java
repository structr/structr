/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.cloud;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import org.structr.cloud.message.AuthenticationRequest;
import org.structr.cloud.message.Begin;
import org.structr.cloud.message.End;
import org.structr.common.StructrConf;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.RunnableService;
import org.structr.core.Services;

/**
 * The cloud service handles networking between structr instances
 *
 * @author Axel Morgner
 * @author Christian Morgner
 */
public class CloudService extends Thread implements RunnableService {

	private static final Logger logger        = Logger.getLogger(CloudService.class.getName());
	private static final int DefaultTcpPort   = 54555;

	/**
	 * The CloudService protocol version. Change this when adding new
	 * fields etc., the protocol only works with the exact same
	 * counterpart.
	 */
	public static final int PROTOCOL_VERSION  = 4;

	public static final int CHUNK_SIZE        = 65536;
	public static final int BUFFER_SIZE       = CHUNK_SIZE * 4;
	public static final int LIVE_PACKET_COUNT = 200;
	public static final long AUTH_TIMEOUT     = 500;
	public static final long DEFAULT_TIMEOUT  = 2000;
	public static final boolean DEBUG         = false;
	public static final String STREAM_CIPHER  = "RC4";


	private ServerSocket serverSocket = null;
	private boolean running           = false;
	private int tcpPort               = DefaultTcpPort;

	public CloudService() {

		super("CloudService");
		this.setDaemon(true);
	}

	@Override
	public void injectArguments(Command command) {
	}

	@Override
	public void initialize(StructrConf config) {
		tcpPort = Integer.parseInt(config.getProperty(Services.TCP_PORT, "54555"));
	}

	@Override
	public void initialized() {}

	@Override
	public void shutdown() {

		try {

			serverSocket.close();

		} catch (Throwable t) {}

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
				new CloudConnection(serverSocket.accept(), new ExportContext(null, 0)).start();

			} catch (IOException ioex) {}
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


	@Override
	public boolean isVital() {
		return false;
	}

	// ----- public static methods -----
	public static <T> T doRemote(final CloudTransmission<T> transmission, final String userName, final String password, final String remoteHost, final int port, final CloudListener listener) throws FrameworkException {

		final ExportContext context = new ExportContext(listener, 3);
		CloudConnection<T> client   = null;
		int maxKeyLen               = 128;
		T remoteResult              = null;

		// obtain max. encryption key length
		try {
			maxKeyLen = Cipher.getMaxAllowedKeyLength(CloudService.STREAM_CIPHER);
		} catch (NoSuchAlgorithmException nsaex) {
			nsaex.printStackTrace();
		}

		try {

			client = new CloudConnection(new Socket(remoteHost, port), context);
			client.start();

			// notify context of increased message stack size
			context.increaseTotal(transmission.getTotalSize());

			// notify listener
			context.transmissionStarted();

			// mark start of transaction
			client.send(new Begin());
			context.progress();

			// store password in client
			client.setPassword(password);

			// send authentication container
			client.send(new AuthenticationRequest(userName, maxKeyLen));
			context.progress();

			client.waitForAuthentication();

			// do transmission in an authenticated and encrypted context
			remoteResult = transmission.doRemote(client);

			// mark end of transaction
			final End endPacket = new End();
			client.send(endPacket);

			// wait for server to close connection here..
			client.waitForClose(2000);
			client.close();

			// notify listener
			context.transmissionFinished();

		} catch (IOException  ioex) {

			ioex.printStackTrace();

			throw new FrameworkException(504, "Unable to connect to remote server: " + ioex.getMessage());

		} finally {

			if (client != null) {
				client.close();
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
