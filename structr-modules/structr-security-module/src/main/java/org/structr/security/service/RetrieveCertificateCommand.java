/*
 * Copyright (C) 2010-2021 Structr GmbH
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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
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
import org.structr.core.Services;
import org.structr.core.StaticValue;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.MaintenanceCommand;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.rest.resource.MaintenanceParameterResource;
import org.structr.rest.service.HttpService;
import org.structr.schema.SchemaHelper;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.Folder;

/**
 * Maintenance command to get or renew TLS certificates via ACME protocol (i.e. Let's Encrypt).
 *
 * There are three different modes:
 * wait (default): Creates an order with http or dns challenges, waits for the specified amount of seconds
 *                 and automatically tries to verify the challenges after that.
 * create:         Creates an order with http or dns challenges.
 * verify:         Verifies pending challenges that have been created in 'create' mode.
 */
public class RetrieveCertificateCommand extends Command implements MaintenanceCommand {

	private static final Logger logger = LoggerFactory.getLogger(RetrieveCertificateCommand.class.getName());
	private final static String CERTIFICATE_RETRIEVAL_STATUS = "CERTIFICATE_RETRIEVAL_STATUS";

	private final static String MODE_PARAM_KEY      = "mode";
	private final static String CHALLENGE_PARAM_KEY = "challenge";
	private final static String SERVER_PARAM_KEY    = "server";
	private final static String WAIT_PARAM_KEY      = "wait";

	private final static String WAIT_MODE_KEY   = "wait";
	private final static String CREATE_MODE_KEY = "create";
	private final static String VERIFY_MODE_KEY = "verify";

	private final static String PRODUCTION_SERVER_KEY = "production";
	private final static String STAGING_SERVER_KEY    = "staging";

	static {

		MaintenanceParameterResource.registerMaintenanceCommand("letsencrypt", RetrieveCertificateCommand.class);
	}

	private HttpServer server;
	private String     serverUrl;
	private String     challengeType;

	private String     mode;
	private int        waitForSeconds;
	private Collection<String> domains;

	private Account                account;
	private List<Order>            orders     = new ArrayList<>();
	private Map<String, Challenge> challenges = new HashMap<>();

	@Override
	public void execute(final Map<String, Object> attributes) throws FrameworkException {

		domains = Arrays.asList(StringUtils.split(Settings.LetsEncryptDomains.getValue(), " "));

		final String challengeParameter = (String) attributes.get(CHALLENGE_PARAM_KEY);
		challengeType = (StringUtils.isNotEmpty(challengeParameter) ? challengeParameter : Settings.LetsEncryptChallengeType.getValue());

		final String serverParameter = (String) attributes.get(SERVER_PARAM_KEY);
		if (StringUtils.isNotEmpty(serverParameter)) {

			switch (serverParameter) {

				case PRODUCTION_SERVER_KEY:
					serverUrl = Settings.LetsEncryptProductionServerURL.getValue();
					break;

				case STAGING_SERVER_KEY:
				default:
					serverUrl = Settings.LetsEncryptStagingServerURL.getValue();
					break;
			}

		} else {
			logger.info("No server supplied, aborting.");
			throw new FrameworkException(422, "No server supplied, aborting.");
		}

		mode = (String) attributes.get(MODE_PARAM_KEY);
		if (StringUtils.isEmpty(mode)) {
			mode = WAIT_PARAM_KEY;
		}

		final Boolean reload        = Boolean.TRUE.equals(attributes.get("reload"));

		final String wait = (String) attributes.get(WAIT_PARAM_KEY);
		if (StringUtils.isBlank(wait)) {
			waitForSeconds = Settings.LetsEncryptWaitBeforeAuthorization.getValue();
		} else {
			waitForSeconds = Integer.parseInt((String) attributes.get("wait"));
		}

		final Map<String, Object> broadcastData = new HashMap<>();
		final Long startTime = System.currentTimeMillis();
		broadcastData.put("start", startTime);
		broadcastData.put("mode", mode);
		publishBeginMessage(CERTIFICATE_RETRIEVAL_STATUS, broadcastData);

		switch (mode) {

			case WAIT_MODE_KEY: {

				final Order order = createNewOrder();
				publishProgressMessage(CERTIFICATE_RETRIEVAL_STATUS, "Order created");

				createChallenges();
				publishProgressMessage(CERTIFICATE_RETRIEVAL_STATUS, "Challenges created");

				try {
					// Wait the specified amount of milliseconds
					Thread.sleep(waitForSeconds * 1000);

				} catch (final InterruptedException ignore) {}
				publishProgressMessage(CERTIFICATE_RETRIEVAL_STATUS, "Waited " + waitForSeconds + " seconds");

				verifyChallenges(order.getAuthorizations());
				publishProgressMessage(CERTIFICATE_RETRIEVAL_STATUS, "Challenges verified");

				getCertificate(reload);

				final Long endTime = System.currentTimeMillis();
				broadcastData.remove("start");
				broadcastData.put("end", endTime);
				DecimalFormat decimalFormat = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
				final String duration = decimalFormat.format(((endTime - startTime) / 1000.0)) + "s";
				broadcastData.put("duration", duration);
				publishEndMessage(CERTIFICATE_RETRIEVAL_STATUS, broadcastData);

				break;
			}

			case CREATE_MODE_KEY: {

				createNewOrder();
				publishProgressMessage(CERTIFICATE_RETRIEVAL_STATUS, "Order created");

				createChallenges();
				publishProgressMessage(CERTIFICATE_RETRIEVAL_STATUS, "Challenges created");

				final Long endTime = System.currentTimeMillis();
				broadcastData.remove("start");
				broadcastData.put("end", endTime);
				DecimalFormat decimalFormat = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
				final String duration = decimalFormat.format(((endTime - startTime) / 1000.0)) + "s";
				broadcastData.put("duration", duration);
				publishEndMessage(CERTIFICATE_RETRIEVAL_STATUS, broadcastData);

				break;
			}

			case VERIFY_MODE_KEY: {

				final Order order = createNewOrder();
				publishProgressMessage(CERTIFICATE_RETRIEVAL_STATUS, "Order created");

				verifyChallenges(order.getAuthorizations());
				publishProgressMessage(CERTIFICATE_RETRIEVAL_STATUS, "Challenges verified");

				getCertificate(reload);

				final Long endTime = System.currentTimeMillis();
				broadcastData.remove("start");
				broadcastData.put("end", endTime);
				DecimalFormat decimalFormat = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
				final String duration = decimalFormat.format(((endTime - startTime) / 1000.0)) + "s";
				broadcastData.put("duration", duration);
				publishEndMessage(CERTIFICATE_RETRIEVAL_STATUS, broadcastData);

				break;
			}

			default:
				error("No valid mode supplied, aborting.");
		}
	}

