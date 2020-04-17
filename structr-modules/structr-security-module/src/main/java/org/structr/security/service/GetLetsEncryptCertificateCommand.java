/**
 * Copyright (C) 2010-2020 Structr GmbH
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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.security.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.shredzone.acme4j.Account;
import org.shredzone.acme4j.AccountBuilder;
import org.shredzone.acme4j.Authorization;
import org.shredzone.acme4j.Certificate;
import org.shredzone.acme4j.Order;
import org.shredzone.acme4j.Session;
import org.shredzone.acme4j.challenge.Challenge;
import org.shredzone.acme4j.challenge.Dns01Challenge;
import org.shredzone.acme4j.challenge.Http01Challenge;
import org.shredzone.acme4j.util.CSRBuilder;
import org.shredzone.acme4j.util.KeyPairUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.service.Command;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.StaticValue;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.MaintenanceCommand;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.rest.resource.MaintenanceParameterResource;
import org.structr.schema.SchemaHelper;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.Folder;

/**
 * Maintenance command to get or renew Let's Encrypt certificates.
 *
 */
public class GetLetsEncryptCertificateCommand extends Command implements MaintenanceCommand {

	private static final Logger logger = LoggerFactory.getLogger(GetLetsEncryptCertificateCommand.class.getName());

	static {

		MaintenanceParameterResource.registerMaintenanceCommand("letsencrypt", GetLetsEncryptCertificateCommand.class);
	}

	private static HttpServer server;
	private static String     serverUrl;
	private static String     challengeType;

	@Override
	public void execute(final Map<String, Object> attributes) throws FrameworkException {

		final String challenge      = (String) attributes.get("challenge");
		final String server         = (String) attributes.get("server");
		final String wait           = (String) attributes.get("wait");

		final int waitForSeconds;
		if (StringUtils.isBlank(wait)) {
			waitForSeconds = Settings.LetsEncryptWaitBeforeAuthorization.getValue();
		} else {
			waitForSeconds = Integer.parseInt((String) attributes.get("wait")) * 1000;
		}

		execute(server, challenge, waitForSeconds);
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}

	@Override
	public boolean requiresFlushingOfCaches() {
		return false;
	}

	@Override
	public Class getServiceClass() {
		return null;
	}

	public void execute(final String server, final String challenge, final Integer wait) throws FrameworkException {

		if (server != null) {

			switch (server) {

				case "production":
					serverUrl = Settings.LetsEncryptProductionServerURL.getValue();
					break;

				case "staging":
				default:
					serverUrl = Settings.LetsEncryptStagingServerURL.getValue();
					break;
			}

		} else {
			logger.info("No server supplied, aborting.");
			throw new FrameworkException(500, "No server supplied, aborting.");
		}

		challengeType = (challenge != null ? challenge : Settings.LetsEncryptChallengeType.getValue());

		getCertificate(challengeType, wait);

	}

	// ----- private methods -----

	private void getCertificate(final String challengeType, final Integer wait) throws FrameworkException {

		try {

			final Collection<String> domains = Arrays.asList(StringUtils.split(Settings.LetsEncryptDomains.getValue(), " "));

			final Session session = new Session(serverUrl);

			final Account account = new AccountBuilder()
					.agreeToTermsOfService()
					.useKeyPair(getOrCreateUserKey())
					.create(session);

			logger.info("Registered a new user, URL: {}", account.getLocation());

			final KeyPair domainKeyPair = getOrCreateDomainKey();

			final Order order = account.newOrder().domains(domains).create();

			for (Authorization auth : order.getAuthorizations()) {
				authorizeChallenge(challengeType, auth, wait);
			}

			final CSRBuilder csrb = new CSRBuilder();
			csrb.addDomains(domains);
			csrb.sign(domainKeyPair);

			try (final Writer out = new FileWriter(new File(Settings.LetsEncryptDomainCSRFileName.getValue()))) {
				csrb.write(out);
			}

			order.execute(csrb.getEncoded());

			try {
				int attempts = 3;
				while (org.shredzone.acme4j.Status.VALID != order.getStatus() && attempts-- > 0) {

					if (org.shredzone.acme4j.Status.INVALID == order.getStatus()) {
						logger.info("Order failed after " + attempts + " attempts, aborting.");
						throw new FrameworkException(422, "Order failed after " + attempts + " attempts, aborting.");
					}

					Thread.sleep(2000L);

					order.update();
				}
			} catch (InterruptedException ex) {
				logger.error("Order thread has been interrupted", ex);
				Thread.currentThread().interrupt();
			}

			final Certificate certificate = order.getCertificate();

			if (certificate != null) {

				logger.info("Certificate for domains {} successfully generated.", domains);
				logger.info("Certificate URL: {}", certificate.getLocation());

				try (FileWriter fw = new FileWriter(new File(Settings.LetsEncryptDomainChainFilename.getValue()))) {
					certificate.writeCertificate(fw);
				}

				logger.info("Writing to keystore {}", Settings.KeystorePath.getValue());

				// Write keystore file
				writeCertificateToKeyStore(domains, certificate, domainKeyPair);

				logger.info("Keystore file successfully written.");

			} else {
				logger.info("Unable to get certificate from order, aborting.");
				throw new FrameworkException(422, "Unable to get certificate from order, aborting.");
			}


		} catch (final Exception e) {

			logger.info("Unable to get certificate from Let's Encrypt: " + e.getMessage());
			throw new FrameworkException(422, "Unable to get certificate from Let's Encrypt: " + e.getMessage());
		}
	}

