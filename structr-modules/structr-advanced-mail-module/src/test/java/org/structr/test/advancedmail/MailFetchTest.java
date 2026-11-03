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
package org.structr.test.advancedmail;

import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.DummySSLSocketFactory;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.mail.MailSettings;
import org.structr.mail.entity.Mailbox;
import org.structr.mail.entity.traits.definitions.EMailMessageTraitDefinition;
import org.structr.mail.entity.traits.definitions.MailboxTraitDefinition;
import org.structr.mail.service.MailService;
import org.structr.schema.export.StructrSchema;
import org.structr.test.rest.common.StructrRestTestBase;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Fetches against GreenMail, an IMAP server that runs inside the test JVM on a free local port: nothing
 * leaves the machine.
 *
 * <p>Each test talks to the service directly instead of through the service layer, so it controls when a
 * fetch happens: the service never fetches on its own.
 */
public class MailFetchTest extends StructrRestTestBase {

	static {

		// GreenMail's IMAPS uses a self-signed certificate; this makes the default SSL socket factory accept it, and it
		// has to be set before anything in this JVM creates that factory
		Security.setProperty("ssl.SocketFactory.provider", DummySSLSocketFactory.class.getName());
	}

	private GreenMail greenMail      = null;
	private MailService mailService  = null;
	private final Session session    = Session.getInstance(new Properties());

	@BeforeMethod
	public void startMailServer() {

		greenMail = new GreenMail(ServerSetupTest.IMAPS.dynamicPort());
		greenMail.start();

		mailService = new MailService();
	}

	@AfterMethod(alwaysRun = true)
	public void stopMailServer() {

		if (mailService != null) {

			mailService.stopService();
		}

		if (greenMail != null) {

			greenMail.stop();
		}

		MailSettings.MaxEmails.setValue(MailSettings.MaxEmails.getDefaultValue());
	}

