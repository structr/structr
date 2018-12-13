/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.mail.service;

import com.sun.mail.util.BASE64DecoderStream;
import io.netty.util.internal.ConcurrentSet;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.*;
import org.structr.api.service.Command;
import org.structr.api.service.RunnableService;
import org.structr.api.service.ServiceDependency;
import org.structr.api.service.StructrServices;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.mail.entity.Mail;
import org.structr.mail.entity.Mailbox;
import org.structr.schema.SchemaService;

import javax.mail.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ServiceDependency(SchemaService.class)
public class MailService extends Thread implements RunnableService {
	private static final Logger logger                  = LoggerFactory.getLogger(MailService.class.getName());
	private static final ExecutorService threadExecutor = Executors.newFixedThreadPool(10);
	private boolean run                                 = false;
	private Set<Class> supportedCommands                = null;
	private Set<Mailbox> processingMailboxes            = null;

	public static final SettingsGroup mailGroup         = new SettingsGroup("mail","Advanced Mail Configuration");
	public static final Setting<Integer> maxEmails      = new IntegerSetting(mailGroup,"Mail", "mail.maxEmails",25);
	public static final Setting<Integer> updateInterval = new IntegerSetting(mailGroup,"Mail", "mail.updateInterval",30000);

	public MailService() {
		super("MailService");

		supportedCommands = new LinkedHashSet<>();
		supportedCommands.add(FetchMailsCommand.class);

		processingMailboxes = new ConcurrentSet<>();

		super.setDaemon(true);
	}

	private String getText(Part p) throws MessagingException, IOException {

		if (p.isMimeType("text/*")) {

			return (String)p.getContent();

		} else if (p.isMimeType("multipart/alternative")) {
			Multipart mp = (Multipart)p.getContent();
			String text = null;

			for (int i = 0; i < mp.getCount(); i++) {
				Part bp = mp.getBodyPart(i);

				if (bp.isMimeType("text/plain")) {

					if (text == null) {

						text = getText(bp);
					}

				} else if (bp.isMimeType("text/html")) {

					String s = getText(bp);

					if (s != null) {

						return s;
					}

				} else {

					return getText(bp);

				}

			}

			return text;

		} else if (p.isMimeType("multipart/*")) {

			Multipart mp = (Multipart)p.getContent();

			for (int i = 0; i < mp.getCount(); i++) {
				String s = getText(mp.getBodyPart(i));

				if (s != null) {

					return s;
				}

			}
		}

		return null;
	}

	public void fetchMails(final Mailbox mb) {

		if (processingMailboxes.contains(mb)) {

			return;
		} else {

			processingMailboxes.add(mb);
		}

		MailFetchTask task = new MailFetchTask(mb);
		threadExecutor.submit(task);
	}

	private void fetchMailsForAllMailboxes() {
		App app = StructrApp.getInstance();
		try (Tx tx = app.tx()) {

			// Fetch mails for each mailbox found
			app.nodeQuery(Mailbox.class).getResultStream().forEach(this::fetchMails);
			tx.success();

		} catch (FrameworkException ex) {
			logger.error("Exception while trying to fetch mails for all mailboxes: " + ex.getMessage());
		}

	}

	@Override
	public void run() {
		logger.info("MailService started");

		Date lastUpdate = new Date();

		while (run) {

			if ( (new Date().getTime() - lastUpdate.getTime()) > updateInterval.getValue(30000) ) {

				fetchMailsForAllMailboxes();
				lastUpdate = new Date();
			}

			// let others act
			try { Thread.sleep(10); }
			catch (InterruptedException ex) { run = false; }
			catch(Throwable ignore) {}
		}
	}

	@Override
	public void startService() throws Exception {
		this.run = true;
		this.start();
	}

	@Override
	public void stopService() {
		this.run = false;
	}

	@Override
	public boolean runOnStartup() {
		return true;
	}

	@Override
	public void injectArguments(Command command) {
		command.setArgument("mailService", this);
	}