	private void writeCertificateToKeyStore(final Collection<String> domains, final Certificate certificate, final KeyPair domainKeyPair) throws FrameworkException {

		final String password = Settings.KeystorePassword.getValue();

		final KeyStore keyStore = getOrCreateKeyStore();
		try {

			final String certificateAlias = StringUtils.join(domains, ", ");

			final List<X509Certificate> certificateChainList = certificate.getCertificateChain();

			KeyStore.PrivateKeyEntry privateKeyEntry = new KeyStore.PrivateKeyEntry(domainKeyPair.getPrivate(), certificateChainList.toArray(new X509Certificate[certificateChainList.size()]));
			KeyStore.ProtectionParameter protParam = new KeyStore.PasswordProtection(password.toCharArray());

			keyStore.setEntry(certificateAlias + "_" + Settings.LetsEncryptDomainKeyFilename.getValue(), privateKeyEntry, protParam);

			writeKeyStore(keyStore);

		} catch (final Exception ex) {

			logger.info("Unable to write to keystore: " + ex.getMessage());
			throw new FrameworkException(422, "Unable to write to keystore: " + ex.getMessage());
		}
	}

	private String getKeyStoreFilename() {
		return Settings.KeystorePath.getValue(Settings.LetsEncryptDomainKeyFilename.getValue() + ".keystore");
	}

	private void writeKeyStore(final KeyStore keyStore) throws FrameworkException {

		final String keyStoreFilename = getKeyStoreFilename();
		final String password         = Settings.KeystorePassword.getValue();

		final File keyStoreFile = new File(keyStoreFilename);

		try (FileOutputStream fos = new FileOutputStream(keyStoreFile)) {

			keyStore.store(fos, password.toCharArray());

		} catch (final Exception ex) {
			logger.info("Unable to write to keystore: " + ex.getMessage());
			throw new FrameworkException(422, "Unable to write to keystore: " + ex.getMessage());
		}
	}

	private KeyStore getOrCreateKeyStore() throws FrameworkException {

		final String keyStoreFilename = getKeyStoreFilename();
		final String password         = Settings.KeystorePassword.getValue();
		final File keyStoreFile       = new File(keyStoreFilename);

		final KeyStore keyStore;
		try {
			keyStore = KeyStore.getInstance("PKCS12");

		} catch (final KeyStoreException ex) {
			logger.info("Unable to create Keystore instance: " + ex.getMessage());
			throw new FrameworkException(422, "Unable to create Keystore instance: " + ex.getMessage());
		}

		try {

			if (!keyStoreFile.exists()) {
				keyStoreFile.createNewFile();

				keyStore.load(null, null);

			} else {

				try (final FileInputStream fis = new java.io.FileInputStream(keyStoreFile)) {

					keyStore.load(fis, password.toCharArray());
				}
			}

			return keyStore;

		} catch (final Exception ex) {
			logger.info("Unable to create new keystore file. Check permissions. " + ex.getMessage());
			throw new FrameworkException(422, "Unable to create new keystore file. Check permissions. " + ex.getMessage());
		}
	}

