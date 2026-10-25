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
package org.structr.mail;

import org.structr.api.config.IntegerSetting;
import org.structr.api.config.Setting;
import org.structr.api.config.Settings;
import org.structr.api.config.StringSetting;

/**
 * Settings of the MailService.
 *
 * They live here and not as static fields of MailService because ServiceLoader only loads a service
 * class without initializing it, so the settings would not show up in the configuration (or the
 * documentation) unless the MailService is actually started.
 */
public final class MailSettings {

	private static final String CATEGORY = "MailService";

	public static final Setting<Integer> MaxEmails            = new IntegerSetting(Settings.smtpGroup, CATEGORY, "mail.maxemails", 25, "Maximum number of emails to fetch per mailbox per fetch. With IMAP, a larger backlog is fetched over several runs.");
	public static final Setting<String> AttachmentBasePath    = new StringSetting(Settings.smtpGroup, CATEGORY, "mail.attachmentbasepath", "/mail/attachments", "Path in Structr's virtual filesystem where the attachments of fetched e-mails are stored, in subfolders by received date and mailbox.");
	public static final Setting<Integer> ConnectTimeout       = new IntegerSetting(Settings.smtpGroup, CATEGORY, "mail.connecttimeout", 30_000, "How long to wait for a mail server to accept a connection, in milliseconds. Without a limit, a server that stops answering holds the fetch of its mailbox forever.");
	public static final Setting<Integer> ReadTimeout          = new IntegerSetting(Settings.smtpGroup, CATEGORY, "mail.readtimeout", 60_000, "How long to wait for a mail server to answer once connected, in milliseconds, for the TLS handshake as well as for every command.");
	public static final Setting<Integer> MaxConcurrentFetches = new IntegerSetting(Settings.smtpGroup, CATEGORY, "mail.maxconcurrentfetches", 4, "How many mailboxes are fetched at the same time. Mailboxes on one server all connect from this host, and a mail server that sees too many connections at once may stop answering. Requires a restart of the service.");

	private MailSettings() {
	}

	/**
	 * Triggers class initialization: each Setting registers itself in its constructor, so the settings
	 * only appear in the configuration once this class has been loaded.
	 */
	static void register() {
	}
}
