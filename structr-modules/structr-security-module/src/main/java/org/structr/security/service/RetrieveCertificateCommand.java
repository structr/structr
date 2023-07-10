/*
 * Copyright (C) 2010-2023 Structr GmbH
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
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.shredzone.acme4j.*;
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
import org.structr.schema.action.Actions;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.Folder;

import java.io.*;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

/**
 * Maintenance command to get or renew TLS certificates via ACME protocol (i.e. Let's Encrypt).
 *#
 * There are three different modes:
 * wait (default): Creates an order with http or dns challenges, waits for the specified amount of seconds
 *                 and automatically tries to verify the challenges after that.
 * create:         Creates an order with http or dns challenges.
 * verify:         Verifies pending challenges that have been created in 'create' mode.
 */
public class RetrieveCertificateCommand extends Command implements MaintenanceCommand {

	private static final Logger logger = LoggerFactory.getLogger(RetrieveCertificateCommand.class.getName());
	private final static String CERTIFICATE_RETRIEVAL_STATUS = "CERTIFICATE_RETRIEVAL_STATUS";
	private final static String ACME_DNS_CHALLENGE_PREFIX    = "_acme-challenge.";
	private final static String ACME_DNS_CHALLENGE_SUFFIX    = ".";
	private final static String MODE_PARAM_KEY                 = "mode";
	private final static String CHALLENGE_PARAM_KEY            = "challenge";
	private final static String SERVER_PARAM_KEY               = "server";
	private final static String WAIT_PARAM_KEY                 = "wait";
	private final static String RELOAD_PARAM_KEY               = "reload";
	private final static String VERBOSE_PARAM_KEY              = "verbose";
	private final static String KEEP_CHALLENGE_FILES_PARAM_KEY = "keepChallengeFiles";

	private final static String WAIT_MODE_KEY   = "wait";
	private final static String CREATE_MODE_KEY = "create";
	private final static String VERIFY_MODE_KEY = "verify";

	private final static String PRODUCTION_SERVER_KEY = "production";
	private final static String STAGING_SERVER_KEY    = "staging";

	private final static Integer MAX_RETRIES = 3;

	static {

		MaintenanceParameterResource.registerMaintenanceCommand("letsencrypt", RetrieveCertificateCommand.class);
	}

	private HttpServer server;
	private String     serverUrl;
	private String     challengeType;

	private String     mode               = WAIT_MODE_KEY;
	private int        waitForSeconds     = Settings.LetsEncryptWaitBeforeAuthorization.getValue();
	private boolean    reload             = false;
	private boolean    verbose            = false;
	private boolean    keepChallengeFiles = false;

	private Collection<String> domains;

	private Account                account;
	private List<Order>            orders     = new ArrayList<>();
	private Map<String, Challenge> challenges = new HashMap<>();
	private boolean success                   = false;
	private List<String> errorMessages        = new ArrayList<>();

