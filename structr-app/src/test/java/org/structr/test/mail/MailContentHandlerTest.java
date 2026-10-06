/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.test.mail;

import org.testng.annotations.Test;

import javax.activation.DataContentHandler;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

/**
 * The mail module reads messages with the javax.mail stack, and javax.activation picks the handler for
 * a content type from every META-INF/mailcap on the class path. A jar built for jakarta.mail - BouncyCastle's
 * bcjmail, which came in through Tika - registered its own handler for multipart/signed, and reading any
 * S/MIME-signed message then failed with NoClassDefFoundError: jakarta/activation/DataContentHandler, which
 * stopped the fetch of the whole folder at that message.
 *
 * <p>Runs in structr-app because only here is the class path what an installation ships: the mail module's
 * own tests never see the jars of the text-search module.
 */
public class MailContentHandlerTest {

	private static final Pattern HANDLER = Pattern.compile("x-java-content-handler\\s*=\\s*([\\w.$]+)");

	private static final String SIGNED_MESSAGE = String.join("\r\n",
		"From: sender@example.com",
		"To: recipient@example.com",
		"Subject: AW: signed",
		"Message-ID: <signed-1@example.com>",
		"MIME-Version: 1.0",
		"Content-Type: multipart/signed; protocol=\"application/pkcs7-signature\"; micalg=sha-256; boundary=\"b1\"",
		"",
		"--b1",
		"Content-Type: text/plain; charset=UTF-8",
		"",
		"Hello",
		"--b1",
		"Content-Type: application/pkcs7-signature; name=smime.p7s",
		"Content-Transfer-Encoding: base64",
		"Content-Disposition: attachment; filename=smime.p7s",
		"",
		"MIAGCSqGSIb3DQEHAqCAMIACAQExDzANBglghkgBZQMEAgEFADCABgkqhkiG9w0BBwEAAA==",
		"--b1--",
		"");

	@Test
	public void testASignedMessageCanBeRead() throws Exception {

		final MimeMessage message = new MimeMessage(Session.getInstance(new Properties()), new ByteArrayInputStream(SIGNED_MESSAGE.getBytes(StandardCharsets.US_ASCII)));
		final Object content      = message.getContent();

		assertTrue("a signed message must come back as a multipart, got " + content.getClass().getName(), content instanceof Multipart);
		assertEquals("the signed part and the signature", 2, ((Multipart) content).getCount());
	}

	@Test
	public void testEveryRegisteredContentHandlerFitsTheMailStackInUse() throws Exception {

		final List<String> unusable  = new ArrayList<>();
		final Enumeration<URL> files = getClass().getClassLoader().getResources("META-INF/mailcap");

		while (files.hasMoreElements()) {

			final URL file = files.nextElement();

			try (final BufferedReader reader = new BufferedReader(new InputStreamReader(file.openStream(), StandardCharsets.UTF_8))) {

				String line;

				while ((line = reader.readLine()) != null) {

					final Matcher matcher = HANDLER.matcher(line);
					if (line.startsWith("#") || !matcher.find()) {

						continue;
					}

					final String handler = matcher.group(1);

					try {

						final Class<?> type = Class.forName(handler, false, getClass().getClassLoader());

						if (!DataContentHandler.class.isAssignableFrom(type)) {

							unusable.add(handler + " from " + file + " is not a javax.activation.DataContentHandler");
						}

					} catch (ClassNotFoundException | LinkageError e) {

						unusable.add(handler + " from " + file + " cannot be loaded: " + e);
					}
				}
			}
		}

		assertTrue("Content handlers javax.activation would pick but cannot use:\n" + String.join("\n", unusable), unusable.isEmpty());
	}
}
