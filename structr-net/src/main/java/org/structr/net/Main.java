/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.net.common.KeyHelper;
import org.structr.net.peer.Peer;
import org.structr.net.protocol.BroadcastMessage;
import org.structr.net.protocol.DirectMessage;
import org.structr.net.repository.DefaultRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.util.*;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;

/**
 * The main class that starts the peer, with an optional interactive
 * flag and additional configuration to select the local bind address
 * and the initial peer to which the discovery packet will be
 * broadcasted (default is 255.255.255.255).
 */
public class Main {

	private static final Logger logger = LoggerFactory.getLogger(Main.class.getName());

	public static void main(final String[] args) {

		String uuid           = UUID.randomUUID().toString().replaceAll("\\-", "");
		String initialPeer    = "255.255.255.255";
		String bindAddress    = "0.0.0.0";
		String privateKeyFile = null;
		String publicKeyFile  = null;
		boolean printKeys     = false;
		boolean isInteractive = false;
		boolean verbose       = false;

		for (int i=0 ;i<args.length; i++) {

			switch (args[i]) {

				case "-i":
					System.out.println("Interactive mode enabled.");
					isInteractive = true;
					break;

				case "-b":
					bindAddress = args[i+1];
					System.out.println("Bind address set to " + bindAddress);
					break;

				case "-p":
					initialPeer = args[i+1];
					System.out.println("Initial peer set to " + initialPeer);
					break;

				case "-v":
					verbose = true;
					System.out.println("Verbose mode enabled.");
					break;

				case "-u":
					uuid = args[i+1];
					System.out.println("Client UUID set to " + uuid);
					break;

				case "--print-keys":
					printKeys = true;
					System.out.println("Printing peer keys.");
					break;

				case "--private-key-file":
					privateKeyFile = args[i+1];
					System.out.println("Using private key from " + privateKeyFile);
					break;

				case "--public-key-file":
					publicKeyFile = args[i+1];
					System.out.println("Using public key from " + publicKeyFile);
					break;

				case "-h":
					printHelp();
					System.exit(0);
					break;
			}
		}

		final DefaultRepository repo = new DefaultRepository(uuid);
		KeyPair keyPair              = null;

		if (privateKeyFile != null && publicKeyFile != null) {

			try {
				final Decoder decoder   = Base64.getDecoder();
				final byte[] privateKey = decoder.decode(readBase64(privateKeyFile));
				final byte[] publicKey  = decoder.decode(readBase64(publicKeyFile));

				keyPair = KeyHelper.fromBytes("RSA", privateKey, publicKey);

			} catch (IOException ioex) {
				logger.warn("", ioex);
			}

		} else {

			keyPair = KeyHelper.getOrCreateKeyPair("RSA", 2048);
		}

		final Peer peer = new Peer(keyPair, repo, bindAddress, initialPeer);

		if (printKeys && keyPair != null) {

			final Encoder encoder = Base64.getEncoder();
			System.out.println("Private key (BASE64 encoded): " + encoder.encodeToString(keyPair.getPrivate().getEncoded()));
			System.out.println("Public key (BASE64 encoded):  " + encoder.encodeToString(keyPair.getPublic().getEncoded()));
		}

		repo.setPeer(peer);

		peer.setVerbose(verbose);
		peer.initializeServer();
		peer.start();


		if (isInteractive) {

			final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			String line                 = null;

			do {

				try {

					System.out.print(peer.getLocalPort() + "> ");
					line = reader.readLine();

					final String[] parts = line.split("[ ]+");
					final String cmd     = parts[0];

					switch (cmd) {

						case "exit":
							peer.stop();
							break;

						case "i":
							peer.printInfo();
							break;

						case "kill":
							peer.broadcast(new BroadcastMessage(peer.getUuid(), "kill"));
							break;

						case "info":
							peer.broadcast(new BroadcastMessage(peer.getUuid(), "info"));
							break;

						case "span":
							peer.broadcast(new BroadcastMessage(peer.getUuid(), "span"));
							break;

						case "msg":
							if (parts.length < 3) {
								System.out.println("usage: msg <peer> <message>");
								break;
							}
							peer.broadcast(new DirectMessage(peer.getUuid(), parts[1], parts[2]));
							break;

						case "broadcast":
							if (parts.length < 2) {
								System.out.println("usage: broadcast <message>");
								break;
							}
							peer.broadcast(new BroadcastMessage(peer.getUuid(), parts[1]));
							break;

						case "ping":
							if (parts.length < 2) {
								System.out.println("usage: ping <peer>");
								break;
							}
							peer.broadcast(new DirectMessage(peer.getUuid(), parts[1], "ping"));
							break;

						case "new":
							if (parts.length < 3) {
								System.out.println("usage: new <type> <owner> [<key> <value>]...");
								break;
							}
							final Map<String, Object> map = new HashMap<>();
							String key                    = null;

							for (int i=3; i<parts.length; i++) {

								if (key == null) {

									key = parts[i];

								} else {

									System.out.println(key + " = " + parts[i]);
									map.put(key, parts[i]);
									key = null;
								}
							}

							repo.create(UUID.randomUUID().toString().replaceAll("\\-", ""), parts[1], peer.getUuid(), parts[2], peer.getPseudoTemporalEnvironment().next(), map);
							break;

						case "get":
							if (parts.length < 3) {
								System.out.println("usage: get <id> <key>");
								break;
							}
							peer.get(parts[1], parts[2]);
							break;

						case "set":
							if (parts.length < 4) {
								System.out.println("usage: set <id> <key> <value>");
								break;
							}
							peer.set(parts[1], parts[2], parts[3]);
							break;
					}

				} catch (Throwable t) {
					logger.warn("", t);
				}


			} while (peer.isRunning() && !line.equals("quit"));

			peer.stop();
		}
	}

	private static void printHelp() {

		System.out.println("structr-net Client command line options");
		System.out.println(" -b <addr>      - set bind address");
		System.out.println(" -h             - show this help message ");
		System.out.println(" -i             - enable interactive mode");
		System.out.println(" -p <addr>      - set initial peer");
		System.out.println(" -u <uuid>      - set peer UUID ");
		System.out.println(" -v             - enable verbose mode");
	}

	private static String readBase64(final String fileName) throws IOException {
		return getKey(Files.readAllLines(Paths.get(fileName)));
	}

	private static String getKey(final List<String> lines) {

		// return the first non-empty line that does not begin with a comment char
		for (final String line : lines) {

			if (line.contains("#")) {

				final String cleanedLine = line.substring(0, line.indexOf("#"));
				if (!cleanedLine.isEmpty()) {

					return cleanedLine;
				}

			} else if (!line.isEmpty()) {

				return line;
			}
		}

		return null;
	}
}
