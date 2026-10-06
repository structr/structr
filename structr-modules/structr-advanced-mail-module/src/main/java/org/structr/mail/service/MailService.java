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
package org.structr.mail.service;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.sun.mail.util.BASE64DecoderStream;
import com.sun.mail.util.MailConnectException;
import org.apache.commons.lang3.ArrayUtils;
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
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.mail.entity.Mailbox;
import org.structr.mail.entity.traits.definitions.EMailMessageTraitDefinition;
import org.structr.schema.SchemaService;
import org.structr.web.common.FileHelper;

import javax.activation.DataSource;
import javax.mail.*;
import javax.mail.internet.MimeUtility;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.structr.mail.entity.traits.definitions.MailboxTraitDefinition;
import org.structr.core.property.PropertyKey;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import javax.mail.UIDFolder;
import javax.mail.StoreClosedException;
import javax.mail.FolderClosedException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ServiceDependency(SchemaService.class)
@StopServiceForMaintenanceMode
public class MailService implements RunnableService, MailServiceInterface {

	private static final Logger logger                      = LoggerFactory.getLogger(MailService.class.getName());
	private final ThreadPoolExecutor threadExecutor;
	private boolean run                                     = false;
	private Set<Class> supportedCommands                    = null;
	private int maxConnectionRetries                        = 5;

	// uuid of each mailbox being fetched, with the time its fetch was requested
	private final Map<String, Long> processingMailboxes     = new ConcurrentHashMap<>();

	// low-hanging status figures since the service started, for the report in every fetch log line
	private final AtomicLong requested                      = new AtomicLong();
	private final AtomicLong alreadyRunning                 = new AtomicLong();
	private final AtomicLong succeeded                      = new AtomicLong();
	private final AtomicLong connectFailures                = new AtomicLong();
	private final AtomicLong otherFailures                  = new AtomicLong();
	private final AtomicLong messagesCreated                = new AtomicLong();
	private final AtomicLong messagesSkipped                = new AtomicLong();
	private volatile String lastError                       = null;

	// Hint: when this class moves from the mail module to structr-base, the documentation can be moved here

	public static final Setting<Integer> maxEmails          = new IntegerSetting(Settings.smtpGroup, "MailService", "mail.maxemails",          25,                  "The number of mails which are checked");
	public static final Setting<String> attachmentBasePath  = new StringSetting (Settings.smtpGroup, "MailService", "mail.attachmentbasepath", "/mail/attachments", "The ");
	public static final Setting<Integer> connectTimeout     = new IntegerSetting(Settings.smtpGroup, "MailService", "mail.connecttimeout",     30000,               "How long to wait for a mail server to accept a connection, in milliseconds. Without a limit, a server that stops answering holds the fetch of its mailbox forever.");
	public static final Setting<Integer> readTimeout        = new IntegerSetting(Settings.smtpGroup, "MailService", "mail.readtimeout",        60000,               "How long to wait for a mail server to answer once connected, in milliseconds, for the TLS handshake as well as for every command.");
	public static final Setting<Integer> maxConcurrentFetches = new IntegerSetting(Settings.smtpGroup, "MailService", "mail.maxconcurrentfetches", 4,                 "How many mailboxes are fetched at the same time. Mailboxes on one server all connect from this host, and a mail server that sees too many connections at once may stop answering. Requires a restart of the service.");

