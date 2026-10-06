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
package org.structr.docs.documentables.service;

import org.structr.docs.*;
import org.structr.docs.ontology.ConceptType;

import java.util.List;

public class MailServiceDocumentable extends AbstractServiceDocumentable {

	@Override
	public DocumentableType getDocumentableType() {

		return DocumentableType.Service;
	}

	@Override
	public String getName() {

		return "MailService";
	}

	@Override
	public String getShortDescription() {

		return "A service that fetches mailboxes on request and stores the emails in the database.";
	}

	@Override
	public String getLongDescription() {

		return """
		The MailService does not fetch anything by itself. A mailbox is fetched when `fetchMails()` is called on it, and the fetch runs in the background. To fetch mail on a schedule, register a method that calls `fetchMails()` with the CronService.

		Every fetch is logged at info level, together with a short status of the service: fetches running and queued, and counts of requested, succeeded and failed fetches since the service started.

		### Types
		`Mailbox`, `EMailMessage`
		""";
	}

	@Override
	public List<Setting> getSettings() {

		return List.of(
			new Setting("mail.maxemails",            "Maximum number of (new) emails that are fetched and created in one go"),
			new Setting("mail.attachmentbasepath",   "path in Structr's virtual filesystem where attachments are downloaded to"),
			new Setting("mail.connecttimeout",       "How long to wait for a mail server to accept a connection, in milliseconds"),
			new Setting("mail.readtimeout",          "How long to wait for a mail server to answer once connected, in milliseconds"),
			new Setting("mail.maxconcurrentfetches", "How many mailboxes are fetched at the same time")
		);
	}

	@Override
	public List<Link> getLinkedConcepts() {

		final List<Link> concepts = super.getLinkedConcepts();

		concepts.add(Link.to("provides", ConceptReference.of(ConceptType.Topic, "Sending emails")));

		return concepts;
	}
}