	@Override
	public void execute(final Map<String, Object> attributes) throws FrameworkException {

		try {

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

			if (attributes.containsKey(MODE_PARAM_KEY)) {
				mode = "" + attributes.get(MODE_PARAM_KEY);
			}

			reload             = Boolean.TRUE.equals(attributes.get(RELOAD_PARAM_KEY));
			verbose            = Boolean.TRUE.equals(attributes.get(VERBOSE_PARAM_KEY));
			keepChallengeFiles = Boolean.TRUE.equals(attributes.get(KEEP_CHALLENGE_FILES_PARAM_KEY));

			if (verbose) {
				logger.info("Debug mode active - logging more verbosely and not removing challenge files.");
			}

			if (attributes.containsKey(WAIT_MODE_KEY)) {
				// support string and integer values (legacy)
				waitForSeconds = Integer.parseInt("" + attributes.get(WAIT_PARAM_KEY));
			}

			final Map<String, Object> broadcastData = new HashMap<>();
			final Long startTime = System.currentTimeMillis();
			broadcastData.put("start", startTime);
			broadcastData.put("mode", mode);
			publishBeginMessage(CERTIFICATE_RETRIEVAL_STATUS, broadcastData);

			switch (mode) {

				case WAIT_MODE_KEY: {

					publishProgressMessage(CERTIFICATE_RETRIEVAL_STATUS, "Creating Order");
					final Order order = createNewOrder();

					publishProgressMessage(CERTIFICATE_RETRIEVAL_STATUS, "Creating Challenges");
					createChallenges();

					try {
						// Wait the specified amount of milliseconds
						publishProgressMessage(CERTIFICATE_RETRIEVAL_STATUS, "Waiting " + waitForSeconds + " seconds");
						logger.info("Waiting " + waitForSeconds + " seconds");

						Thread.sleep(waitForSeconds * 1000);

					} catch (final InterruptedException ignore) {
					}

					publishProgressMessage(CERTIFICATE_RETRIEVAL_STATUS, "Waited " + waitForSeconds + " seconds");

					publishProgressMessage(CERTIFICATE_RETRIEVAL_STATUS, "Verifying Challenges");
					verifyChallenges(order.getAuthorizations());

					getCertificate();

					success = true;

					sendEndMessage(broadcastData, startTime);

					break;
				}

				case CREATE_MODE_KEY: {

					createNewOrder();
					publishProgressMessage(CERTIFICATE_RETRIEVAL_STATUS, "Order created");

					createChallenges();
					publishProgressMessage(CERTIFICATE_RETRIEVAL_STATUS, "Challenges created");

					sendEndMessage(broadcastData, startTime);

					break;
				}

				case VERIFY_MODE_KEY: {

					final Order order = createNewOrder();
					publishProgressMessage(CERTIFICATE_RETRIEVAL_STATUS, "Order created");

					verifyChallenges(order.getAuthorizations());
					publishProgressMessage(CERTIFICATE_RETRIEVAL_STATUS, "Challenges verified");

					getCertificate();

					success = true;

					sendEndMessage(broadcastData, startTime);

					break;
				}

				default:
					error("No valid mode supplied, aborting.");
			}

		} catch (FrameworkException fex) {

			// catch FrameworkException, so we always return a command result
			success = false;

		} finally {

			cleanUpChallengeFiles();
		}
	}

	private void sendEndMessage (Map<String, Object> broadcastData, final Long startTime) {

		final Long endTime = System.currentTimeMillis();
		broadcastData.remove("start");
		broadcastData.put("end", endTime);

		DecimalFormat decimalFormat = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

		final String duration = decimalFormat.format(((endTime - startTime) / 1000.0)) + "s";
		broadcastData.put("duration", duration);
		publishEndMessage(CERTIFICATE_RETRIEVAL_STATUS, broadcastData);
	}

	private void error(final String msg) throws FrameworkException {

		error(msg, true);
	}

	private void error(final String msg, final boolean addToErrorResponseList) throws FrameworkException {

		logger.error("Error in certificate retrieval progress: {}", msg);
		publishWarningMessage("Error in certificate retrieval progress", msg);

		if (addToErrorResponseList) {

			errorMessages.add(msg);
		}

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

			error("Unable to create account: " + t.getMessage(), false);
		}