	@Override
	public boolean initialize(StructrServices services) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		return true;
	}

	@Override
	public void shutdown() {}

	@Override
	public void initialized() {}

	@Override
	public boolean isRunning() {
		return this.run;
	}

	@Override
	public boolean isVital() {
		return false;
	}

	@Override
	public boolean waitAndRetry() {
		return false;
	}

	@Override
	public String getModuleName() {
		return "advanced-mail";
	}

	private class MailFetchTask implements Runnable {
		private final Mailbox mailbox;

		public MailFetchTask(final Mailbox mailbox) {
			this.mailbox = mailbox;
		}

		@Override
		public void run() {
			try {

				String host = mailbox.getHost();
				String mailProtocol = mailbox.getMailProtocol().toString();
				String user = mailbox.getUser();
				String password = mailbox.getPassword();
				Integer port = mailbox.getPort();

				if (host == null || mailProtocol == null || user == null || password == null) {
					logger.warn("MailService::fetchMails: Could not retrieve mails from mailbox[" + mailbox.getUuid() + "], because not all required attributes were specified.");
					return;
				}

				Properties properties = new Properties();

				properties.put("mail." + mailProtocol + ".host", host);
				properties.put("mail." + mailProtocol + ".starttls.enable", "true");

				if (port != null) {
					properties.put("mail." + mailProtocol + ".port", port);
				}

				Session emailSession = Session.getDefaultInstance(properties);

				Store store = emailSession.getStore(mailProtocol);

				store.connect(host, user, password);

				Folder emailFolder = store.getFolder("INBOX");
				emailFolder.open(Folder.READ_ONLY);

				Message[] messages = emailFolder.getMessages();

				ArrayUtils.reverse(messages);

				App app = StructrApp.getInstance();

				for (int i = 0; i < messages.length; i ++) {
					// Limit fetched emails
					if (i >= maxEmails.getValue(25)) {
						break;
					}

					Message message = messages[i];

					PropertyMap pm = new PropertyMap();

					String from = Arrays.stream(message.getFrom()).map(Address::toString).reduce("", (a, b) -> a.equals("") ? b : a + "," + b);
					String to = Arrays.stream(message.getAllRecipients()).map(Address::toString).reduce("", (a, b) -> a.equals("") ? b : a + "," + b);

					try (Tx tx = app.tx()) {

						Mail existingMail = app.nodeQuery(Mail.class).and(StructrApp.key(Mail.class, "subject"), message.getSubject()).and(StructrApp.key(Mail.class, "from"), from).and(StructrApp.key(Mail.class, "to"),to).and(StructrApp.key(Mail.class, "receivedDate"),message.getReceivedDate()).and(StructrApp.key(Mail.class, "sentDate"),message.getSentDate()).getFirst();

						if (existingMail == null) {

							pm.put(StructrApp.key(Mail.class, "subject"), message.getSubject());
							pm.put(StructrApp.key(Mail.class, "from"), from);
							pm.put(StructrApp.key(Mail.class, "to"), to);
							pm.put(StructrApp.key(Mail.class, "folder"), message.getFolder().getFullName());
							pm.put(StructrApp.key(Mail.class, "receivedDate"), message.getReceivedDate());
							pm.put(StructrApp.key(Mail.class, "sentDate"), message.getSentDate());
							pm.put(StructrApp.key(Mail.class, "mailbox"), mailbox);

							// Handle content extraction
							String content = null;
							Object contentObj = message.getContent();
							if (contentObj instanceof Part) {

								content = getText((Part)contentObj);
							} else if (contentObj  instanceof Multipart) {

								content = getText(((Multipart) contentObj).getParent());
							} else if (contentObj instanceof InputStream) {

								logger.info("MailService: Can't process streamed content. Not implemented yet!");
							} else {

								content = contentObj.toString();
							}


							pm.put(StructrApp.key(Mail.class, "content"), content);

							app.create(Mail.class, pm);

						}

						tx.success();

					}

				}

				//close the store and folder objects
				emailFolder.close(false);
				store.close();

			} catch (MessagingException ex) {
				logger.error("Error while updating Mails: " + ex.getLocalizedMessage());
			} catch (FrameworkException | IOException ex) {
				logger.error("Error while updating Mails: " + ex.getMessage());
			} catch (Throwable ex) {
				logger.error("Error while updating Mails: " + ex.getLocalizedMessage());
			}

			processingMailboxes.remove(mailbox);
		}
	}
}