	private void error(final String msg) throws FrameworkException {
		logger.error(msg);
		publishWarningMessage("Error in certificate retrieval progress", msg);
		throw new FrameworkException(422, msg);
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

	// ----- private methods -----

	private Account getOrCreateAccount() throws FrameworkException {

		if (account != null) {
			return account;
		}

		try {
			final Session session = new Session(serverUrl);

			account = new AccountBuilder()
					.agreeToTermsOfService()
					.useKeyPair(getOrCreateUserKey())
					.create(session);

			logger.info("Created new ACME session, account URL: {}", account.getStatus(), account.getLocation());


		} catch (final Throwable t) {
			error("Unable to create account: " + t.getMessage());
		}

		return account;
	}

	private Order createNewOrder() throws FrameworkException {

		Order order = null;

		try {

			order = getOrCreateAccount().newOrder().domains(domains).create();

			for (int i=0; i<order.getAuthorizations().size(); i++) {
				logger.info("Authorization: " + order.getAuthorizations().get(i).getJSON());
			}

			logger.info("Successfully created new certificate order for {}: {}", domains, order.getJSON());
			orders.add(order);

		} catch (final Throwable t) {
			error("Unable to create certificate order: " + t.getMessage());
		}

		return order;
	}

	private Order getOrCreateOrder() throws FrameworkException {

		if (orders.isEmpty()) {

			logger.info("No existing orders found, creating new order");
			return createNewOrder();

		} else {

			// At the moment, we support only one order
			return orders.get(0);
		}
	}

	private void getCertificate(final Boolean reload) throws FrameworkException {

		Order order = null;
		if (!orders.isEmpty()) {
			order = orders.get(0);
		} else {
			logger.warn("No existing certificate orders found, aborting.");
			return;
		}

		try {

			final CSRBuilder csrb = new CSRBuilder();
			csrb.addDomains(domains);

			final KeyPair domainKeyPair = getOrCreateDomainKey();
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

				if (reload) {

					logger.info("Reloading TLS certificate");

					Services.getInstance().getService(HttpService.class, "default").reloadSSLCertificate();
				}

			} else {
				error("Unable to get certificate from order, aborting.");
			}

		} catch (final Exception e) {
			error("Unable to retrieve certificate from ACME server: " + e.getMessage());
		}
	}

	private void createChallenges() throws FrameworkException {

		for (final Authorization auth : getOrCreateOrder().getAuthorizations()) {

			Challenge challenge = null;

			String domain = auth.getIdentifier().getDomain();

			if (org.shredzone.acme4j.Status.VALID == auth.getStatus()) {
				logger.info("Challenge for {} is already authorized.", domain);

				challenge = auth.getChallenges().get(0);

				domain = auth.getJSON().toString();

			} else {

				switch (challengeType) {

					case "http":
						challenge = httpChallenge(auth);
						break;

					case "dns":
						challenge = dnsChallenge(auth);
						break;
				}

				if (challenge == null) {

					clear();
					error("No ACME challenge found for type " + challengeType + ", aborting.");
				}

				logger.info("Created " + challengeType + " challenge authorization for domain {}; {}", domain, auth.getJSON());

				if (challenge.getStatus() == org.shredzone.acme4j.Status.VALID) {

					logger.info("Challenge for {} has already been authorized.", domain);
				}
			}
			if (challenge != null) {
				challenges.put(domain, challenge);
			}
		}
	}