	private void authorizeChallenge(final String challengeType, final Authorization auth, final int wait) throws Exception {

		logger.info("Starting challenge authorization for domain {}", auth.getIdentifier().getDomain());

		if (org.shredzone.acme4j.Status.VALID == auth.getStatus()) {
			return;
		}

		Challenge challenge = null;
		switch (challengeType) {

			case "http":
				challenge = httpChallenge(auth);
				break;

			case "dns":
				challenge = dnsChallenge(auth, wait);
				break;
		}

		if (challenge == null) {

			stopServer();

			logger.info("No ACME challenge found, aborting.");
			throw new FrameworkException(422, "No ACME challenge found, aborting.");
		}

		if (challenge.getStatus() == org.shredzone.acme4j.Status.VALID) {

			stopServer();

			logger.info("Challenge has already been authorized, aborting.");
			return;
		}

		// Wait the specified amount of milliseconds
		Thread.sleep(wait);

		challenge.trigger();

		try {

			int attempts = 10;

			while (org.shredzone.acme4j.Status.VALID != challenge.getStatus() && attempts-- > 0) {

				if (challenge.getStatus() == org.shredzone.acme4j.Status.INVALID) {
					logger.info("Challenge authorization failed due to invalid response, aborting. Error: {}", challenge.getError());
					throw new FrameworkException(422, "Challenge authorization failed due to invalid response, aborting.");
				}

				Thread.sleep(3000L);

				challenge.update();
			}

		} catch (final InterruptedException ex) {

			stopServer();

			logger.error("Challenge authorization thread has been interrupted", ex);
			Thread.currentThread().interrupt();
		}

		if (challenge.getStatus() != org.shredzone.acme4j.Status.VALID) {

			stopServer();

			logger.info("No valid authorization received for challenge for domain " + auth.getIdentifier().getDomain() + ", aborting.");
			throw new FrameworkException(422, "No valid authorization received for challenge for domain " + auth.getIdentifier().getDomain() + ", aborting.");
		}

		if (challengeType.equals("http")) {

			logger.info("Successfully finished challenge, cleaning up...");
			stopServer();
		}
	}

	private void stopServer() {

		if (challengeType.equals("http")) {

			if (server != null) {

				logger.info("Stopping temporary HTTP server...");

				// If a temporary HTTP server is running, stop it.
				server.stop(0);

				logger.info("Successfully stopped temporary HTTP server.");

			} else {

				logger.info("Removing /.well-known/acme-challenge/* from internal file system...");

				// put cleanup of folders/file in thread so we can use it in scripting
				final Thread workerThread = new Thread(() -> {

					final App app = StructrApp.getInstance();
					try (final Tx tx = app.tx()) {

						// Delete challenge response file and all parent folders from internal file system

						final SecurityContext adminContext = SecurityContext.getSuperUserInstance();
						final Folder wellKnownFolder = (Folder) FileHelper.getFileByAbsolutePath(adminContext, "/.well-known");
						if (wellKnownFolder != null) {

							final List<NodeInterface> filteredResults = new LinkedList<>();
							filteredResults.addAll(wellKnownFolder.getAllChildNodes());

							for (NodeInterface node : filteredResults) {
								app.delete(node);
							}

							app.delete(wellKnownFolder);
						}

						tx.success();

					} catch (FrameworkException fex) {

						logger.error("Unable to remove challenge response file and folders. {}", fex.getMessage());
					}

					logger.info("Successfully removed challenge response resources /.well-known/acme-challenge/* from internal file system.");
				});

				workerThread.start();
				try { workerThread.join(); } catch (Throwable t) { t.printStackTrace(); }
			}
		}
	}

