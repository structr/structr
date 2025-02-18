/*
 * Copyright (C) 2010-2024 Structr GmbH
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
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.IntegerSetting;
import org.structr.api.config.Setting;
import org.structr.api.config.Settings;
import org.structr.api.config.StringSetting;
import org.structr.api.service.*;
import org.structr.common.AccessControllable;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.AdvancedMailContainer;
import org.structr.common.helper.DynamicMailAttachment;
import org.structr.common.mail.MailServiceInterface;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.Traits;
import org.structr.mail.entity.Mailbox;
import org.structr.schema.SchemaService;
import org.structr.web.common.FileHelper;

import javax.activation.DataSource;
import javax.mail.*;
import javax.mail.internet.MimeUtility;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ServiceDependency(SchemaService.class)
@StopServiceForMaintenanceMode
public class MailService extends Thread implements RunnableService, MailServiceInterface {

	private static final Logger logger                      = LoggerFactory.getLogger(MailService.class.getName());
	private static final ExecutorService threadExecutor     = Executors.newCachedThreadPool();
	private boolean run                                     = false;
	private Set<Class> supportedCommands                    = null;
	private Set<Mailbox> processingMailboxes                = null;
	private int maxConnectionRetries                        = 5;

	public static final Setting<Integer> maxEmails          = new IntegerSetting(Settings.smtpGroup, "MailService", "mail.maxemails",          25,                  "The number of mails which are checked");
	public static final Setting<Integer> updateInterval     = new IntegerSetting(Settings.smtpGroup, "MailService", "mail.updateinterval",     30000,               "The interval in which the mailbox is checked. Unit is milliseconds");
	public static final Setting<String> attachmentBasePath  = new StringSetting (Settings.smtpGroup, "MailService", "mail.attachmentbasepath", "/mail/attachments", "The path in structrs virtual filesystem where attachments are downloaded to");

	public MailService() {

		super("MailService");

		supportedCommands = new LinkedHashSet<>();
		supportedCommands.add(FetchMailsCommand.class);
		supportedCommands.add(FetchFoldersCommand.class);

		processingMailboxes = ConcurrentHashMap.newKeySet();

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

			if (store.isConnected()) {

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
	public ServiceResult initialize(StructrServices services, String serviceName) throws ReflectiveOperationException {
		return new ServiceResult(true);
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

	@Override
	public NodeInterface saveOutgoingMessage(final SecurityContext securityContext, final AdvancedMailContainer amc, final String messageId) {

		NodeInterface outgoingMessage = null;

		final App app = StructrApp.getInstance(securityContext);

		try (final Tx tx = app.tx()) {

			final Traits traits = Traits.of("EmailMessage");
			PropertyMap props = new PropertyMap();

			props.put(traits.key("from"),           amc.getDisplayName(amc.getFromName(), amc.getFromAddress()));
			props.put(traits.key("fromMail"),       amc.getFromAddress());
			props.put(traits.key("to"),             amc.getCombinedDisplayNames(amc.getTo()));
			props.put(traits.key("subject"),        amc.getSubject());
			props.put(traits.key("content"),        amc.getTextContent());
			props.put(traits.key("htmlContent"),    amc.getHtmlContent());
			props.put(traits.key("sentDate"),       new Date());

			props.put(traits.key("messageId"),      messageId);
			props.put(traits.key("inReplyTo"),      amc.getInReplyTo());

			props.put(traits.key("header"),         new Gson().toJson(amc.getCustomHeaders()));

			props.put(traits.key("replyTo"),        amc.getCombinedDisplayNames(amc.getReplyTo()));
			props.put(traits.key("bcc"),            amc.getCombinedDisplayNames(amc.getBcc()));


			if (amc.getAttachments().size() > 0) {

				final ArrayList concreteAttachedFiles = new ArrayList();

				for (final DynamicMailAttachment attachment : amc.getAttachments()) {

					final NodeInterface savedFile = handleOutgoingMailAttachment(securityContext, attachment);

					if (savedFile != null) {
						concreteAttachedFiles.add(savedFile);
					}
				}

				props.put(traits.key("attachedFiles"), concreteAttachedFiles);
			}

			// not setting folder/receivedDate
//			props.put(StructrApp.key(entityType, "folder"), null);
//			props.put(StructrApp.key(entityType, "receivedDate"), null);

			outgoingMessage = app.create("EMailMessage", props);

			tx.success();

		} catch (Throwable t) {

			logger.warn("Error creating outgoing mail!", t);
		}

		return outgoingMessage;
	}

	private NodeInterface handleOutgoingMailAttachment(final SecurityContext securityContext, final DynamicMailAttachment dma) {

		NodeInterface file = null;

		final DataSource ds   = dma.getDataSource();
		final String fileType = ds.getContentType().toLowerCase().startsWith("image/") ? "Image" : "File";

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final String path = getStoragePath("/outgoing", new Date());

			NodeInterface fileFolder = FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), path);

			file = FileHelper.createFile(SecurityContext.getSuperUserInstance(), ds.getInputStream(), ds.getContentType(), fileType, dma.getName(), fileFolder.as(org.structr.web.entity.Folder.class));

			final Principal owner = securityContext.getUser(false);
			if (owner != null) {

				file.as(AccessControllable.class).setProperty(Traits.of("NodeInterface").key("owner"), securityContext.getUser(false));
			}

			tx.success();

		} catch (IOException | FrameworkException ex) {

			logger.error("Exception while creating file attachment for outgoing message: ", ex);
		}

		return file;
	}

	//////////////////////////////////////////////////////////////// Private Methods

	private String getStoragePath (final String lastPathPart, final Date receivedDate) {

		final Calendar cal = Calendar.getInstance();

		if (receivedDate != null) {

			cal.setTime(receivedDate);
		}

		return (attachmentBasePath.getValue() + "/" + Integer.toString(cal.get(Calendar.YEAR)) + "/" + Integer.toString(cal.get(Calendar.MONTH) + 1) + "/" + Integer.toString(cal.get(Calendar.DAY_OF_MONTH)) + "/" + lastPathPart);
	}

	// Returns attachment UUID to append to the mail to be created
	private NodeInterface extractFileAttachment(final Mailbox mb, final Message m, final Part p) {

		NodeInterface file = null;

		try {

			final String fileType = p.getContentType().toLowerCase().startsWith("image/") ? "Image" : "File";

			final App app = StructrApp.getInstance();

			try (final Tx tx = app.tx()) {

				NodeInterface fileFolder = FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), getStoragePath(mb.getUuid(), m.getReceivedDate()));

				try {

					String fileName = p.getFileName();

					if (fileName == null) {

						fileName = NodeServiceCommand.getNextUuid();

					} else {

						fileName = decodeText(fileName);

					}

					file = FileHelper.createFile(SecurityContext.getSuperUserInstance(), p.getInputStream(), p.getContentType(), fileType, fileName, fileFolder.as(org.structr.web.entity.Folder.class));

				} catch (FrameworkException ex) {

					logger.warn("EMail in mailbox[" + mb.getUuid() + "] attachment has invalid name. Using random UUID as fallback.");
					file = FileHelper.createFile(SecurityContext.getSuperUserInstance(), p.getInputStream(), p.getContentType(), fileType, NodeServiceCommand.getNextUuid(), fileFolder.as(org.structr.web.entity.Folder.class));
				}

				tx.success();

			} catch (IOException | FrameworkException ex) {

				logger.error("Exception while extracting file attachment: ", ex);
			}


		} catch (MessagingException ex) {

			logger.error("Exception while extracting file attachment: ", ex);
		}

		return file;
	}

	private Map<String,String> handleMultipart(final Mailbox mb, final Message message, final Multipart p, final List<NodeInterface> attachments) {

		final Map<String,String> result = new HashMap<>();

		try {

			for (int i = 0; i < p.getCount(); i++) {

				final String htmlContent = result.get("htmlContent") != null ? result.get("htmlContent") : "";
				final String content     = result.get("content") != null ? result.get("content") : "";

				BodyPart part = (BodyPart) p.getBodyPart(i);
				if (part.getContent() instanceof Multipart) {

					final Map<String,String> subResult = handleMultipart(mb, message, (Multipart)part.getContent(), attachments);

					if (subResult.get("content") != null) {
						result.put("content", content.concat(subResult.get("content")));
					}

					if (subResult.get("htmlContent") != null) {
						result.put("htmlContent", htmlContent.concat(subResult.get("htmlContent")));
					}


				} else if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) || (part.getContentType().toLowerCase().contains("image/") && Part.INLINE.equalsIgnoreCase(part.getDisposition())) || part.getContentType().toLowerCase().contains("application/pdf")) {

					final NodeInterface file = extractFileAttachment(mb, message, part);

					if (file != null) {

						attachments.add(file);
					}

				} else {

					if (part.isMimeType("text/html")) {

						result.put("htmlContent", htmlContent.concat(getText(part)));

					} else if (part.isMimeType("text/plain")) {

						result.put("content", content.concat(getText(part)));

					} else if (!part.isMimeType("message/delivery-status")){

						logger.warn("Cannot handle content type given by email part. Given metadata is either faulty or specific implementation is missing. Type: {}, Mailbox: {}, Content: {}, Subject; {}", part.getContentType(), mb.getUuid(), part.getContent().toString(), message.getSubject());
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

			final Object content = p.getContent();

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

		final App app = StructrApp.getInstance();

		try (Tx tx = app.tx()) {

			// Fetch mails for each mailbox found
			app.nodeQuery("Mailbox").getResultStream().forEach(n -> fetchMails(n.as(Mailbox.class)));
			tx.success();

		} catch (FrameworkException ex) {
			logger.error("Exception while trying to fetch mails for all mailboxes: " + ex.getMessage());
		}
	}

	private Store connectToStore(final Mailbox mailbox) {

		final String host = mailbox.getHost();
		final String mailProtocol = mailbox.getMailProtocol().toString();
		final String user = mailbox.getUser();
		final String password = mailbox.getPassword();
		final Integer port = mailbox.getPort();
		final String[] folders = mailbox.getFolders();

		try {

			if (host == null || mailProtocol == null || user == null || password == null || folders == null) {

				logger.warn("MailService::fetchMails: Could not retrieve mails from mailbox[" + mailbox.getUuid() + "], because not all required attributes were specified.");
				processingMailboxes.remove(mailbox);
				return null;
			}

			final Properties properties = new Properties();

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


			final Session emailSession = Session.getDefaultInstance(properties);
			final Store store          = emailSession.getStore(mailProtocol);

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

	private String decodeText (final String text) {

		try {

			return MimeUtility.decodeText(text);

		} catch (UnsupportedEncodingException ex) {

			logger.warn("UnsupportedEncodingException for input '{}'. Returning as is.", text);
			return text;
		}
	}

	//////////////////////////////////////////////////////////////// Nested classes
	private class MailFetchTask implements Runnable {
		private final Mailbox mailbox;

		public MailFetchTask(final Mailbox mailbox) {
			this.mailbox = mailbox;
		}

		@Override
		public void run() {

			try (final Tx tx = StructrApp.getInstance().tx()) {

				String[] folders = mailbox.getFolders();

				if (folders == null) {
					folders = new String[]{};
				}

				final Store store = connectToStore(mailbox);

				if (store != null && store.isConnected()) {

					for (final String folder : folders) {

						fetchMessagesInFolder(store.getFolder(folder));
					}

					store.close();
				}

				tx.success();

			} catch (MessagingException ex) {
				logger.error("Error while updating Mails: ", ex);
			} catch (Throwable ex) {
				logger.error("Error while updating Mails: ", ex);
			}

			processingMailboxes.remove(mailbox);
		}

		private void fetchMessagesInFolder (final Folder folder) {

			if (folder != null) {

				try {

					final Gson gson = new Gson();

					folder.open(Folder.READ_ONLY);

					final Message[] messages = folder.getMessages();

					ArrayUtils.reverse(messages);

					final App app = StructrApp.getInstance();

					for (int i = 0; i < messages.length; i++) {

						// Limit fetched emails
						if (i >= maxEmails.getValue(25)) {
							break;
						}

						final Message message = messages[i];
						final PropertyMap pm  = new PropertyMap();

						final String from = message.getFrom() != null ? Arrays.stream(message.getFrom()).map((a) -> a != null ? decodeText(a.toString()) : "").reduce("", (a, b) -> a.equals("") ? b : a + "," + b) : "";
						final String to   = message.getRecipients(Message.RecipientType.TO) != null ? Arrays.stream(message.getRecipients(Message.RecipientType.TO)).map((a) -> a != null ? decodeText(a.toString()) : "").reduce("", (a, b) -> a.equals("") ? b : a + "," + b) : "";
						final String cc   = message.getRecipients(Message.RecipientType.CC) != null ? Arrays.stream(message.getRecipients(Message.RecipientType.CC)).map((a) -> a != null ? decodeText(a.toString()) : "").reduce("", (a, b) -> a.equals("") ? b : a + "," + b) : "";
						final String bcc  = message.getRecipients(Message.RecipientType.BCC) != null ? Arrays.stream(message.getRecipients(Message.RecipientType.BCC)).map((a) -> a != null ? decodeText(a.toString()) : "").reduce("", (a, b) -> a.equals("") ? b : a + "," + b) : "";

						// Allow mail instance class to be overriden by custom types to enable special mail handling
						String entityType   = "EMailMessage";
						String overrideType = mailbox.getOverrideMailEntityType();

						if (StringUtils.isNotBlank(overrideType)) {

							if (Traits.exists(overrideType) && Traits.of(overrideType).contains("EMailMessage")) {

								entityType = overrideType;

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

						final Traits traits = Traits.of(entityType);
						NodeInterface existingEMailMessage = null;

						// Try to match via messageId first
						if (messageId != null) {
							existingEMailMessage = app.nodeQuery(entityType).and(traits.key("messageId"), messageId).getFirst();
						}
						// If messageId can't be matched, use fallback
						if (existingEMailMessage == null) {
							existingEMailMessage = app.nodeQuery(entityType).and(traits.key("subject"), message.getSubject()).and(traits.key("from"), from).and(traits.key("to"), to).and(traits.key("receivedDate"), message.getReceivedDate()).and(traits.key("sentDate"), message.getSentDate()).getFirst();
						}

						if (existingEMailMessage == null) {

							pm.put(traits.key("subject"), message.getSubject());
							pm.put(traits.key("from"), from);


							final Pattern pattern = Pattern.compile(".* <(.*)>");
							final Matcher matcher = pattern.matcher(from);
							if (matcher.matches()) {
								pm.put(traits.key("fromMail"), matcher.group(1));
							} else {
								pm.put(traits.key("fromMail"), from);
							}

							pm.put(traits.key("to"), to);
							pm.put(traits.key("cc"), cc);
							pm.put(traits.key("bcc"), bcc);
							pm.put(traits.key("folder"), message.getFolder().getFullName());
							pm.put(traits.key("receivedDate"), message.getReceivedDate());
							pm.put(traits.key("sentDate"), message.getSentDate());
							pm.put(traits.key("mailbox"), mailbox);
							pm.put(traits.key("header"), gson.toJson(headers));

							if (messageId != null) {
								pm.put(traits.key("messageId"), messageId);
							}

							if (inReplyTo != null) {
								pm.put(traits.key("inReplyTo"), inReplyTo);
							}

							// Handle content extraction
							String content = null;
							String htmlContent = null;
							final Object contentObj = message.getContent();

							final List<NodeInterface> attachments = new ArrayList<>();

							if (message.getContentType().contains("multipart")) {

								final Map<String, String> result = handleMultipart(mailbox, message, (Multipart)contentObj, attachments);
								content = result.get("content");
								htmlContent = result.get("htmlContent");

							} else if (message.getContentType().contains("text/plain")){

								content = contentObj.toString();

							} else if (message.getContentType().contains("text/html")) {

								htmlContent = contentObj.toString();
							}

							pm.put(traits.key("content"), content);
							pm.put(traits.key("htmlContent"), htmlContent);
							pm.put(traits.key("attachedFiles"), attachments);

							app.create(entityType, pm);
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
