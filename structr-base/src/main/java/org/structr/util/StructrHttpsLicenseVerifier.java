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
package org.structr.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 *
 */
public class StructrHttpsLicenseVerifier {

	private static final Logger logger         = LoggerFactory.getLogger(StructrHttpsLicenseVerifier.class);

	private static final Pattern HostIdPattern = Pattern.compile("[a-f0-9]{32}");
	private static final Pattern DatePattern   = Pattern.compile("[0-9]{4}\\-[0-9]{2}\\-[0-9]{2}");
	private static final Pattern NamePattern   = Pattern.compile("[a-zA-Z0-9 \\-]+");

	private Gson gson               = null;
	private String keystoreFileName = null;
	private String passwordFileName = null;

	public static void main(final String[] args) {

		if (args.length < 2) {

			System.out.println("Parameters: keystoreFileName passwordFileName");
			System.exit(0);
		}

		final String keystoreFileName = args[0];
		final String passwordFileName = args[1];

		new StructrHttpsLicenseVerifier(keystoreFileName, passwordFileName).run();
	}

	private StructrHttpsLicenseVerifier(final String keystoreFileName, final String passwordFileName) {

		logger.info("Starting HTTPS license server..");

		this.gson             = new GsonBuilder().setPrettyPrinting().create();
		this.keystoreFileName = keystoreFileName;
		this.passwordFileName = passwordFileName;
	}

