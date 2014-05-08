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

import org.structr.cloud.message.Message;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.digest.DigestUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.Tx;

/**
 *
 * @author Christian Morgner
 */
public class ClientConnection implements CloudConnection {

	// the logger
	private static final Logger logger = Logger.getLogger(ClientConnection.class.getName());

	// private fields
	private final CloudContext context  = new CloudContext();
	private Cipher encrypter            = null;
	private Cipher decrypter            = null;
	private Receiver receiver           = null;
	private Sender sender               = null;
	private Socket socket               = null;
	private Tx tx                       = null;
	private int timeout                 = 2000;

	public ClientConnection(final Socket socket) {
		this.socket = socket;
	}

	public void start() {

		// setup read and write threads for the connection
		if (socket.isConnected() && !socket.isClosed()) {

			try {


				decrypter = Cipher.getInstance(CloudService.STREAM_CIPHER);
				encrypter = Cipher.getInstance(CloudService.STREAM_CIPHER);

				// this key is only used for the first two packets
				// of a transmission, it is replaced by the users
				// password hash afterwards.
				setEncryptionKey("StructrInitialEncryptionKey");

				sender   = new Sender(socket, new ObjectOutputStream(new GZIPOutputStream(new CipherOutputStream(socket.getOutputStream(), encrypter), true)));
				receiver = new Receiver(socket, new ObjectInputStream(new GZIPInputStream(new CipherInputStream(socket.getInputStream(), decrypter))));

				receiver.start();
				sender.start();

			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	public void send(final Message message) throws IOException {
		sender.send(message);
	}

	public void shutdown() {

		receiver.finish();
		sender.finish();

		context.endTransaction();
	}

	@Override
	public void closeConnection() {

		shutdown();

		try {

			socket.close();

		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public Message waitForMessage() throws FrameworkException {

		final long abortTime = System.currentTimeMillis() + timeout;
		Message message      = null;

		while (message == null) {

			message = receiver.receive();

			if (message == null) {

				if (System.currentTimeMillis() > abortTime) {

					final Throwable error = receiver.getErrorMessage();
					if (error != null) {

						if ("Unexpected end of ZLIB input stream".equals(error.getMessage())) {
							throw new FrameworkException(504, "Wrong user name or password.");
						}

						if ("invalid distance too far back".equals(error.getMessage())) {
							throw new FrameworkException(504, "Transmission failed, please try again.");
						}

						throw new FrameworkException(504, error.getMessage());

					} else {

						throw new FrameworkException(504, "Timeout while connecting to remote server.");
					}
				}

				try { Thread.sleep(1); } catch (Throwable t) {}
			}
		}

		return message;
	}

	public void waitForClose(int timeout) {

		final long abortTime = System.currentTimeMillis() + timeout;

		while ((sender.isConnected() || receiver.isConnected()) && System.currentTimeMillis() < abortTime) {

			try {

				Thread.sleep(10);

			} catch (Throwable t) {}
		}

	}

	@Override
	public void setEncryptionKey(final String key) throws InvalidKeyException {

		try {
			final int maxKeyLen    = Cipher.getMaxAllowedKeyLength(CloudService.STREAM_CIPHER);
			SecretKeySpec skeySpec = new SecretKeySpec(CloudService.trimToSize(DigestUtils.sha256(key), maxKeyLen), CloudService.STREAM_CIPHER);

			logger.log(Level.INFO, "Maximum allowed key size for stream encryption cipher {0}: {1}", new Object[] { CloudService.STREAM_CIPHER, maxKeyLen } );

			decrypter.init(Cipher.DECRYPT_MODE, skeySpec);
			encrypter.init(Cipher.ENCRYPT_MODE, skeySpec);

		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}
