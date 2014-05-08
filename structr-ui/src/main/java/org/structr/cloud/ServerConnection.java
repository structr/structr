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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import org.structr.cloud.message.Message;
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

/**
 *
 * @author Christian Morgner
 */
public class ServerConnection extends Thread implements CloudConnection {

	// the logger
	private static final Logger logger = Logger.getLogger(ServerConnection.class.getName());

	private final CloudContext context = new CloudContext();
	private Cipher encrypter            = null;
	private Cipher decrypter            = null;
	private Receiver receiver           = null;
	private Sender sender               = null;
	private Socket socket               = null;

	public ServerConnection(final Socket socket) {

		this.socket = socket;

		logger.log(Level.INFO, "CloudService: New connection from {0}", socket.getRemoteSocketAddress());
	}

	@Override
	public void start() {

		// setup read and write threads for the connection
		if (socket.isConnected() && !socket.isClosed()) {

			try {

				decrypter = Cipher.getInstance(CloudService.STREAM_CIPHER);
				encrypter = Cipher.getInstance(CloudService.STREAM_CIPHER);

				// this key is only used for the first two packets
				// of a transmission, it is replaced by the users
				// password hash afterwards.
				setEncryptionKey("StructrInitialEncryptionKey", 128);

				sender   = new Sender(this, new ObjectOutputStream(new GZIPOutputStream(new CipherOutputStream(new BufferedOutputStream(socket.getOutputStream()), encrypter), true)));
				receiver = new Receiver(this, new ObjectInputStream(new GZIPInputStream(new CipherInputStream(new BufferedInputStream(socket.getInputStream()), decrypter))));

				receiver.start();
				sender.start();

				// start thread
				super.start();

			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	@Override
	public void run() {

		while (isConnected()) {

			try {

				final Message request = receiver.receive();
				if (request != null) {

					final Message response = request.process(this, context);
					if (response != null) {

						sender.send(response);
					}
				}

				Thread.sleep(10);

			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

		shutdown();
	}

	@Override
	public void shutdown() {

		try {

			socket.close();

		} catch (Throwable t) {
			t.printStackTrace();
		}

		context.endTransaction();
	}

	@Override
	public void setEncryptionKey(final String key, final int keyLength) throws InvalidKeyException {

		try {
			SecretKeySpec skeySpec = new SecretKeySpec(CloudService.trimToSize(DigestUtils.sha256(key), keyLength), CloudService.STREAM_CIPHER);

			decrypter.init(Cipher.DECRYPT_MODE, skeySpec);
			encrypter.init(Cipher.ENCRYPT_MODE, skeySpec);

		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@Override
	public boolean isConnected() {
		return socket.isConnected() && !socket.isClosed();
	}
}
