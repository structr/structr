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
package org.structr.mail.entity.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;

import java.util.Date;
import java.util.Map;
import java.util.Set;

public class EMailMessageTraitDefinition extends AbstractNodeTraitDefinition {

	public EMailMessageTraitDefinition() {
		super("EMailMessage");
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Iterable<NodeInterface>> attachedFilesProperty = new EndNodes("attachedFiles", "EMailMessageHAS_ATTACHMENTFile");
		final Property<NodeInterface> mailboxProperty = new StartNode("mailbox", "MailboxCONTAINS_EMAILMESSAGESEMailMessage");

		final Property<String> subjectProperty = new StringProperty("subject").indexed();
		final Property<String> fromProperty = new StringProperty("from").indexed();
		final Property<String> fromMailProperty = new StringProperty("fromMail").indexed();
		final Property<String> toProperty = new StringProperty("to").indexed();
		final Property<String> ccProperty = new StringProperty("cc").indexed();
		final Property<String> bccProperty = new StringProperty("bcc").indexed();
		final Property<String> replyToProperty = new StringProperty("replyTo").indexed();
		final Property<String> contentProperty = new StringProperty("content");
		final Property<String> htmlContentProperty = new StringProperty("htmlContent");
		final Property<String> folderProperty = new StringProperty("folder").indexed();
		final Property<String> headerProperty = new StringProperty("header");
		final Property<String> messageIdProperty = new StringProperty("messageId").indexed();
		final Property<String> inReplyToProperty = new StringProperty("inReplyTo").indexed();

		final Property<Date> receivedDateProperty = new DateProperty("receivedDate").indexed();
		final Property<Date> sentDateProperty = new DateProperty("sentDate").indexed();

		return newSet(
			attachedFilesProperty,
			mailboxProperty,
			subjectProperty,
			fromProperty,
			fromMailProperty,
			toProperty,
			ccProperty,
			bccProperty,
			replyToProperty,
			contentProperty,
			htmlContentProperty,
			folderProperty,
			headerProperty,
			messageIdProperty,
			inReplyToProperty,
			receivedDateProperty,
			sentDateProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(

			PropertyView.Public,
			newSet(
				"id", "type", "subject", "from", "fromMail", "to", "cc", "bcc", "replyTo",
				"content", "htmlContent", "folder", "header", "messageId", "inReplyTo",
				"receivedDate", "sentDate"
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