	private void verifyChallenges(final List<Authorization> authorizations) throws FrameworkException {

		logger.info("Starting authorization for existing challenges.");

		// Loop over all authorizations
		for (final Authorization authorization : authorizations) {

			logger.info("Verify challenge authorization for {}", authorization.getJSON());

			try {

				final Challenge challenge = authorization.getChallenges().get(0);

				challenge.trigger();

				int attempts = 10;

				while (org.shredzone.acme4j.Status.VALID != challenge.getStatus() && attempts-- > 0) {

					if (challenge.getStatus() == org.shredzone.acme4j.Status.INVALID) {
						logger.info("Challenge authorization failed due to invalid response, aborting. Error: {}", challenge.getError());
						throw new FrameworkException(422, "Challenge authorization failed due to invalid response, aborting.");
					}

					Thread.sleep(3000L);

					challenge.update();
				}

				if (challenge.getStatus() != org.shredzone.acme4j.Status.VALID) {

					clear();

					logger.info("No valid authorization received for challenge " + challenge.getJSON() + ", aborting.");
					throw new FrameworkException(422, "No valid authorization received for challenge " + challenge.getJSON() + ", aborting.");
				}


				logger.info("Successfully finished challenge, cleaning up...");

				clear();

			} catch (final Throwable t) {

				clear();
				error("Challenge authorization failed, aborting: " + t.getMessage());
			}
		}

	}

	private void clear() {

		if ("http".equals(challengeType)) {
			stopServer();
		}

		account    = null;
		orders     = new ArrayList<>();
		challenges = new HashMap<>();
	}

	private void stopServer() {

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
			try { workerThread.join(); } catch (Throwable t) {
				logger.error(ExceptionUtils.getStackTrace(t));
			}
		}
	}

	private Challenge httpChallenge(final Authorization auth) throws FrameworkException {

		final Http01Challenge challenge = auth.findChallenge(Http01Challenge.class);

		if (challenge == null) {
			error("No " + Http01Challenge.TYPE + " challenge found, aborting.");
		}

		final String uriPath = "/.well-known/acme-challenge/" + challenge.getToken();
		final String content = challenge.getAuthorization();

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
			try { workerThread.join(); } catch (Throwable t) {
				logger.error(ExceptionUtils.getStackTrace(t));
			}

			if (exceptionFromThread.get(null) != null) {
				FrameworkException fex = exceptionFromThread.get(null);
				logger.error("Unable to create challenge response file in internal file system, aborting.", fex.getMessage());
				throw fex;
			}

		}

		return challenge;
	}

	private Challenge dnsChallenge(final Authorization auth) throws FrameworkException {

		return dnsChallenge(auth, null);
	}

	private Challenge dnsChallenge(final Authorization auth, final Integer wait) throws FrameworkException {

		final Dns01Challenge challenge = auth.findChallenge(Dns01Challenge.TYPE);
		if (challenge == null) {
			error("No " + Dns01Challenge.TYPE + " challenge found, aborting.");
		}

		if (WAIT_MODE_KEY.equals(mode)) {
			logger.info("Within the next " + waitForSeconds + " seconds, create a TXT record in your DNS for " + auth.getIdentifier().getDomain() + " with the following data:");
		}

		logger.info("_acme-challenge.{}. IN TXT {}", auth.getIdentifier().getDomain(), ((Dns01Challenge) challenge).getDigest());

		if (WAIT_MODE_KEY.equals(mode)) {
			logger.info("After " + waitForSeconds + " seconds, the certificate authority will probe the DNS record to authorize the challenge. If the record is not available, the authorization will fail.");
		}

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
			error("Unable to write to keystore: " + ex.getMessage());
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
			error("Unable to write to keystore: " + ex.getMessage());
		}
	}

	private KeyStore getOrCreateKeyStore() throws FrameworkException {

		final String keyStoreFilename = getKeyStoreFilename();
		final String password         = Settings.KeystorePassword.getValue();
		final File keyStoreFile       = new File(keyStoreFilename);

		KeyStore keyStore = null;
		try {
			keyStore = KeyStore.getInstance("PKCS12");

		} catch (final KeyStoreException ex) {
			error("Unable to create Keystore instance: " + ex.getMessage());
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
		} catch (final Exception ex) {
			error("Unable to create new keystore file. Check permissions. " + ex.getMessage());
		}
		return keyStore;

	}

	@Override
	public Map<String, String> getCustomHeaders() {
		return Collections.EMPTY_MAP;
	}

	@Override
	public List<Object> getPayload() {

		final List<Object> payload = new ArrayList<>();
		final JSONArray challengesJSON = new JSONArray();
		for (final String domain : challenges.keySet()) {
			final Challenge challenge = challenges.get(domain);
			final JSONObject challengeJson = new JSONObject(challenge.getJSON().toMap());
			challengeJson.put("domain", domain);
			challengesJSON.add(challengeJson);
		}
		payload.add(challengesJSON);
		return payload;
	}
}
