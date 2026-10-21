/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.test.xmpp;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;

import java.io.OutputStream;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * A Prosody XMPP server in a test container. The client of Structr requires TLS, so the server gets a self-signed
 * certificate for the XMPP domain "localhost", and the JVM trusts it through a trust store that replaces the default one
 * while the server runs. Started once per test class, hard-fails when Docker is not available.
 */
final class ProsodyTestSupport {

	static final String DOMAIN     = "localhost";
	static final String MUC_DOMAIN = "conference.localhost";

	private static final Logger logger = LoggerFactory.getLogger(ProsodyTestSupport.class);

	// mod_offline keeps the messages for a user that is not connected, mod_admin_shell lets the tests close sessions, the
	// muc component provides the chat rooms; a new room is open right away instead of locked until its owner configures it
	private static final String CONFIG = """
		admins = { }
		modules_enabled = { "roster"; "saslauth"; "tls"; "disco"; "ping"; "offline"; "admin_shell"; }
		modules_disabled = { "s2s"; }
		allow_registration = false
		c2s_require_encryption = true
		authentication = "internal_hashed"
		storage = "internal"
		log = { info = "*console"; }
		VirtualHost "localhost"
			ssl = { certificate = "/etc/prosody/certs/localhost.crt"; key = "/etc/prosody/certs/localhost.key"; }
		Component "conference.localhost" "muc"
			muc_room_locking = false
		""";

	private static GenericContainer<?> container = null;
	private static String previousTrustStore     = null;
	private static String previousTrustStorePass = null;
	private static String previousTrustStoreType = null;

	private ProsodyTestSupport() {
	}

	static synchronized void start() throws Exception {

		if (container != null) {

			return;
		}

		final KeyPair keyPair      = createKeyPair();
		final X509Certificate cert = createCertificate(keyPair);

		trustCertificate(cert);

		container = new GenericContainer<>("prosodyim/prosody:13.0")
			.withExposedPorts(5222)
			.withCopyToContainer(Transferable.of(CONFIG.getBytes(StandardCharsets.UTF_8), 0644), "/etc/prosody/prosody.cfg.lua")
			.withCopyToContainer(Transferable.of(toPem(cert), 0644), "/etc/prosody/certs/localhost.crt")
			.withCopyToContainer(Transferable.of(toPem(keyPair.getPrivate()), 0644), "/etc/prosody/certs/localhost.key")
			.waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));

		container.start();

		logger.info("Prosody test container running at {}:{}", getHost(), getPort());
	}

	static synchronized void stop() {

		if (container != null) {

			container.stop();
			container = null;
		}

		restoreProperty("javax.net.ssl.trustStore",         previousTrustStore);
		restoreProperty("javax.net.ssl.trustStorePassword", previousTrustStorePass);
		restoreProperty("javax.net.ssl.trustStoreType",     previousTrustStoreType);
	}

	static String getHost() {

		return container.getHost();
	}

	static int getPort() {

		return container.getMappedPort(5222);
	}

	/**
	 * Creates an account on the server.
	 */
	static void register(final String username, final String password) throws Exception {

		final Container.ExecResult result = container.execInContainerWithUser("prosody", "prosodyctl", "register", username, DOMAIN, password);
		if (result.getExitCode() != 0) {

			throw new IllegalStateException("Unable to register " + username + ": " + result.getStdout() + result.getStderr());
		}
	}

	/**
	 * Closes all sessions of the given account with the stream error system-shutdown, which is what a server sends to its
	 * clients when it restarts. A close without an error would tell the client that the server ended the session on
	 * purpose.
	 */
	static void closeSessions(final String jid) throws Exception {

		final String lua = "> for session_jid, session in pairs(prosody.full_sessions) do "
			+ "if session_jid:match('^" + jid + "/') then session:close('system-shutdown') end end";

		final Container.ExecResult result = container.execInContainerWithUser("prosody", "prosodyctl", "shell", lua);
		if (result.getExitCode() != 0) {

			throw new IllegalStateException("Unable to close the sessions of " + jid + ": " + result.getStdout() + result.getStderr());
		}
	}

	// ----- private static methods -----
	private static KeyPair createKeyPair() throws Exception {

		final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");

		generator.initialize(2048);

		return generator.generateKeyPair();
	}

	private static X509Certificate createCertificate(final KeyPair keyPair) throws Exception {

		final X500Name name                       = new X500Name("CN=" + DOMAIN);
		final Instant now                         = Instant.now();
		final JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(name, BigInteger.valueOf(now.toEpochMilli()), Date.from(now.minus(Duration.ofDays(1))), Date.from(now.plus(Duration.ofDays(30))), name, keyPair.getPublic());

		// the client checks the certificate against the XMPP domain
		builder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(new GeneralName(GeneralName.dNSName, DOMAIN)));

		return new JcaX509CertificateConverter().getCertificate(builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate())));
	}

	/**
	 * Makes the JVM trust the certificate by pointing the default trust store to a store that contains only it. The
	 * client of Structr uses the default trust managers, which read the trust store property when they are created.
	 */
	private static void trustCertificate(final X509Certificate cert) throws Exception {

		final Path path           = Files.createTempFile("structr-xmpp-test-truststore", ".p12");
		final char[] password     = "structr-test".toCharArray();
		final KeyStore trustStore = KeyStore.getInstance("PKCS12");

		trustStore.load(null, password);
		trustStore.setCertificateEntry(DOMAIN, cert);

		try (final OutputStream out = Files.newOutputStream(path)) {

			trustStore.store(out, password);
		}

		previousTrustStore     = System.getProperty("javax.net.ssl.trustStore");
		previousTrustStorePass = System.getProperty("javax.net.ssl.trustStorePassword");
		previousTrustStoreType = System.getProperty("javax.net.ssl.trustStoreType");

		System.setProperty("javax.net.ssl.trustStore",         path.toString());
		System.setProperty("javax.net.ssl.trustStorePassword", new String(password));
		System.setProperty("javax.net.ssl.trustStoreType",     "PKCS12");
	}

	private static void restoreProperty(final String key, final String value) {

		if (value != null) {

			System.setProperty(key, value);

		} else {

			System.clearProperty(key);
		}
	}

	private static byte[] toPem(final Object object) throws Exception {

		final StringWriter buffer = new StringWriter();

		try (final JcaPEMWriter writer = new JcaPEMWriter(buffer)) {

			writer.writeObject(object);
		}

		return buffer.toString().getBytes(StandardCharsets.UTF_8);
	}
}