		return account;
	}

	private Order createNewOrder() throws FrameworkException {

		Order order = null;

		try {

			order = getOrCreateAccount().newOrder().domains(domains).create();

			for (final Authorization authorization : order.getAuthorizations()) {

				if (verbose) {
					logger.info("Authorization: " + authorization.getJSON());
				}
			}

			if (verbose) {
				logger.info("Successfully created new certificate order for {}: {}", domains, order.getJSON());
			} else {
				logger.info("Successfully created new certificate order for {}", domains);
			}

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

	private void getCertificate() throws FrameworkException {

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

				int attempts = MAX_RETRIES;
				while (org.shredzone.acme4j.Status.VALID != order.getStatus() && attempts-- > 0) {

					if (org.shredzone.acme4j.Status.INVALID == order.getStatus()) {
						error("Order failed due to invalid response, aborting. Error: " + order.getError(), false);
					}

					Thread.sleep(2000L);

					order.update();
				}

				if (order.getStatus() != org.shredzone.acme4j.Status.VALID) {
					error("No valid order received after " + MAX_RETRIES + " attempts, aborting.", false);
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

				error("Unable to get certificate from order, aborting.", false);
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
				domain    = auth.getJSON().toString();

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

				if (verbose) {
					logger.info("Created " + challengeType + " challenge authorization for domain {}; {}", domain, auth.getJSON());
				} else {
					logger.info("Created " + challengeType + " challenge authorization for domain {}", domain);
				}

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

		for (final Authorization authorization : authorizations) {

			if (verbose) {
				logger.info("Verify challenge authorization for {}", authorization.getJSON());
			} else {
				logger.info("Verify challenge authorization");
			}

			try {

				final List<Challenge> challenges = authorization.getChallenges();

				for (final Challenge challenge : challenges) {

					if (challenge.getType().startsWith(challengeType)) {

						challenge.trigger();

						int attempts = MAX_RETRIES;
						while (org.shredzone.acme4j.Status.VALID != challenge.getStatus() && attempts-- > 0) {

							if (challenge.getStatus() == org.shredzone.acme4j.Status.INVALID) {
								error("Received invalid challenge response, aborting. Error: {}" + challenge.getError(), false);
							}

							Thread.sleep(3000L);

							challenge.update();
						}

						if (challenge.getStatus() != org.shredzone.acme4j.Status.VALID) {
							error("No valid authorization received for challenge " + challenge.getJSON() + ", after " + MAX_RETRIES + " attempts aborting.", false);
						}

						logger.info("Successfully finished challenge, cleaning up...");
					}
				}

			} catch (final Throwable t) {

				error("Challenge authorization failed: " + t.getMessage());

			} finally {

				clear();
			}
		}
	}

	private void clear() {

		if ("http".equals(challengeType)) {
			stopServer();
		}

		account    = null;
		//orders     = new ArrayList<>();
		challenges = new HashMap<>();
	}

	private void stopServer() {

		if (server != null) {

			logger.info("Stopping temporary HTTP server...");

			// If a temporary HTTP server is running, stop it.
			server.stop(0);

			logger.info("Successfully stopped temporary HTTP server.");
		}
	}

	private void cleanUpChallengeFiles() {

		if (keepChallengeFiles) {

			logger.info("Not removing challenge files /.well-known/acme-challenge/* from internal file system...");
			return;
		}

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
			try {

				workerThread.join();

			} catch (Throwable t) {

				logger.error(ExceptionUtils.getStackTrace(t));
			}

			if (exceptionFromThread.get(null) != null) {

				FrameworkException fex = exceptionFromThread.get(null);
				error("Unable to create challenge response file in internal file system, aborting. Error: " + fex.getMessage());
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

		final String domain = auth.getIdentifier().getDomain();
		final String record = ACME_DNS_CHALLENGE_PREFIX + domain + ACME_DNS_CHALLENGE_SUFFIX;
		final String digest = challenge.getDigest();

		final Object result = Actions.callWithSecurityContext("onAcmeChallenge", SecurityContext.getSuperUserInstance(), Map.of("type", "dns", "domain", domain, "record", record, "digest", digest));
		if (result == null) {

			publishProgressMessage(CERTIFICATE_RETRIEVAL_STATUS, "Lifecycle method 'onAcmeChallenge' not found! Within the next " + waitForSeconds + " seconds, create a DNS record for " + domain + " with the following data: Name: '" + record + "', Type: 'TXT', Value: '" + digest + "'");

			logger.info("Within the next " + waitForSeconds + " seconds, create a DNS TXT record for " + domain + " with the following data:");
			logger.info("{} IN TXT {}", record, digest);
			logger.info("After " + waitForSeconds + " seconds, the certificate authority will probe the DNS record to authorize the challenge. If the record is not available, the authorization will fail.");

		} else {

			publishProgressMessage(CERTIFICATE_RETRIEVAL_STATUS, "Called lifecycle method onAcmeChallenge");

			logger.info("DNS TXT record for domain " + domain + " has to be created with the following data:");
			logger.info("{} IN TXT {}", record, digest);
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
	public Object getCommandResult() {

		final Map<String, Object> result = new HashMap<>();
		result.put("success", success);
		result.put("errors", errorMessages);

		return result;
	}
}
