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
package org.structr.test.common.helper;

import org.apache.commons.mail.EmailAttachment;
import org.apache.commons.mail.HtmlEmail;
import org.structr.common.helper.AdvancedMailContainer;
import org.structr.common.helper.DynamicMailAttachment;
import org.structr.common.helper.MailHelper;
import org.testng.annotations.Test;

import javax.mail.util.ByteArrayDataSource;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Ticket 1603: names, subjects and custom headers were handed to commons-email exactly as an
 * application script had supplied them, and a script is free to put a line break into any of them.
 *
 * <p>A line break in a header <em>name</em> ends that header and starts the next one, so
 * mail_add_header('X-Custom\r\nBcc', 'attacker@example.com') put a Bcc line into the message that
 * the sender never wrote. In a header value, a personal name or a subject, Jakarta Mail folds the
 * break into a continuation line instead - no extra header, but attacker-controlled text ends up
 * where the sender expects a name.
 *
 * <p>These tests build the message the way MailHelper does and read the bytes that would go on the
 * wire, because that is where the injected header appears: the parsed MimeMessage still reports the
 * mangled name as one header, and only the serialized form shows the second line.
 */
public class MailHeaderInjectionTest {

	private static final String INJECTED_ADDRESS = "attacker@evil.example";

	@Test
	public void testALineBreakInAHeaderNameDoesNotAddAHeader() throws Exception {

		final AdvancedMailContainer amc = container();

		amc.addCustomHeader("X-Custom\r\nBcc", INJECTED_ADDRESS);

		final String headers = headersOf(amc);

		assertNoInjectedHeader(headers);
		assertTrue("the header must survive on a single line", headers.contains("X-CustomBcc: " + INJECTED_ADDRESS));
	}

	@Test
	public void testALineBreakInAHeaderValueStaysInsideItsHeader() throws Exception {

		final AdvancedMailContainer amc = container();

		amc.addCustomHeader("X-Custom", "value\r\nBcc: " + INJECTED_ADDRESS);

		final String headers = headersOf(amc);

		assertNoInjectedHeader(headers);
		assertTrue("the header must survive on a single line", headers.contains("X-Custom: valueBcc: " + INJECTED_ADDRESS));
	}

	@Test
	public void testALineBreakInARecipientNameStaysInsideItsHeader() throws Exception {

		final AdvancedMailContainer amc = container();

		amc.clearTo();
		amc.addTo("recipient@example.com", "Recipient\r\nBcc: " + INJECTED_ADDRESS);

		final String headers = headersOf(amc);

		assertNoInjectedHeader(headers);
		assertTrue("the recipient must survive on a single line", headers.contains("To: \"RecipientBcc: " + INJECTED_ADDRESS + "\" <recipient@example.com>"));
	}

	@Test
	public void testALineBreakInTheSubjectStaysInsideItsHeader() throws Exception {

		final AdvancedMailContainer amc = container();

		amc.setSubject("Subject\r\nBcc: " + INJECTED_ADDRESS);

		final String headers = headersOf(amc);

		assertNoInjectedHeader(headers);
		assertTrue("the subject must survive on a single line", headers.contains("Subject: SubjectBcc: " + INJECTED_ADDRESS));
	}

	/**
	 * The attachment name is script-controlled - mail_add_attachment(file, name) takes it verbatim,
	 * and without a name it is the uploaded file's own name. It ends up in the headers of the MIME
	 * part, where a line break starts a header of its own just as it does at the top of the message.
	 */
	@Test
	public void testALineBreakInAnAttachmentNameDoesNotAddAHeader() throws Exception {

		final AdvancedMailContainer amc        = container();
		final DynamicMailAttachment attachment = new DynamicMailAttachment();

		attachment.setName("invoice\r\nX-Injected: yes\r\n.pdf");
		attachment.setDisposition(EmailAttachment.ATTACHMENT);
		attachment.setDataSource(new ByteArrayDataSource("data".getBytes(StandardCharsets.UTF_8), "application/pdf"));

		amc.addAttachment(attachment);

		final String message = messageOf(amc);

		for (final String line : message.split("\r\n|\n")) {

			assertFalse("no header line may be added by the attachment name: " + line, line.startsWith("X-Injected"));
		}
	}

	@Test
	public void testAValueWithoutALineBreakIsLeftAlone() {

		assertEquals("Recipient Name", MailHelper.stripLineBreaks("Recipient Name"));
		assertEquals(null, MailHelper.stripLineBreaks(null));
	}

	private AdvancedMailContainer container() {

		final AdvancedMailContainer amc = new AdvancedMailContainer();

		amc.setFrom("sender@example.com", "Sender");
		amc.addTo("recipient@example.com", "Recipient");
		amc.setSubject("Subject");
		amc.setTextContent("text");

		return amc;
	}

	/**
	 * Builds the message and returns the header block of the bytes that would be sent.
	 */
	private String headersOf(final AdvancedMailContainer amc) throws Exception {

		final String message   = messageOf(amc);
		final int endOfHeaders = message.indexOf("\r\n\r\n");

		return endOfHeaders > 0 ? message.substring(0, endOfHeaders) : message;
	}

	/**
	 * Builds the message and returns all of the bytes that would be sent, part headers included.
	 */
	private String messageOf(final AdvancedMailContainer amc) throws Exception {

		final HtmlEmail mail = MailHelper.createAdvancedMail(amc);

		mail.buildMimeMessage();

		final ByteArrayOutputStream out = new ByteArrayOutputStream();

		mail.getMimeMessage().writeTo(out);

		return out.toString(StandardCharsets.UTF_8);
	}

	private void assertNoInjectedHeader(final String headers) {

		for (final String line : headers.split("\r\n|\n")) {

			assertFalse("no header line may be added by the payload: " + line, line.startsWith("Bcc"));
		}
	}
}
