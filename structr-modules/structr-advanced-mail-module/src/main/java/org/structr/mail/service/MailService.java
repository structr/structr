/**
 * Copyright (C) 2010-2019 Structr GmbH
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

import com.google.gson.Gson;
import com.sun.mail.util.BASE64DecoderStream;
import com.sun.mail.util.MailConnectException;
import io.netty.util.internal.ConcurrentSet;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.neo4j.driver.v1.exceptions.NoSuchRecordException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.*;
import org.structr.api.service.Command;
import org.structr.api.service.RunnableService;
import org.structr.api.service.ServiceDependency;
import org.structr.api.service.StructrServices;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.mail.entity.EMailMessage;
import org.structr.mail.entity.Mailbox;
import org.structr.schema.SchemaService;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.structr.web.entity.Image;

import javax.mail.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ServiceDependency(SchemaService.class)
public class MailService extends Thread implements RunnableService {
	private static final Logger logger                      = LoggerFactory.getLogger(MailService.class.getName());
	private static final ExecutorService threadExecutor     = Executors.newCachedThreadPool();
	private boolean run                                     = false;
	private Set<Class> supportedCommands                    = null;
	private Set<Mailbox> processingMailboxes                = null;
	private int maxConnectionRetries                        = 5;

	public static final SettingsGroup mailGroup             = new SettingsGroup("mail","Advanced EMailMessage Configuration");
	public static final Setting<Integer> maxEmails          = new IntegerSetting(mailGroup,"EMailMessage", "mail.maxEmails",25);
	public static final Setting<Integer> updateInterval     = new IntegerSetting(mailGroup,"EMailMessage", "mail.updateInterval",30000);
	public static final Setting<String> attachmentBasePath  = new StringSetting(mailGroup,"EMailMessage", "mail.attachmentBasePath","/mail/attachments");

	//////////////////////////////////////////////////////////////// Public Methods

	public MailService() {
		super("MailService");

		supportedCommands = new LinkedHashSet<>();
		supportedCommands.add(FetchMailsCommand.class);
		supportedCommands.add(FetchFoldersCommand.class);

		processingMailboxes = new ConcurrentSet<>();

		super.setDaemon(true);
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

	public Iterable<String> fetchFolders(final Mailbox mb) {

		if (mb.getHost() != null && mb.getMailProtocol() != null && mb.getUser() != null && mb.getPassword() != null && mb.getFolders() != null) {

			final Store store = connectToStore(mb);

			List<String> folders = new ArrayList<>();

			try {
				final Folder defaultFolder = store.getDefaultFolder();
				if (defaultFolder != null) {

					final Folder[] folderList = defaultFolder.list("*");

					for (final Folder folder : folderList) {

						if ((folder.getType() & javax.mail.Folder.HOLDS_MESSAGES) != 0) {

							folders.add(folder.getFullName());
						}
					}
				}

			} catch (MessagingException ex) {

				logger.error("Exception while trying to fetch mailbox folders.", ex);
			}

			return folders;

		} else {

			logger.warn("Could not retrieve folders for mailbox[" + mb.getUuid() + "] since not all required attributes were specified.");
			return new ArrayList<>();
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

	//////////////////////////////////////////////////////////////// Private Methods

	// Returns attachment UUID to append to the mail to be created
	private File extractFileAttachment(final Mailbox mb, final Part p) {
		File file = null;

		try {

			Class fileClass = p.getContentType().toLowerCase().startsWith("image/") ? Image.class : File.class;

			final App app = StructrApp.getInstance();

			try (final Tx tx = app.tx()) {

				final Date date = new Date();
				final Calendar cal = Calendar.getInstance();

				cal.setTime(date);

				final String path = (attachmentBasePath.getValue() + "/" + Integer.toString(cal.get(Calendar.YEAR)) + "/" + Integer.toString(cal.get(Calendar.MONTH)) + "/" + Integer.toString(cal.get(Calendar.DAY_OF_MONTH)) + "/" + mb.getUuid());

				org.structr.web.entity.Folder fileFolder = FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), path);
				file = FileHelper.createFile(SecurityContext.getSuperUserInstance(), p.getInputStream(), p.getContentType(), fileClass, p.getFileName(), fileFolder);

				tx.success();

			} catch (IOException | FrameworkException ex) {

				logger.error("Exception while extracting file attachment: ", ex);
			}


		} catch (MessagingException ex) {

			logger.error("Exception while extracting file attachment: ", ex);
		}

		return file;

	}

	private Map<String,String> handleMultipart(final Mailbox mb, Multipart p, List<File> attachments) {

		Map<String,String> result = new HashMap<>();

		try {

			for (int i = 0; i < p.getCount(); i++) {
				final String htmlContent = result.get("htmlContent") != null ? result.get("htmlContent") : "";
				final String content = result.get("content") != null ? result.get("content") : "";

				BodyPart part = (BodyPart) p.getBodyPart(i);
				if (part.getContentType().contains("multipart")) {

					Map<String,String> subResult = handleMultipart(mb, (Multipart)part.getContent(), attachments);

					if (subResult.get("content") != null) {
						result.put("content", content.concat(subResult.get("content")));
					}

					if (subResult.get("htmlContent") != null) {
						result.put("htmlContent", htmlContent.concat(subResult.get("htmlContent")));
					}


				} else if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) || (part.getContentType().toLowerCase().contains("image/") && Part.INLINE.equalsIgnoreCase(part.getDisposition()))) {

					File file = extractFileAttachment(mb, part);

					if (file != null) {

						attachments.add(file);
					}
				} else {

					if (part.isMimeType("text/html")) {

						result.put("htmlContent", htmlContent.concat(getText(part)));
					} else if (part.isMimeType("text/plain")) {

						result.put("content", content.concat(getText(part)));
					} else {

						logger.warn("Cannot handle content type given by email part. Given metadata is either faulty or specific implementation is missing. Type: {}, Mailbox: {}, Content: {}", part.getContentType(), mb.getUuid(), part.getContent().toString());
					}
				}
			}

			return result;
		} catch (MessagingException | IOException ex) {
			logger.error("Error while handling multipart message: ", ex);
		}

		return null;

	}

	private String getText(Part p) throws MessagingException, IOException {

		if (p.isMimeType("text/plain") || p.isMimeType("text/html")) {

			Object content = p.getContent();

			if (!(content instanceof BASE64DecoderStream)) {

				return (String)p.getContent();
			} else if(p.getContentType().equals("base64")) {

				BASE64DecoderStream contentStream = (BASE64DecoderStream)content;

				return contentStream.toString();
			} else {

				return null;
			}
		}

		return null;
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

	private Store connectToStore(final Mailbox mailbox) {
		String host = mailbox.getHost();
		String mailProtocol = mailbox.getMailProtocol().toString();
		String user = mailbox.getUser();
		String password = mailbox.getPassword();
		Integer port = mailbox.getPort();
		String[] folders = mailbox.getFolders();

		try {

			if (host == null || mailProtocol == null || user == null || password == null || folders == null) {
				logger.warn("MailService::fetchMails: Could not retrieve mails from mailbox[" + mailbox.getUuid() + "], because not all required attributes were specified.");
				processingMailboxes.remove(mailbox);
				return null;
			}

			Properties properties = new Properties();

			properties.put("mail." + mailProtocol + ".host", host);

			switch (mailProtocol) {
				case "pop3":
					properties.put("mail." + mailProtocol + ".starttls.enable", "true");
					break;
				case "imaps":
					properties.put("mail." + mailProtocol + ".ssl.enable", "true");
					break;
			}

			if (port != null) {
				properties.put("mail." + mailProtocol + ".port", port);
			}


			Session emailSession = Session.getDefaultInstance(properties);

			Store store = emailSession.getStore(mailProtocol);

			int retries = 0;
			while (retries < maxConnectionRetries && !store.isConnected()) {
				try {

					store.connect(host, user, password);

				} catch (AuthenticationFailedException ex) {

					logger.warn("Could not authenticate mailbox[" + mailbox.getUuid() + "]: " + ex.getMessage());
					break;
				} catch (MailConnectException ex) {
					// silently catch connection exception
					retries++;
					Thread.sleep(100);
					if (retries >= maxConnectionRetries) {
						throw ex;
					}
				}
			}

			return store;

		} catch (AuthenticationFailedException ex) {
			logger.warn("Authentication failed for Mailbox[" + mailbox.getUuid() + "].");
		} catch (MailConnectException ex) {
			logger.error("Could not connect to mailbox [" + mailbox.getUuid() + "]: " + ex.getMessage());
		} catch (MessagingException ex) {
			logger.error("Error while updating Mails: ", ex);
		} catch (InterruptedException ex) {
			logger.error("Interrupted while trying to connect to email store.", ex);
		}

		return null;
	}

	//////////////////////////////////////////////////////////////// Nested classes
	private class MailFetchTask implements Runnable {
		private final Mailbox mailbox;

		public MailFetchTask(final Mailbox mailbox) {
			this.mailbox = mailbox;
		}

		@Override
		public void run() {
			try {

				String[] folders = mailbox.getFolders();

				if (folders == null) {
					folders = new String[]{};
				}

				final Store store = connectToStore(mailbox);

				if (store.isConnected()) {

					for (final String folder : folders) {

						fetchMessagesInFolder(store.getFolder(folder));
					}

					store.close();

				}

			} catch (MessagingException ex) {
				logger.error("Error while updating Mails: ", ex);
			} catch (Throwable ex) {
				logger.error("Error while updating Mails: ", ex);
			}

			processingMailboxes.remove(mailbox);
		}

		private void fetchMessagesInFolder(final Folder folder) {

			if (folder != null) {

				try {

					Gson gson = new Gson();

					folder.open(Folder.READ_ONLY);

					Message[] messages = folder.getMessages();

					ArrayUtils.reverse(messages);

					App app = StructrApp.getInstance();

					for (int i = 0; i < messages.length; i++) {
						// Limit fetched emails
						if (i >= maxEmails.getValue(25)) {
							break;
						}

						Message message = messages[i];

						PropertyMap pm = new PropertyMap();

						String from = message.getFrom() != null ? Arrays.stream(message.getFrom()).map(Address::toString).reduce("", (a, b) -> a.equals("") ? b : a + "," + b) : "";
						String to = message.getAllRecipients() != null ? Arrays.stream(message.getAllRecipients()).map((a) -> a != null ? a.toString() : "").reduce("", (a, b) -> a.equals("") ? b : a + "," + b) : "";

						try (Tx tx = app.tx()) {

							// Allow mail instance class to be overriden by custom types to enable special mail handling
							Class<? extends EMailMessage> entityClass = EMailMessage.class;

							final String entityType = mailbox.getOverrideMailEntityType();
							if (entityType != null && entityType.length() > 0) {
								Class overrideClass = StructrApp.getConfiguration().getNodeEntityClass(entityType);
								if (overrideClass != null && EMailMessage.class.isAssignableFrom(overrideClass)) {

									entityClass = overrideClass;
								} else {

									logger.warn("Mailbox[" + mailbox.getUuid() + "] has invalid overrideMailEntityType set. Given type is not found or does not extend EMailMessage.");
								}
							}


							String messageId = null;
							String inReplyTo = null;

							Enumeration en = message.getAllHeaders();
							Map<String, String> headers = new HashMap<>();
							while (en.hasMoreElements()) {
								Header header = (Header) en.nextElement();
								if (header.getName().equals("Message-ID") || header.getName().equals("Message-Id")) {
									messageId = header.getValue();
								} else if (header.getName().equals("In-Reply-To") || header.getName().equals("References")) {
									inReplyTo = header.getValue();
								}
								headers.put(header.getName(), header.getValue());
							}

							EMailMessage existingEMailMessage = null;

							// Try to match via messageId first
							if (messageId != null) {
								existingEMailMessage = app.nodeQuery(entityClass).and(StructrApp.key(EMailMessage.class, "messageId"), messageId).getFirst();
							}
							// If messageId can't be matched, use fallback
							if (existingEMailMessage == null) {
								existingEMailMessage = app.nodeQuery(entityClass).and(StructrApp.key(EMailMessage.class, "subject"), message.getSubject()).and(StructrApp.key(EMailMessage.class, "from"), from).and(StructrApp.key(EMailMessage.class, "to"), to).and(StructrApp.key(EMailMessage.class, "receivedDate"), message.getReceivedDate()).and(StructrApp.key(EMailMessage.class, "sentDate"), message.getSentDate()).getFirst();
							}

							if (existingEMailMessage == null) {

								pm.put(StructrApp.key(EMailMessage.class, "subject"), message.getSubject());
								pm.put(StructrApp.key(EMailMessage.class, "from"), from);


								Pattern pattern = Pattern.compile(".* <(.*)>");
								Matcher matcher = pattern.matcher(from);
								if (matcher.matches()) {

									pm.put(StructrApp.key(EMailMessage.class, "fromMail"), matcher.group(1));
								} else {
									pm.put(StructrApp.key(EMailMessage.class, "fromMail"), from);
								}
								pm.put(StructrApp.key(EMailMessage.class, "to"), to);
								pm.put(StructrApp.key(EMailMessage.class, "folder"), message.getFolder().getFullName());
								pm.put(StructrApp.key(EMailMessage.class, "receivedDate"), message.getReceivedDate());
								pm.put(StructrApp.key(EMailMessage.class, "sentDate"), message.getSentDate());
								pm.put(StructrApp.key(EMailMessage.class, "mailbox"), mailbox);
								pm.put(StructrApp.key(EMailMessage.class, "header"), gson.toJson(headers));

								if (messageId != null) {
									pm.put(StructrApp.key(EMailMessage.class, "messageId"), messageId);
								} else if (inReplyTo != null) {
									pm.put(StructrApp.key(EMailMessage.class, "inReplyTo"), inReplyTo);
								}

								// Handle content extraction
								String content = null;
								String htmlContent = null;
								Object contentObj = message.getContent();

								List<File> attachments = new ArrayList<>();

								if (message.getContentType().contains("multipart")) {

									Map<String, String> result = handleMultipart(mailbox, (Multipart)contentObj, attachments);
									content = result.get("content");
									htmlContent = result.get("htmlContent");
								} else if (message.getContentType().contains("text/plain")){

									content = contentObj.toString();
								} else if (message.getContentType().contains("text/html")) {

									htmlContent = contentObj.toString();
								}

								pm.put(StructrApp.key(EMailMessage.class, "content"), content);
								pm.put(StructrApp.key(EMailMessage.class, "htmlContent"), htmlContent);
								pm.put(StructrApp.key(EMailMessage.class, "attachedFiles"), attachments);

								app.create(entityClass, pm);

							}

							tx.success();

						}

					}

					//close the store and folder objects
					folder.close(false);

				} catch (MessagingException ex) {
					logger.error("Error while updating Mails: ", ex);
				} catch (FrameworkException | IOException ex) {
					logger.error("Error while updating Mails: ", ex);
				} catch (Throwable ex) {
					logger.error("Error while updating Mails: ", ex);
				}
			}

		}
	}
}