	private void run() {

		try {

			logger.info("Listening on port 80");

			final InetSocketAddress address = new InetSocketAddress(80);
			final HttpServer server         = HttpServer.create();

			final HttpContext context = server.createContext("/", new HttpHandler() {

				@Override
				public void handle(final HttpExchange exchange) throws IOException {

					try {

						final String method = exchange.getRequestMethod();

						logger.info("New connection from {}, method {}", exchange.getRemoteAddress().getAddress().getHostAddress(), method);

						// support HEAD and GET requests to check health
						if ("head".equalsIgnoreCase(method)) {

							exchange.sendResponseHeaders(200, 0);

						} else if ("get".equalsIgnoreCase(method)) {

							String responseString = "Structr license server status ok";
							byte[] response = responseString.getBytes("utf-8");

							exchange.sendResponseHeaders(200, response.length);
							try (final OutputStream out = exchange.getResponseBody()) {

								out.write(response);
								out.flush();
							}

						} else if ("post".equalsIgnoreCase(method)) {

							final Ciphers ciphers = new Ciphers(keystoreFileName, passwordFileName);
							final InputStream is  = exchange.getRequestBody();
							final int bufSize     = 4096;
							final byte[] buf      = new byte[bufSize];
							int count             = 0;

							final byte[] sessionKeySource = IOUtils.readFully(is, 256);
							final byte[] ivSpecSource     = IOUtils.readFully(is, 256);

							logger.debug("Session key source from request: length {}, data {}", sessionKeySource.length, new String(sessionKeySource));
							logger.debug("IV spec source from request: length {}, data {}", ivSpecSource.length, new String(ivSpecSource));

							final byte[] sessionKey = ciphers.blockCipher.doFinal(sessionKeySource);
							final byte[] ivSpec = ciphers.blockCipher.doFinal(ivSpecSource);

							ciphers.streamCipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(sessionKey, "AES"), new IvParameterSpec(ivSpec));

							// we want to be able to control the number of bytes AND the timeout
							// of the underlying socket, so that we read the available amount of
							// data until the socket times out or we have read all the data.
							try {

								count = is.read(buf, 0, bufSize);

							} catch (IOException exception) {
								logger.warn("Exception reading request input stream {}", exception.getMessage());
							}

							if (count == 0) {

								logger.warn("Received empty or malformed request body from {}", exchange.getRemoteAddress());
								exchange.sendResponseHeaders(400, 0);
							}

							final byte[] decrypted = ciphers.streamCipher.doFinal(buf, 0, count);
							final String data      = new String(decrypted, "utf-8");

							// transform decrypted data into a Map<String, String>
							final List<Pair> pairs        = split(data).stream().map(StructrHttpsLicenseVerifier::keyValue).collect(Collectors.toList());
							final Map<String, String> map = pairs.stream().filter(Objects::nonNull).collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

							// send signature of name field back to client
							final String name     = map.get(StructrLicenseManager.NameKey);
							final byte[] response = name.getBytes("utf-8");

							// send response body
							try (final OutputStream out = exchange.getResponseBody()) {

								// validate data against customer database
								final ValidationResult result = isValid(ciphers, map);

								exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
								exchange.sendResponseHeaders(200, result.hasEndDate() ? 266 : 256);

								if (result.isValid()) {

									out.write(sign(ciphers, response));
									out.flush();

									// send end date if present?!
									if (result.hasEndDate()) {

										out.write(result.getEndDate().getBytes(StandardCharsets.UTF_8));
										out.flush();
									}

								} else {

									logger.warn("License verification failed.");

									// in case license isn't valid, we send a false response
									out.write(sign(ciphers, "invalid".getBytes("utf-8")));
									out.flush();
								}

							} catch (Throwable t) {

								t.printStackTrace();

								logger.warn("License verification failed, can't get response body.");
								exchange.sendResponseHeaders(400, 0);
							}

						} else {

							exchange.getResponseHeaders().add("Allow", "HEAD, GET, POST");
							exchange.sendResponseHeaders(405, 0);
						}

					} catch (Throwable t) {

						exchange.sendResponseHeaders(400, 0);
						t.printStackTrace();

					} finally {

						exchange.close();
					}
				}
			});

			server.bind(address, 10);
			server.setExecutor(Executors.newFixedThreadPool(10));
			server.start();

		} catch (Throwable t) {
			logger.warn("Unable to start license server: {}", t.getMessage());
		}
	}

	private ValidationResult isValid(final Ciphers ciphers, final Map<String, String> map) {

		final String toValidate       = StructrLicenseManager.collectLicenseFieldsForSignature(map);
		final String signature        = map.get(StructrLicenseManager.SignatureKey);
		final String startDate        = map.get(StructrLicenseManager.StartKey);
		final String licensee         = map.get(StructrLicenseManager.NameKey);
		final String hostId           = map.get(StructrLicenseManager.HostIdKey);
		final ValidationResult valid  = new ValidationResult();

		if (StringUtils.isNotBlank(toValidate) && StringUtils.isNotBlank(signature)) {

			try {

				final byte[] signedData    = toValidate.getBytes("utf-8");
				final byte[] signatureData = Hex.decodeHex(signature.toCharArray());

				ciphers.signer.initVerify(ciphers.keyStore.getCertificate("structr"));
				ciphers.signer.update(signedData);

				// verify signature of license data sent to us
				if (!ciphers.signer.verify(signatureData)) {

					logger.info("Client signature not valid.");

					return valid;
				}

			} catch (Throwable t) {

				logger.warn("Unable to verify client signature: {}", t.getMessage());

				return valid;
			}

			// verify license contents
			if (StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(licensee) && StringUtils.isNotBlank(hostId)) {

				// make sure that date and licensee do not contain illegal characters
				if (DatePattern.matcher(startDate).matches() && NamePattern.matcher(licensee).matches() && HostIdPattern.matcher(hostId).matches()) {

					logger.info("License request for {}, start date {}, host ID {}", licensee, startDate, hostId);

					final String cleanedName = licensee.replace(" ", "-");
					final String configName  = startDate + "-" + cleanedName + ".json";

					logger.info("Loading license configuration from {}..", configName);

					// reading and wrinting the configuration must be synchronized
					// so the file is protected from accidental overwriting
					synchronized (this) {

						// load config file or create new one
						Map<String, Object> config = readConfig(configName);
						if (config == null) {

							config = new HashMap<>();
						}

						// load count
						final Map<String, Object> hostIdMapping = getMapValue(config, StructrLicenseManager.HostIdMappingKey);
						final int limit                         = getIntValue(config, StructrLicenseManager.LimitKey, -1);
						final int hostIdCount                   = getIntValue(hostIdMapping, hostId, 0);
						final int count                         = hostIdMapping.size();

						// send end date from server to client
						valid.setEndDate(getStringValue(config, StructrLicenseManager.EndKey, null));

						if (limit == -1) {

							// no numerical limit found in config, check for "*" value
							valid.setValid("*".equals(getStringValue(config, StructrLicenseManager.LimitKey, null)));

							logger.info("count: {}, unlimited license, end date {}", count, valid.getEndDate());

						} else {

							valid.setValid(count <= limit);

							logger.info("count: {}, limit: {}, end date {}", count, limit, valid.getEndDate());
						}


						// update host ID count
						hostIdMapping.put(hostId, hostIdCount + 1);

						// store config
						writeConfig(configName, config);
					}

				} else {

					logger.info("Client request malformed: {} {} {}", startDate, licensee, hostId);
				}

			} else {

				logger.info("Client request incomplete, missing startDate, licensee or hostId.");
			}

		} else {

			logger.info("Client request incomplete, missing data or signature.");
		}

		return valid;
	}

	private byte[] sign(final Ciphers ciphers, final byte[] data) throws SignatureException, InvalidKeyException, NoSuchAlgorithmException {

		ciphers.signer.initSign((PrivateKey)ciphers.key);
		ciphers.signer.update(data);

		return ciphers.signer.sign();
	}

	private List<String> split(final String src) {
		return Arrays.asList(src.split("\n"));
	}

	private Map<String, Object> readConfig(final String name) {

		try (final FileReader reader = new FileReader(name)) {

			return gson.fromJson(reader, Map.class);

		} catch (IOException ioex) {
			logger.warn("Unable to open license config {}: {}", name, ioex.getMessage());
		}

		return null;
	}

	private void writeConfig(final String name, final Map<String, Object> config) {

		try (final FileWriter writer = new FileWriter(name)) {

			gson.toJson(config, writer);

		} catch (IOException ioex) {
			logger.warn("Unable to store license config {}: {}", name, ioex.getMessage());
		}
	}

	private int getIntValue(final Map<String, Object> data, final String key, final int defaultValue) {

		final Object src = data.get(key);
		if (src instanceof Number) {

			return ((Number)src).intValue();
		}

		return defaultValue;
	}

	private String getStringValue(final Map<String, Object> data, final String key, final String defaultValue) {

		final Object src = data.get(key);
		if (src instanceof String) {

			return (String)src;
		}

		return defaultValue;
	}

	private Map<String, Object> getMapValue(final Map<String, Object> data, final String key) {

		final Object src = data.get(key);
		if (src instanceof Map) {

			return (Map<String, Object>)src;
		}

		// create empty map
		final Map<String, Object> newMap = new HashMap<>();

		// store map in data object
		data.put(key, newMap);

		return newMap;

	}

	private String readPasswordFromFile(final String passwordFileName) throws IOException {

		try (final InputStream is = new FileInputStream(passwordFileName)) {

			final List<String> lines = IOUtils.readLines(is, "utf-8");

			if (!lines.isEmpty()) {

				return lines.get(0);
			}
		}

		return "";
	}

	// ----- private static members -----
	private static Pair keyValue(final String line) {

		final String[] parts = line.split("=", 2);

		if (parts.length == 2) {

			final String key   = parts[0].trim();
			final String value = parts[1].trim();

			return new Pair(key, value);
		}

		// ignore invalid lines
		return null;
	}

	private static class Pair {

		private String left  = null;
		private String right = null;

		public Pair(final String left, final String right) {
			this.left = left;
			this.right = right;
		}

		public String getLeft() {
			return left;
		}

		public String getRight() {
			return right;
		}
	}

	private class Ciphers {

		private Signature signer = null;
		private KeyStore keyStore = null;
		private Cipher streamCipher = null;
		private Cipher blockCipher = null;
		private Key key = null;

		public Ciphers(final String keystoreFileName, final String passwordFileName) throws KeyStoreException, NoSuchPaddingException, NoSuchAlgorithmException, UnrecoverableKeyException, CertificateException, IOException, InvalidKeyException {
			initialize(keystoreFileName, passwordFileName);
		}

		private final void initialize(final String keystoreFileName, final String passwordFileName) throws KeyStoreException, NoSuchPaddingException, NoSuchAlgorithmException, UnrecoverableKeyException, CertificateException, IOException, InvalidKeyException {

			logger.info("Loading key store, initializing ciphers..");

			this.keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			this.blockCipher = Cipher.getInstance(StructrLicenseManager.KeyEncryptionAlgorithm);
			this.streamCipher = Cipher.getInstance(StructrLicenseManager.DataEncryptionAlgorithm);
			this.signer = Signature.getInstance(StructrLicenseManager.SignatureAlgorithm);

			try (final InputStream is = new FileInputStream(keystoreFileName)) {

				final String password = readPasswordFromFile(passwordFileName);
				final char[] pwd = password.toCharArray();

				keyStore.load(is, pwd);

				this.key = keyStore.getKey("structr", pwd);

				blockCipher.init(Cipher.DECRYPT_MODE, key);
			}
		}
	}
}