	public Challenge httpChallenge(final Authorization auth) throws FrameworkException {

		final Http01Challenge challenge = auth.findChallenge(Http01Challenge.class);
		final String uriPath            = "/.well-known/acme-challenge/" + challenge.getToken();
		final String content            = challenge.getAuthorization();

		if (challenge == null) {
			logger.info("No " + Http01Challenge.TYPE + " challenge found, aborting.");
			throw new FrameworkException(422, "No " + Http01Challenge.TYPE + " challenge found, aborting.");
		}

		try {

			logger.info("Creating temporary HTTP server listening on port 80.");

			server = HttpServer.create(new InetSocketAddress(80), 0);


			logger.info("HTTP Challenge URI path: " + uriPath);
			logger.info("HTTP Challenge content: " + content);

			server.createContext(uriPath, new HttpHandler() {

				@Override
				public void handle(HttpExchange he) throws IOException {

					logger.info("Processing challenge authorization request...");

					he.sendResponseHeaders(200, content.length());
					OutputStream os = he.getResponseBody();
					os.write(content.getBytes());
					os.close();

					logger.info("Successfully responded to challenge authorization request.");
				}

			});

			server.start();

			logger.info("Temporary HTTP started.");

		} catch (final IOException iox) {

			stopServer();

			logger.info("Unable to start temporary HTTP server for challenge authorization, trying internal file server... (Reason: {})", iox.getMessage());

			final StaticValue<FrameworkException> exceptionFromThread = new StaticValue<>(null);

			// put creation of folders/file in thread so we can use it in scripting
			final Thread workerThread = new Thread(() -> {

				final App app = StructrApp.getInstance();

				try (final Tx tx = app.tx()) {

					final SecurityContext adminContext = SecurityContext.getSuperUserInstance();
					final Folder parentFolder = FileHelper.createFolderPath(adminContext, "/.well-known/acme-challenge/");

					PropertyMap props = new PropertyMap();
					props.put(StructrApp.key(org.structr.web.entity.Folder.class, "visibleToPublicUsers"), true);
					props.put(StructrApp.key(org.structr.web.entity.Folder.class, "visibleToAuthenticatedUsers"), true);

					parentFolder.setProperties(adminContext, props);
					parentFolder.getParent().setProperties(adminContext, props);

					org.structr.web.entity.File challengeFile = FileHelper.createFile(adminContext, new ByteArrayInputStream(content.getBytes()), "text/plain", SchemaHelper.getEntityClassForRawType("File"), challenge.getToken(), parentFolder);

					props = new PropertyMap();
					props.put(StructrApp.key(org.structr.web.entity.File.class, "visibleToPublicUsers"), true);
					props.put(StructrApp.key(org.structr.web.entity.File.class, "visibleToAuthenticatedUsers"), true);

					challengeFile.setProperties(adminContext, props);

					tx.success();

				} catch (IOException ex) {

					exceptionFromThread.set(null, new FrameworkException(422, ex.getMessage()));

				} catch (FrameworkException fex) {

					exceptionFromThread.set(null, fex);
				}
			});

			workerThread.start();
			try { workerThread.join(); } catch (Throwable t) { t.printStackTrace(); }

			if (exceptionFromThread.get(null) != null) {
				FrameworkException fex = exceptionFromThread.get(null);
				logger.error("Unable to create challenge response file in internal file system, aborting.", fex.getMessage());
				throw fex;
			}

		}

		return challenge;
	}

	public Challenge dnsChallenge(final Authorization auth, final int wait) throws FrameworkException {

		final Dns01Challenge challenge = auth.findChallenge(Dns01Challenge.TYPE);
		if (challenge == null) {
			logger.info("No " + Dns01Challenge.TYPE + " challenge found, aborting.");
			throw new FrameworkException(422, "No " + Dns01Challenge.TYPE + " challenge found, aborting.");
		}

		logger.info("Within the next " + (wait/1000) + " seconds, create a TXT record in your DNS for " + auth.getIdentifier().getDomain() + " with the folling data:");
		logger.info("_acme-challenge.{}. IN TXT {}", auth.getIdentifier().getDomain(), challenge.getDigest());
		logger.info("After " + (wait/1000) + " seconds, the certificate authority will probe the DNS record to authorize the challenge. If the record is not available, the authorization will fail.");

		return challenge;
	}

	private KeyPair getOrCreateUserKey() throws IOException {

		final File userKeyFile = new File(Settings.LetsEncryptUserKeyFilename.getValue());

		if (userKeyFile.exists()) {

			try (final FileReader fileWriter = new FileReader(userKeyFile)) {
				return KeyPairUtils.readKeyPair(fileWriter);
			}

		} else {

			final KeyPair userKey = KeyPairUtils.createKeyPair(Settings.LetsEncryptKeySize.getValue());

			try (FileWriter fileWriter = new FileWriter(userKeyFile)) {

				KeyPairUtils.writeKeyPair(userKey, fileWriter);
			}

			return userKey;
		}
	}

	private KeyPair getOrCreateDomainKey() throws IOException {

		final File domainKeyFile = new File(Settings.LetsEncryptDomainKeyFilename.getValue());

		if (domainKeyFile.exists()) {

			try (final FileReader fileWriter = new FileReader(domainKeyFile)) {
				return KeyPairUtils.readKeyPair(fileWriter);
			}

		} else {

			final KeyPair domainKey = KeyPairUtils.createKeyPair(Settings.LetsEncryptKeySize.getValue());

			try (final FileWriter fileWriter = new FileWriter(domainKeyFile)) {

				KeyPairUtils.writeKeyPair(domainKey, fileWriter);
			}

			return domainKey;
		}
	}

	@Override
	public Map<String, String> getCustomHeaders() {
		return Collections.EMPTY_MAP;
	}

	@Override
	public List<Object> getPayload() {
		return Collections.EMPTY_LIST;
	}
}