	@Test
	public void testAFetchStoresNewMessagesAndRecordsTheOutcome() throws Exception {

		final GreenMailUser user = greenMail.setUser("inbox@example.com", "inbox", "secret");

		deliver(user, "first", "second", "third");

		final String mailbox = createMailbox("inbox", "secret", null);

		fetch(mailbox);

		assertEquals("every message must be stored", List.of("first", "second", "third"), subjectsIn(mailbox));

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.getNodeById(StructrTraits.MAILBOX, mailbox);

			assertNotNull("the start of the fetch must be recorded",  node.getProperty(key(MailboxTraitDefinition.LAST_FETCH_STARTED_PROPERTY)));
			assertNotNull("its success must be recorded",              node.getProperty(key(MailboxTraitDefinition.LAST_FETCH_SUCCEEDED_PROPERTY)));
			assertNull("a successful fetch leaves no error",            node.getProperty(key(MailboxTraitDefinition.LAST_FETCH_ERROR_PROPERTY)));
			assertEquals("the number of new messages",               3, (int) (Integer) node.getProperty(key(MailboxTraitDefinition.LAST_FETCH_COUNT_PROPERTY)));
			assertTrue("the fetch state must name the folder",        ((String) node.getProperty(key(MailboxTraitDefinition.FETCH_STATE_PROPERTY))).contains("INBOX"));

			tx.success();
		}
	}

	@Test
	public void testFetchingAgainStoresOnlyWhatIsNew() throws Exception {

		final GreenMailUser user = greenMail.setUser("inbox@example.com", "inbox", "secret");
		final String mailbox     = createMailbox("inbox", "secret", null);

		deliver(user, "one", "two");
		fetch(mailbox);

		fetch(mailbox);
		assertEquals("a fetch without new mail must not store anything again", 2, subjectsIn(mailbox).size());

		deliver(user, "three");
		fetch(mailbox);

		assertEquals(List.of("one", "three", "two"), subjectsIn(mailbox));
		assertEquals("the last fetch stored exactly the new message", 1, lastFetchCount(mailbox));
	}

	@Test
	public void testAFirstFetchTakesOnlyTheNewestBatch() throws Exception {

		MailSettings.MaxEmails.setValue(3);

		final GreenMailUser user = greenMail.setUser("inbox@example.com", "inbox", "secret");

		deliver(user, "m1", "m2", "m3", "m4", "m5");

		final String mailbox = createMailbox("inbox", "secret", null);

		fetch(mailbox);

		// a mailbox connected for the first time does not import its whole history
		assertEquals(List.of("m3", "m4", "m5"), subjectsIn(mailbox));
	}

	@Test
	public void testABurstLargerThanTheBatchIsFetchedOverSeveralRuns() throws Exception {

		MailSettings.MaxEmails.setValue(3);

		final GreenMailUser user = greenMail.setUser("inbox@example.com", "inbox", "secret");
		final String mailbox     = createMailbox("inbox", "secret", null);

		deliver(user, "before");
		fetch(mailbox);

		// eight arrive between two fetches: with "the newest batch" five of them would be lost for good
		deliver(user, "b1", "b2", "b3", "b4", "b5", "b6", "b7", "b8");

		fetch(mailbox);
		assertEquals("one batch, oldest first", 4, subjectsIn(mailbox).size());

		fetch(mailbox);
		fetch(mailbox);

		assertEquals(List.of("b1", "b2", "b3", "b4", "b5", "b6", "b7", "b8", "before"), subjectsIn(mailbox));
	}

	@Test
	public void testTheSameMessageInTwoMailboxesIsStoredForEach() throws Exception {

		final GreenMailUser sales   = greenMail.setUser("sales@example.com", "sales", "secret");
		final GreenMailUser support = greenMail.setUser("support@example.com", "support", "secret");

		// one mail sent to both addresses: identical Message-ID in both mailboxes
		sales.deliver(message("to both", "<shared-1@example.com>"));
		support.deliver(message("to both", "<shared-1@example.com>"));

		final String salesBox   = createMailbox("sales", "secret", null);
		final String supportBox = createMailbox("support", "secret", null);

		fetch(salesBox);
		fetch(supportBox);

		assertEquals("the first mailbox gets its copy",  List.of("to both"), subjectsIn(salesBox));
		assertEquals("the second mailbox gets its own", List.of("to both"), subjectsIn(supportBox));
	}

	@Test
	public void testAMessageThatFailsToCommitCostsOnlyItself() throws Exception {

		// a type whose onCreate refuses one subject: that message fails when its transaction commits
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.addType("PickyMail");

			type.addTrait(StructrTraits.EMAIL_MESSAGE);
			type.addMethod("onCreate", "{ $.assert($.this.subject !== 'reject me', 422, 'rejected'); }");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();
		}

		final GreenMailUser user = greenMail.setUser("inbox@example.com", "inbox", "secret");

		deliver(user, "before", "reject me", "after");

		final String mailbox = createMailbox("inbox", "secret", "PickyMail");

		fetch(mailbox);

		assertEquals("the messages around the failing one must be stored", List.of("after", "before"), subjectsIn(mailbox));

		final String error = lastFetchError(mailbox);

		assertNotNull("the skipped message must be reported on the mailbox", error);
		assertTrue("the report names the message: " + error, error.contains("UID 2"));

		// and it is moved past: fetching again does not retry it, or anything else
		fetch(mailbox);
		assertEquals(0, lastFetchCount(mailbox));
	}

	@Test
	public void testAFailedMessageIsImportedByARetryAfterTheFix() throws Exception {

		// rejects one subject until a group named "allow" exists, which stands for fixing the cause
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.addType("PickyMail");

			type.addTrait(StructrTraits.EMAIL_MESSAGE);
			type.addMethod("onCreate", "{ $.assert($.this.subject !== 'reject me' || $.find('Group', { name: 'allow' }).length > 0, 422, 'rejected'); }");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();
		}

		final GreenMailUser user = greenMail.setUser("inbox@example.com", "inbox", "secret");

		deliver(user, "before", "reject me", "after");

		final String mailbox = createMailbox("inbox", "secret", "PickyMail");

		fetch(mailbox);

		assertEquals(List.of("after", "before"), subjectsIn(mailbox));
		assertTrue("the failed message is listed in the fetch state", fetchState(mailbox).contains("\"failed\":{\"2\":"));

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.GROUP, "allow");

			tx.success();
		}

		// the cause is fixed, but a plain fetch does not try it again
		fetch(mailbox);
		assertEquals(List.of("after", "before"), subjectsIn(mailbox));
		assertTrue("the failed message is still listed", fetchState(mailbox).contains("\"failed\":{\"2\":"));

		// requested while a plain fetch is still running, the retry runs right after it instead of being dropped
		try (final Tx tx = app.tx()) {

			final Mailbox node = app.getNodeById(StructrTraits.MAILBOX, mailbox).as(Mailbox.class);

			mailService.fetchMails(node, false);
			mailService.fetchMails(node, true);

			tx.success();
		}

		fetch(mailbox, false);

		assertEquals("the retry imports the failed message", List.of("after", "before", "reject me"), subjectsIn(mailbox));
		assertTrue("and removes it from the list: " + fetchState(mailbox), !fetchState(mailbox).contains("failed"));
	}

	@Test
	public void testAnAttachmentNameWithASlashIsStored() throws Exception {

		final GreenMailUser user    = greenMail.setUser("inbox@example.com", "inbox", "secret");
		final MimeMessage message   = message("with attachment", null);
		final MimeBodyPart text     = new MimeBodyPart();
		final MimeBodyPart pdf      = new MimeBodyPart();
		final MimeMultipart content = new MimeMultipart();

		text.setText("see attachment");
		pdf.setContent("%PDF-1.4", "application/pdf");
		pdf.setFileName("RE_2026/123456.pdf");

		content.addBodyPart(text);
		content.addBodyPart(pdf);

		message.setContent(content);
		message.saveChanges();

		user.deliver(message);

		final String mailbox = createMailbox("inbox", "secret", null);

		fetch(mailbox);

		// File.name may not contain a slash, and the failed validation used to skip the whole message
		assertEquals(List.of("with attachment"), subjectsIn(mailbox));

		try (final Tx tx = app.tx()) {

			assertNotNull("the attachment is stored with the slash replaced", app.nodeQuery(StructrTraits.FILE).name("RE_2026_123456.pdf").getFirst());

			tx.success();
		}
	}

	@Test
	public void testAnAttachmentWithoutDispositionIsStored() throws Exception {

		final GreenMailUser user    = greenMail.setUser("inbox@example.com", "inbox", "secret");
		final MimeMessage message   = message("with image", null);
		final MimeBodyPart text     = new MimeBodyPart();
		final MimeBodyPart png      = new MimeBodyPart();
		final MimeMultipart content = new MimeMultipart();

		text.setText("see image");
		png.setDataHandler(new DataHandler(new ByteArrayDataSource(new byte[] { (byte)0x89, 'P', 'N', 'G' }, "image/png; name=328.png")));
		png.setHeader("Content-Transfer-Encoding", "base64");

		content.addBodyPart(text);
		content.addBodyPart(png);

		message.setContent(content);
		message.saveChanges();

		assertNull("the part carries its name in the Content-Type only", png.getHeader("Content-Disposition"));

		user.deliver(message);

		final String mailbox = createMailbox("inbox", "secret", null);

		fetch(mailbox);

		assertEquals(List.of("with image"), subjectsIn(mailbox));

		try (final Tx tx = app.tx()) {

			assertNotNull("a part without Content-Disposition used to be dropped", app.nodeQuery(StructrTraits.FILE).name("328.png").getFirst());

			tx.success();
		}
	}

	@Test
	public void testAttachmentsWithTheSameNameKeepTheirNames() throws Exception {

		final GreenMailUser user = greenMail.setUser("inbox@example.com", "inbox", "secret");

		for (final String subject : List.of("first", "second")) {

			final MimeMessage message   = message(subject, null);
			final MimeBodyPart text     = new MimeBodyPart();
			final MimeBodyPart gif      = new MimeBodyPart();
			final MimeMultipart content = new MimeMultipart();

			text.setText("signature");
			gif.setDataHandler(new DataHandler(new ByteArrayDataSource(new byte[] { 'G', 'I', 'F', '8' }, "image/gif; name=image001.gif")));

			content.addBodyPart(text);
			content.addBodyPart(gif);

			message.setContent(content);
			message.saveChanges();

			user.deliver(message);
		}

		final String mailbox = createMailbox("inbox", "secret", null);

		fetch(mailbox);

		assertEquals(List.of("first", "second"), subjectsIn(mailbox));

		try (final Tx tx = app.tx()) {

			final Set<String> expected = new TreeSet<>();
			final Set<String> actual   = new TreeSet<>();

			for (final NodeInterface message : app.nodeQuery(StructrTraits.EMAIL_MESSAGE).getAsList()) {

				expected.add(message.getUuid() + "/image001.gif");
			}

			for (final NodeInterface file : app.nodeQuery(StructrTraits.FILE).getAsList()) {

				final String path = file.as(org.structr.web.entity.File.class).getPath();
				actual.add(path.substring(path.lastIndexOf('/', path.lastIndexOf('/') - 1) + 1));
			}

			assertEquals("each message has its own attachment folder, named after the message", expected, actual);

			tx.success();
		}
	}

	@Test
	public void testATextPartWithAnEmptyCharsetIsReadAsUtf8() throws Exception {

		final GreenMailUser user = greenMail.setUser("inbox@example.com", "inbox", "secret");
		final String raw         = String.join("\r\n",
			"From: sender@example.com",
			"To: inbox@example.com",
			"Subject: empty charset",
			"MIME-Version: 1.0",
			"Content-Type: multipart/mixed; boundary=\"outer\"",
			"",
			"--outer",
			"Content-Type: multipart/alternative; boundary=\"inner\"",
			"",
			"--inner",
			"Content-Type: text/plain; charset=\"\"",
			"Content-Transfer-Encoding: 8bit",
			"",
			"Grüße",
			"--inner--",
			"--outer--",
			""
		);

		user.deliver(new MimeMessage(session, new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8))));

		final String mailbox = createMailbox("inbox", "secret", null);

		fetch(mailbox);

		// the unreadable part used to fail the whole message on every retry
		assertEquals(List.of("empty charset"), subjectsIn(mailbox));

		try (final Tx tx = app.tx()) {

			final NodeInterface message = app.nodeQuery(StructrTraits.EMAIL_MESSAGE).getFirst();

			assertEquals("Grüße", message.getProperty(Traits.of(StructrTraits.EMAIL_MESSAGE).key(EMailMessageTraitDefinition.CONTENT_PROPERTY)));

			tx.success();
		}
	}

	@Test
	public void testClearingTheFetchStateStartsOverWithoutDuplicates() throws Exception {

		final GreenMailUser user = greenMail.setUser("inbox@example.com", "inbox", "secret");
		final String mailbox     = createMailbox("inbox", "secret", null);

		deliver(user, "one", "two");
		fetch(mailbox);

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.getNodeById(StructrTraits.MAILBOX, mailbox);

			node.unlockReadOnlyPropertiesOnce();
			node.setProperty(key(MailboxTraitDefinition.FETCH_STATE_PROPERTY), null);

			tx.success();
		}

		fetch(mailbox);

		assertEquals("starting over reads the same messages again, the duplicate check keeps them single", List.of("one", "two"), subjectsIn(mailbox));
		assertEquals(0, lastFetchCount(mailbox));
	}

	@Test
	public void testAMailboxThatCannotConnectRecordsTheError() throws Exception {

		final String mailbox;

		try (final Tx tx = app.tx()) {

			// a port nothing listens on, because the server was never started for it
			mailbox = createMailboxNode("nobody", "secret", null, freePort()).getUuid();

			tx.success();
		}

		fetch(mailbox);

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.getNodeById(StructrTraits.MAILBOX, mailbox);

			assertNotNull("the attempt must be recorded", node.getProperty(key(MailboxTraitDefinition.LAST_FETCH_STARTED_PROPERTY)));
			assertNull("it did not succeed",              node.getProperty(key(MailboxTraitDefinition.LAST_FETCH_SUCCEEDED_PROPERTY)));
			assertNotNull("the error must be recorded",    node.getProperty(key(MailboxTraitDefinition.LAST_FETCH_ERROR_PROPERTY)));

			tx.success();
		}
	}

	// ----- private methods -----

	private void deliver(final GreenMailUser user, final String... subjects) throws MessagingException {

		for (final String subject : subjects) {

			user.deliver(message(subject, null));
		}
	}

	private MimeMessage message(final String subject, final String messageId) throws MessagingException {

		final MimeMessage message = new MimeMessage(session) {

			@Override
			protected void updateMessageID() throws MessagingException {

				if (messageId != null) {

					setHeader("Message-ID", messageId);

				} else {

					super.updateMessageID();
				}
			}
		};

		message.setFrom("sender@example.com");
		message.setRecipients(Message.RecipientType.TO, "inbox@example.com");
		message.setSubject(subject);
		message.setSentDate(new Date());
		message.setText("Text of " + subject);
		message.saveChanges();

		return message;
	}

	private String createMailbox(final String user, final String password, final String overrideType) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final String uuid = createMailboxNode(user, password, overrideType, greenMail.getImaps().getPort()).getUuid();

			tx.success();

			return uuid;
		}
	}

	private NodeInterface createMailboxNode(final String user, final String password, final String overrideType, final int port) throws FrameworkException {

		final Traits traits = Traits.of(StructrTraits.MAILBOX);
		final List<NodeAttribute> attributes = new ArrayList<>(List.of(
			new NodeAttribute<>(traits.key("name"),                                          user + " mailbox"),
			new NodeAttribute<>(traits.key(MailboxTraitDefinition.HOST_PROPERTY),          "localhost"),
			new NodeAttribute<>(traits.key(MailboxTraitDefinition.MAIL_PROTOCOL_PROPERTY), "imaps"),
			new NodeAttribute<>(traits.key(MailboxTraitDefinition.PORT_PROPERTY),          port),
			new NodeAttribute<>(traits.key(MailboxTraitDefinition.USER_PROPERTY),          user),
			new NodeAttribute<>(traits.key(MailboxTraitDefinition.PASSWORD_PROPERTY),      password),
			new NodeAttribute<>(traits.key(MailboxTraitDefinition.FOLDERS_PROPERTY),       new String[] { "INBOX" })
		));

		if (overrideType != null) {

			attributes.add(new NodeAttribute<>(traits.key(MailboxTraitDefinition.OVERRIDE_MAIL_ENTITY_TYPE_PROPERTY), overrideType));
		}

		return app.create(StructrTraits.MAILBOX, attributes.toArray(new NodeAttribute[0]));
	}

	/** requests a fetch and waits for it, which runs in the background */
	private void fetch(final String mailboxUuid) throws Exception {

		fetch(mailboxUuid, false);
	}

	private void retry(final String mailboxUuid) throws Exception {

		fetch(mailboxUuid, true);
	}

	private void fetch(final String mailboxUuid, final boolean retryFailed) throws Exception {

		try (final Tx tx = app.tx()) {

			mailService.fetchMails(app.getNodeById(StructrTraits.MAILBOX, mailboxUuid).as(Mailbox.class), retryFailed);

			tx.success();
		}

		final long deadline = System.currentTimeMillis() + 60_000;

		while (mailService.isFetching(mailboxUuid)) {

			if (System.currentTimeMillis() > deadline) {

				fail("the fetch of " + mailboxUuid + " did not finish within a minute");
			}

			Thread.sleep(50);
		}
	}

	/** the subjects of the messages stored for one mailbox, sorted */
	private List<String> subjectsIn(final String mailboxUuid) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final Traits traits                         = Traits.of(StructrTraits.EMAIL_MESSAGE);
			final PropertyKey<NodeInterface> mailboxKey = traits.key(EMailMessageTraitDefinition.MAILBOX_PROPERTY);
			final PropertyKey<String> subjectKey        = traits.key(EMailMessageTraitDefinition.SUBJECT_PROPERTY);
			final TreeSet<String> subjects              = new TreeSet<>();
			final List<String> all                      = new ArrayList<>();

			for (final NodeInterface message : app.nodeQuery(StructrTraits.EMAIL_MESSAGE).getAsList()) {

				final NodeInterface owner = message.getProperty(mailboxKey);
				if (owner != null && mailboxUuid.equals(owner.getUuid())) {

					all.add(message.getProperty(subjectKey));
					subjects.add(message.getProperty(subjectKey));
				}
			}

			tx.success();

			assertEquals("no message may be stored twice for one mailbox: " + all, subjects.size(), all.size());

			return new ArrayList<>(subjects);
		}
	}

	private int lastFetchCount(final String mailboxUuid) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final Integer count = app.getNodeById(StructrTraits.MAILBOX, mailboxUuid).getProperty(key(MailboxTraitDefinition.LAST_FETCH_COUNT_PROPERTY));

			tx.success();

			return count != null ? count : -1;
		}
	}

	private String fetchState(final String mailboxUuid) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final String state = app.getNodeById(StructrTraits.MAILBOX, mailboxUuid).getProperty(key(MailboxTraitDefinition.FETCH_STATE_PROPERTY));

			tx.success();

			return state;
		}
	}

	private String lastFetchError(final String mailboxUuid) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final String error = app.getNodeById(StructrTraits.MAILBOX, mailboxUuid).getProperty(key(MailboxTraitDefinition.LAST_FETCH_ERROR_PROPERTY));

			tx.success();

			return error;
		}
	}

	private static <T> PropertyKey<T> key(final String name) {

		return Traits.of(StructrTraits.MAILBOX).key(name);
	}

	private static int freePort() throws Exception {

		try (final java.net.ServerSocket socket = new java.net.ServerSocket(0)) {

			return socket.getLocalPort();
		}
	}
}