	public MailService() {

		supportedCommands = new LinkedHashSet<>();
		supportedCommands.add(FetchMailsCommand.class);
		supportedCommands.add(FetchFoldersCommand.class);

		final AtomicInteger threadNumber = new AtomicInteger();
		final int poolSize               = Math.max(1, maxConcurrentFetches.getValue(4));

		// bounded, because every mailbox of a server connects from this host, and the server answers too many at once with silence
		threadExecutor = new ThreadPoolExecutor(poolSize, poolSize, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), runnable -> {

			final Thread thread = new Thread(runnable, "MailFetch-" + threadNumber.incrementAndGet());
			thread.setDaemon(true);
			return thread;
		});
	}

	/**
	 * Fetches the given mailbox, asynchronously. This is the only way a mailbox is fetched: there is no
	 * automatic fetching, a schedule is a cron job that calls fetchMails().
	 */
	public void fetchMails(final Mailbox mb) {

		requested.incrementAndGet();

		final Long since = processingMailboxes.putIfAbsent(mb.getUuid(), System.currentTimeMillis());
		if (since != null) {

			alreadyRunning.incrementAndGet();

			logger.info("Fetch of mailbox [{}] not started, the previous one is still running after {} s | {}", mb.getUuid(), (System.currentTimeMillis() - since) / 1000, statusReport());

			return;
		}

		logger.info("Fetch of mailbox [{}] on {} requested | {}", mb.getUuid(), mb.getHost(), statusReport());

		threadExecutor.submit(new MailFetchTask(mb));
	}

	/**
	 * Whether a fetch of the given mailbox has been requested and not yet finished.
	 */
	public boolean isFetching(final String mailboxUuid) {

		return processingMailboxes.containsKey(mailboxUuid);
	}

	/**
	 * A one-line summary of the service: what is running and waiting now, and what happened since it started.
	 */
	public String statusReport() {

		final long now  = System.currentTimeMillis();
		final long oldest = processingMailboxes.values().stream().mapToLong(Long::longValue).min().orElse(now);

		return String.format("running %d of %d, queued %d, oldest %d s; since start: %d requested, %d not started (already running), %d succeeded, %d could not connect, %d failed, %d messages created, %d skipped%s",
			threadExecutor.getActiveCount(),
			threadExecutor.getMaximumPoolSize(),
			threadExecutor.getQueue().size(),
			(now - oldest) / 1000,
			requested.get(),
			alreadyRunning.get(),
			succeeded.get(),
			connectFailures.get(),
			otherFailures.get(),
			messagesCreated.get(),
			messagesSkipped.get(),
			lastError != null ? "; last error: " + lastError : ""
		);
	}

	public Iterable<String> fetchFolders(final Mailbox mb) {

		if (mb.getHost() != null && mb.getMailProtocol() != null && mb.getUser() != null && mb.getPassword() != null && mb.getFolders() != null) {

			final Store store = connectToStore(mb);
			List<String> folders = new ArrayList<>();

			if (store != null && store.isConnected()) {

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

				} finally {

					close(store);
				}

			}

			return folders;

		} else {

			logger.warn("Could not retrieve folders for mailbox[" + mb.getUuid() + "] since not all required attributes were specified.");

			return new ArrayList<>();
		}
	}

	@Override
	public void startService() throws Exception {

		this.run = true;

		logger.info("MailService started: mailboxes are fetched only when fetchMails() is called, a schedule is a cron job that calls it");
	}

	@Override
	public void stopService() {

		this.run = false;

		threadExecutor.shutdownNow();
	}

	@Override
	public String getName() {

		// what Thread.getName() answered while the service was still a thread
		return "MailService";
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

			final Traits traits = Traits.of(StructrTraits.EMAIL_MESSAGE);
			PropertyMap props = new PropertyMap();

			props.put(traits.key(EMailMessageTraitDefinition.FROM_PROPERTY),           amc.getDisplayName(amc.getFromName(), amc.getFromAddress()));
			props.put(traits.key(EMailMessageTraitDefinition.FROM_MAIL_PROPERTY),       amc.getFromAddress());
			props.put(traits.key(EMailMessageTraitDefinition.TO_PROPERTY),             amc.getCombinedDisplayNames(amc.getTo()));
			props.put(traits.key(EMailMessageTraitDefinition.SUBJECT_PROPERTY),        amc.getSubject());
			props.put(traits.key(EMailMessageTraitDefinition.CONTENT_PROPERTY),        amc.getTextContent());
			props.put(traits.key(EMailMessageTraitDefinition.HTML_CONTENT_PROPERTY),    amc.getHtmlContent());
			props.put(traits.key(EMailMessageTraitDefinition.SENT_DATE_PROPERTY),       new Date());

			props.put(traits.key(EMailMessageTraitDefinition.MESSAGE_ID_PROPERTY),      messageId);
			props.put(traits.key(EMailMessageTraitDefinition.IN_REPLY_TO_PROPERTY),      amc.getInReplyTo());

			props.put(traits.key(EMailMessageTraitDefinition.HEADER_PROPERTY),         new Gson().toJson(amc.getCustomHeaders()));

			props.put(traits.key(EMailMessageTraitDefinition.REPLY_TO_PROPERTY),        amc.getCombinedDisplayNames(amc.getReplyTo()));
			props.put(traits.key(EMailMessageTraitDefinition.BCC_PROPERTY),            amc.getCombinedDisplayNames(amc.getBcc()));

			if (amc.getAttachments().size() > 0) {

				final ArrayList concreteAttachedFiles = new ArrayList();

				for (final DynamicMailAttachment attachment : amc.getAttachments()) {

					final NodeInterface savedFile = handleOutgoingMailAttachment(securityContext, attachment);
					if (savedFile != null) {

						concreteAttachedFiles.add(savedFile);
					}
				}

				props.put(traits.key(EMailMessageTraitDefinition.ATTACHED_FILES_PROPERTY), concreteAttachedFiles);
			}

			// not setting folder/receivedDate
//			props.put(StructrApp.key(entityType, "folder"), null);
//			props.put(StructrApp.key(entityType, "receivedDate"), null);

			outgoingMessage = app.create(StructrTraits.EMAIL_MESSAGE, props);

			tx.success();

		} catch (Throwable t) {

			logger.warn("Error creating outgoing mail!", t);
		}

		return outgoingMessage;
	}

	private NodeInterface handleOutgoingMailAttachment(final SecurityContext securityContext, final DynamicMailAttachment dma) {

		NodeInterface file = null;
		final DataSource ds   = dma.getDataSource();
		final String fileType = ds.getContentType().toLowerCase().startsWith("image/") ? StructrTraits.IMAGE : StructrTraits.FILE;
		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final String path = getStoragePath("/outgoing", new Date());
			NodeInterface fileFolder = FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), path);

			file = FileHelper.createFile(SecurityContext.getSuperUserInstance(), ds.getInputStream(), ds.getContentType(), fileType, dma.getName(), fileFolder.as(org.structr.web.entity.Folder.class));

			final Principal owner = securityContext.getUser(false);
			if (owner != null) {

				file.as(AccessControllable.class).setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.OWNER_PROPERTY), securityContext.getUser(false));
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

			final String fileType = p.getContentType().toLowerCase().startsWith("image/") ? StructrTraits.IMAGE : StructrTraits.FILE;
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

			for (int i = 0, len = p.getCount(); i < len; i++) {

				final String htmlContent = result.get(EMailMessageTraitDefinition.HTML_CONTENT_PROPERTY) != null ? result.get(EMailMessageTraitDefinition.HTML_CONTENT_PROPERTY) : "";
				final String content     = result.get(EMailMessageTraitDefinition.CONTENT_PROPERTY) != null ? result.get(EMailMessageTraitDefinition.CONTENT_PROPERTY) : "";
				BodyPart part = (BodyPart) p.getBodyPart(i);

				if (part.getContent() instanceof Multipart) {

					final Map<String,String> subResult = handleMultipart(mb, message, (Multipart)part.getContent(), attachments);
					if (subResult.get(EMailMessageTraitDefinition.CONTENT_PROPERTY) != null) {

						result.put(EMailMessageTraitDefinition.CONTENT_PROPERTY, content.concat(subResult.get(EMailMessageTraitDefinition.CONTENT_PROPERTY)));
					}

					if (subResult.get(EMailMessageTraitDefinition.HTML_CONTENT_PROPERTY) != null) {

						result.put(EMailMessageTraitDefinition.HTML_CONTENT_PROPERTY, htmlContent.concat(subResult.get(EMailMessageTraitDefinition.HTML_CONTENT_PROPERTY)));
					}

				} else if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) || (part.getContentType().toLowerCase().contains("image/") && Part.INLINE.equalsIgnoreCase(part.getDisposition())) || part.getContentType().toLowerCase().contains("application/pdf")) {

					final NodeInterface file = extractFileAttachment(mb, message, part);
					if (file != null) {

						attachments.add(file);
					}

				} else {

					if (part.isMimeType("text/html")) {

						result.put(EMailMessageTraitDefinition.HTML_CONTENT_PROPERTY, htmlContent.concat(getText(part)));

					} else if (part.isMimeType("text/plain")) {

						result.put(EMailMessageTraitDefinition.CONTENT_PROPERTY, content.concat(getText(part)));

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

			} else if("base64".equals(p.getContentType())) {

				BASE64DecoderStream contentStream = (BASE64DecoderStream)content;

				return contentStream.toString();

			} else {

				return null;
			}
		}

		return null;
	}

	private Store connectToStore(final Mailbox mailbox) {

		return connectToStore(MailboxConfig.of(mailbox));
	}

	private Store connectToStore(final MailboxConfig mailbox) {

		final String host = mailbox.host();
		final String mailProtocol = mailbox.protocol();
		final String user = mailbox.user();
		final String password = mailbox.password();
		final Integer port = mailbox.port();
		final String[] folders = mailbox.folders();

		try {

			if (host == null || mailProtocol == null || user == null || password == null || folders == null) {

				logger.warn("MailService::fetchMails: Could not retrieve mails from mailbox[" + mailbox.uuid() + "], because not all required attributes were specified.");

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

			final String readTimeoutMillis = String.valueOf(readTimeout.getValue(60000));

			properties.put("mail." + mailProtocol + ".connectiontimeout", String.valueOf(connectTimeout.getValue(30000)));
			properties.put("mail." + mailProtocol + ".timeout",           readTimeoutMillis);
			properties.put("mail." + mailProtocol + ".writetimeout",      readTimeoutMillis);

			// getInstance, not getDefaultInstance: that one is a singleton that keeps the properties of whichever mailbox came first
			final Session emailSession = Session.getInstance(properties);
			final Store store          = emailSession.getStore(mailProtocol);
			int retries = 0;

			while (retries < maxConnectionRetries && !store.isConnected()) {

				try {

					store.connect(host, user, password);

				} catch (AuthenticationFailedException ex) {

					logger.warn("Could not authenticate mailbox[" + mailbox.uuid() + "]: " + ex.getMessage());
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

			logger.warn("Authentication failed for Mailbox[" + mailbox.uuid() + "].");

		} catch (MailConnectException ex) {

			logger.error("Could not connect to mailbox [" + mailbox.uuid() + "]: " + ex.getMessage());

		} catch (MessagingException ex) {

			// one line per mailbox: when a server stops answering, every mailbox on it fails the same way on every round
			lastError = "mailbox [" + mailbox.uuid() + "]: " + rootMessage(ex);

			logger.warn("Could not connect to mailbox [{}] on {}: {}", mailbox.uuid(), mailbox.host(), rootMessage(ex));
			logger.debug("Connection failure details for mailbox [" + mailbox.uuid() + "]", ex);

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

	private static void close(final Store store) {

		if (store != null && store.isConnected()) {

			try { store.close(); } catch (MessagingException ex) { logger.debug("Unable to close mail store", ex); }
		}
	}

	private static void close(final Folder folder) {

		if (folder != null && folder.isOpen()) {

			try { folder.close(false); } catch (MessagingException ex) { logger.debug("Unable to close mail folder", ex); }
		}
	}

	private static String rootMessage(final Throwable t) {

		Throwable root = t;

		while (root.getCause() != null && root.getCause() != root) {

			root = root.getCause();
		}

		return root == t ? String.valueOf(t.getMessage()) : t.getMessage() + " (" + root + ")";
	}

	//////////////////////////////////////////////////////////////// Nested classes
	/**
	 * What a fetch needs to connect, read in a transaction and kept, so the connection itself runs outside one.
	 */
	private record MailboxConfig(String uuid, String host, String protocol, String user, String password, Integer port, String[] folders) {

		static MailboxConfig of(final Mailbox mailbox) {

			final Object protocol = mailbox.getMailProtocol();

			return new MailboxConfig(mailbox.getUuid(), mailbox.getHost(), protocol != null ? protocol.toString() : null, mailbox.getUser(), mailbox.getPassword(), mailbox.getPort(), mailbox.getFolders());
		}
	}

	/**
	 * Per IMAP folder, its UIDVALIDITY and the highest UID fetched so far, stored as JSON on the mailbox.
	 */
	static final class FetchState {

		static final class Folder {

			long uidValidity;
			long lastUid;
		}

		private final Map<String, Folder> folders = new LinkedHashMap<>();

		static FetchState parse(final String json) {

			final FetchState state = new FetchState();

			if (StringUtils.isNotBlank(json)) {

				try {

					// the tree API, not reflection: on the module path Gson may not read the fields of this package
					for (final Map.Entry<String, JsonElement> entry : JsonParser.parseString(json).getAsJsonObject().entrySet()) {

						final JsonObject stored = entry.getValue().getAsJsonObject();
						final Folder folder     = new Folder();

						folder.uidValidity = stored.get("uidValidity").getAsLong();
						folder.lastUid     = stored.get("lastUid").getAsLong();

						state.folders.put(entry.getKey(), folder);
					}

				} catch (Exception ex) {

					logger.warn("Ignoring unreadable fetch state, the next fetch starts over from the newest messages: {}", ex.getMessage());
				}
			}

			return state;
		}

		/** the highest UID already fetched, or -1 if the folder is new or its UIDs were reassigned */
		long lastUid(final String folder, final long uidValidity) {

			final Folder f = folders.get(folder);

			return f != null && f.uidValidity == uidValidity ? f.lastUid : -1;
		}

		void advance(final String folder, final long uidValidity, final long uid) {

			final Folder f = folders.computeIfAbsent(folder, k -> new Folder());

			if (f.uidValidity != uidValidity) {

				f.uidValidity = uidValidity;
				f.lastUid     = 0;
			}

			f.lastUid = Math.max(f.lastUid, uid);
		}

		String toJson() {

			final JsonObject json = new JsonObject();

			for (final Map.Entry<String, Folder> entry : folders.entrySet()) {

				final JsonObject folder = new JsonObject();

				folder.addProperty("uidValidity", entry.getValue().uidValidity);
				folder.addProperty("lastUid",     entry.getValue().lastUid);

				json.add(entry.getKey(), folder);
			}

			return json.toString();
		}
	}

	private class MailFetchTask implements Runnable {

		// the uuid, not the node: this runs on another thread, in transactions of its own
		private final String mailboxUuid;
		private final String host;
		private int created = 0;
		private int skipped = 0;
		private String error = null;

		public MailFetchTask(final Mailbox mailbox) {

			this.mailboxUuid = mailbox.getUuid();
			this.host        = mailbox.getHost();
		}

		@Override
		public void run() {

			final long start  = System.currentTimeMillis();
			Store store       = null;
			boolean connected = false;
			boolean completed = false;

			try {

				final MailboxConfig config;
				FetchState state;

				// short transactions only: the network I/O runs outside them, and every message commits on its own
				try (final Tx tx = StructrApp.getInstance().tx()) {

					final Mailbox mailbox = loadMailbox();

					config = MailboxConfig.of(mailbox);
					state  = FetchState.parse(mailbox.getProperty(Traits.of(StructrTraits.MAILBOX).key(MailboxTraitDefinition.FETCH_STATE_PROPERTY)));

					writeStatus(mailbox, Map.of(MailboxTraitDefinition.LAST_FETCH_STARTED_PROPERTY, new Date()));

					tx.success();
				}

				logger.debug("Fetching mailbox [{}] on {}, folders {}", mailboxUuid, host, Arrays.toString(config.folders()));

				store = connectToStore(config);
				if (store != null && store.isConnected()) {

					connected = true;

					for (final String folder : config.folders() != null ? config.folders() : new String[0]) {

						fetchFolder(store.getFolder(folder), state);
					}
				}

				completed = true;

			} catch (Throwable ex) {

				error = String.valueOf(ex);
				logger.error("Error while updating Mails: ", ex);

			} finally {

				// in finally, so that no failure, however it ends, leaves the mailbox marked as being fetched or the connection open
				close(store);

				final String outcome;

				if (!completed) {

					outcome = "failed";
					otherFailures.incrementAndGet();

				} else if (!connected) {

					outcome = "could not connect";
					connectFailures.incrementAndGet();

				} else {

					outcome = skipped > 0 ? "succeeded, " + skipped + " message(s) skipped" : "succeeded";
					succeeded.incrementAndGet();
				}

				messagesCreated.addAndGet(created);

				if (error != null) {

					lastError = "mailbox [" + mailboxUuid + "]: " + error;
				}

				recordOutcome(completed && connected, outcome);

				logger.info("Fetch of mailbox [{}] {} after {} ms, {} new message(s) | {}", mailboxUuid, outcome, System.currentTimeMillis() - start, created, statusReport());

				// last, so that whoever waits for the fetch to end finds its outcome already recorded on the mailbox
				processingMailboxes.remove(mailboxUuid);
			}
		}

		private Mailbox loadMailbox() throws FrameworkException {

			final NodeInterface node = StructrApp.getInstance().getNodeById(StructrTraits.MAILBOX, mailboxUuid);
			if (node == null) {

				throw new FrameworkException(404, "Mailbox " + mailboxUuid + " no longer exists");
			}

			return node.as(Mailbox.class);
		}

		private void writeStatus(final Mailbox mailbox, final Map<String, Object> values) throws FrameworkException {

			final Traits traits    = Traits.of(StructrTraits.MAILBOX);
			final PropertyMap map  = new PropertyMap();

			for (final Map.Entry<String, Object> entry : values.entrySet()) {

				map.put(traits.key(entry.getKey()), entry.getValue());
			}

			mailbox.unlockReadOnlyPropertiesOnce();
			mailbox.setProperties(SecurityContext.getSuperUserInstance(), map);
		}

		private void recordOutcome(final boolean success, final String outcome) {

			try (final Tx tx = StructrApp.getInstance().tx()) {

				final Map<String, Object> values = new LinkedHashMap<>();

				values.put(MailboxTraitDefinition.LAST_FETCH_COUNT_PROPERTY, created);
				values.put(MailboxTraitDefinition.LAST_FETCH_ERROR_PROPERTY, success && error == null ? null : (error != null ? error : outcome));

				if (success) {

					values.put(MailboxTraitDefinition.LAST_FETCH_SUCCEEDED_PROPERTY, new Date());
				}

				writeStatus(loadMailbox(), values);

				tx.success();

			} catch (Throwable t) {

				logger.warn("Unable to record the fetch outcome on mailbox [{}]: {}", mailboxUuid, t.toString());
			}
		}

		private void fetchFolder(final Folder folder, final FetchState state) {

			if (folder == null) {

				return;
			}

			try {

				folder.open(Folder.READ_ONLY);

				final int batchSize   = Math.max(1, maxEmails.getValue(25));
				final Gson gson       = new Gson();
				final String name     = folder.getFullName();
				final UIDFolder uids  = folder instanceof UIDFolder u ? u : null;
				final long validity   = uids != null ? uids.getUIDValidity() : -1;
				final List<Message> batch;

				if (uids != null && state.lastUid(name, validity) >= 0) {

					// everything newer than what was fetched before, oldest first: a burst larger than the batch continues next time
					final long lastUid = state.lastUid(name, validity);
					batch = new ArrayList<>();

					for (final Message message : uids.getMessagesByUID(lastUid + 1, UIDFolder.LASTUID)) {

						// n:* always returns the highest message, even when it is not above n
						if (message != null && uids.getUID(message) > lastUid && batch.size() < batchSize) {

							batch.add(message);
						}
					}

				} else {

					// first fetch, POP3, or reassigned UIDs: the newest messages, as before, so a mailbox full of history is not imported
					final Message[] all = folder.getMessages();
					batch = new ArrayList<>(Arrays.asList(all).subList(Math.max(0, all.length - batchSize), all.length));
				}

				for (final Message message : batch) {

					final long uid = uids != null ? uids.getUID(message) : -1;

					try (final Tx tx = StructrApp.getInstance().tx()) {

						final Mailbox mailbox = loadMailbox();

						if (importMessage(StructrApp.getInstance(), gson, mailbox, message)) {

							created++;
						}

						// committed together with the message, so a crash between the two cannot lose or duplicate it
						if (uids != null) {

							state.advance(name, validity, uid);
							writeStatus(mailbox, Map.of(MailboxTraitDefinition.FETCH_STATE_PROPERTY, state.toJson()));
						}

						tx.success();

					} catch (Exception | LinkageError ex) {

						final String what = "message " + (uid >= 0 ? "UID " + uid : "#" + message.getMessageNumber()) + " in " + name;

						// a lost connection fails every message after it: stop without moving the mark, the next fetch resumes here
						if (!folder.isOpen() || ex instanceof FolderClosedException || ex instanceof StoreClosedException) {

							error = "connection lost at " + what + ": " + rootMessage(ex);
							logger.warn("Connection to mailbox [{}] lost at {}, the next fetch resumes there: {}", mailboxUuid, what, rootMessage(ex));

							break;
						}

						skipped++;
						messagesSkipped.incrementAndGet();

						error = what + ": " + ex;

						logger.warn("Skipping {} of mailbox [{}]: {}", what, mailboxUuid, ex.toString());
						logger.debug("Details for the skipped message", ex);

						// moved past in a transaction of its own, so that one unreadable message cannot block the folder
						if (uids != null) {

							advancePast(name, validity, uid, state);
						}
					}
				}

			} catch (MessagingException ex) {

				error = "folder " + folder.getFullName() + ": " + rootMessage(ex);
				logger.warn("Unable to read folder {} of mailbox [{}]: {}", folder.getFullName(), mailboxUuid, rootMessage(ex));

			} finally {

				close(folder);
			}
		}

		private void advancePast(final String folder, final long validity, final long uid, final FetchState state) {

			try (final Tx tx = StructrApp.getInstance().tx()) {

				state.advance(folder, validity, uid);
				writeStatus(loadMailbox(), Map.of(MailboxTraitDefinition.FETCH_STATE_PROPERTY, state.toJson()));

				tx.success();

				logger.info("Moved past UID {} in {} of mailbox [{}], it is not fetched again", uid, folder, mailboxUuid);

			} catch (Throwable t) {

				logger.warn("Unable to store the fetch state of mailbox [{}], UID {} in {} is fetched again next time: {}", mailboxUuid, uid, folder, t.toString());
			}
		}

		/**
		 * The message this mailbox already holds for the given one, or null. Only this mailbox: the same mail in two
		 * mailboxes is two messages, and the base type, so a changed overrideMailEntityType does not make old mail new.
		 */
		private NodeInterface findExisting(final App app, final Mailbox mailbox, final Message message, final String messageId, final String from, final String to) throws FrameworkException, MessagingException {

			final Traits traits                         = Traits.of(StructrTraits.EMAIL_MESSAGE);
			final PropertyKey<NodeInterface> mailboxKey = traits.key(EMailMessageTraitDefinition.MAILBOX_PROPERTY);

			if (messageId != null) {

				final NodeInterface match = inMailbox(app.nodeQuery(StructrTraits.EMAIL_MESSAGE).key(traits.key(EMailMessageTraitDefinition.MESSAGE_ID_PROPERTY), messageId).getAsList(), mailboxKey, mailbox);
				if (match != null) {

					return match;
				}
			}

			return inMailbox(app.nodeQuery(StructrTraits.EMAIL_MESSAGE)
				.key(traits.key(EMailMessageTraitDefinition.SUBJECT_PROPERTY), message.getSubject())
				.key(traits.key(EMailMessageTraitDefinition.FROM_PROPERTY), from)
				.key(traits.key(EMailMessageTraitDefinition.TO_PROPERTY), to)
				.key(traits.key(EMailMessageTraitDefinition.RECEIVED_DATE_PROPERTY), message.getReceivedDate())
				.key(traits.key(EMailMessageTraitDefinition.SENT_DATE_PROPERTY), message.getSentDate()).getAsList(), mailboxKey, mailbox);
		}

		private NodeInterface inMailbox(final List<NodeInterface> candidates, final PropertyKey<NodeInterface> mailboxKey, final Mailbox mailbox) {

			for (final NodeInterface candidate : candidates) {

				final NodeInterface owner = candidate.getProperty(mailboxKey);
				if (owner != null && mailbox.getUuid().equals(owner.getUuid())) {

					return candidate;
				}
			}

			return null;
		}

		private boolean importMessage(final App app, final Gson gson, final Mailbox mailbox, final Message message) throws Exception {


			final PropertyMap pm  = new PropertyMap();
			final String from = message.getFrom() != null ? Arrays.stream(message.getFrom()).map((a) -> a != null ? decodeText(a.toString()) : "").reduce("", (a, b) -> a.isEmpty() ? b : a + "," + b) : "";
			final String to   = message.getRecipients(Message.RecipientType.TO) != null ? Arrays.stream(message.getRecipients(Message.RecipientType.TO)).map((a) -> a != null ? decodeText(a.toString()) : "").reduce("", (a, b) -> a.isEmpty() ? b : a + "," + b) : "";
			final String cc   = message.getRecipients(Message.RecipientType.CC) != null ? Arrays.stream(message.getRecipients(Message.RecipientType.CC)).map((a) -> a != null ? decodeText(a.toString()) : "").reduce("", (a, b) -> a.isEmpty() ? b : a + "," + b) : "";
			final String bcc  = message.getRecipients(Message.RecipientType.BCC) != null ? Arrays.stream(message.getRecipients(Message.RecipientType.BCC)).map((a) -> a != null ? decodeText(a.toString()) : "").reduce("", (a, b) -> a.isEmpty() ? b : a + "," + b) : "";

			// Allow mail instance class to be overriden by custom types to enable special mail handling
			String entityType   = StructrTraits.EMAIL_MESSAGE;
			String overrideType = mailbox.getOverrideMailEntityType();

			if (StringUtils.isNotBlank(overrideType)) {

				if (Traits.exists(overrideType) && Traits.of(overrideType).contains(StructrTraits.EMAIL_MESSAGE)) {

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
				if ("Message-ID".equals(header.getName()) || "Message-Id".equals(header.getName())) {

					messageId = header.getValue();

				} else if ("In-Reply-To".equals(header.getName()) || "References".equals(header.getName())) {

					inReplyTo = header.getValue();
				}

				headers.put(header.getName(), header.getValue());
			}

			final Traits traits                      = Traits.of(entityType);
			final NodeInterface existingEMailMessage = findExisting(app, mailbox, message, messageId, from, to);

			if (existingEMailMessage == null) {

				pm.put(traits.key(EMailMessageTraitDefinition.SUBJECT_PROPERTY), message.getSubject());
				pm.put(traits.key(EMailMessageTraitDefinition.FROM_PROPERTY), from);

				final Pattern pattern = Pattern.compile(".* <(.*)>");
				final Matcher matcher = pattern.matcher(from);

				if (matcher.matches()) {

					pm.put(traits.key(EMailMessageTraitDefinition.FROM_MAIL_PROPERTY), matcher.group(1));

				} else {

					pm.put(traits.key(EMailMessageTraitDefinition.FROM_MAIL_PROPERTY), from);
				}

				pm.put(traits.key(EMailMessageTraitDefinition.TO_PROPERTY), to);
				pm.put(traits.key(EMailMessageTraitDefinition.CC_PROPERTY), cc);
				pm.put(traits.key(EMailMessageTraitDefinition.BCC_PROPERTY), bcc);
				pm.put(traits.key(EMailMessageTraitDefinition.FOLDER_PROPERTY), message.getFolder().getFullName());
				pm.put(traits.key(EMailMessageTraitDefinition.RECEIVED_DATE_PROPERTY), message.getReceivedDate());
				pm.put(traits.key(EMailMessageTraitDefinition.SENT_DATE_PROPERTY), message.getSentDate());
				pm.put(traits.key(EMailMessageTraitDefinition.MAILBOX_PROPERTY), mailbox);
				pm.put(traits.key(EMailMessageTraitDefinition.HEADER_PROPERTY), gson.toJson(headers));

				if (messageId != null) {

					pm.put(traits.key(EMailMessageTraitDefinition.MESSAGE_ID_PROPERTY), messageId);
				}

				if (inReplyTo != null) {

					pm.put(traits.key(EMailMessageTraitDefinition.IN_REPLY_TO_PROPERTY), inReplyTo);
				}

				// Handle content extraction
				String content = null;
				String htmlContent = null;
				final Object contentObj = message.getContent();
				final List<NodeInterface> attachments = new ArrayList<>();

				if (message.getContentType().contains("multipart")) {

					final Map<String, String> result = handleMultipart(mailbox, message, (Multipart)contentObj, attachments);
					content = result.get(EMailMessageTraitDefinition.CONTENT_PROPERTY);
					htmlContent = result.get(EMailMessageTraitDefinition.HTML_CONTENT_PROPERTY);

				} else if (message.getContentType().contains("text/plain")){

					content = contentObj.toString();

				} else if (message.getContentType().contains("text/html")) {

					htmlContent = contentObj.toString();
				}

				pm.put(traits.key(EMailMessageTraitDefinition.CONTENT_PROPERTY), content);
				pm.put(traits.key(EMailMessageTraitDefinition.HTML_CONTENT_PROPERTY), htmlContent);
				pm.put(traits.key(EMailMessageTraitDefinition.ATTACHED_FILES_PROPERTY), attachments);

				app.create(entityType, pm);

				return true;
			}

			return false;
		}
	}
}
