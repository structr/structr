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
package org.structr.mail.entity.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;

import java.util.Date;
import java.util.Map;
import java.util.Set;

public class EMailMessageTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String ATTACHED_FILES_PROPERTY = "attachedFiles";
	public static final String MAILBOX_PROPERTY        = "mailbox";
	public static final String SUBJECT_PROPERTY        = "subject";
	public static final String FROM_PROPERTY           = "from";
	public static final String FROM_MAIL_PROPERTY      = "fromMail";
	public static final String TO_PROPERTY             = "to";
	public static final String CC_PROPERTY             = "cc";
	public static final String BCC_PROPERTY            = "bcc";
	public static final String REPLY_TO_PROPERTY       = "replyTo";
	public static final String CONTENT_PROPERTY        = "content";
	public static final String HTML_CONTENT_PROPERTY   = "htmlContent";
	public static final String FOLDER_PROPERTY         = "folder";
	public static final String HEADER_PROPERTY         = "header";
	public static final String MESSAGE_ID_PROPERTY     = "messageId";
	public static final String IN_REPLY_TO_PROPERTY    = "inReplyTo";
	public static final String RECEIVED_DATE_PROPERTY  = "receivedDate";
	public static final String SENT_DATE_PROPERTY      = "sentDate";

	public EMailMessageTraitDefinition() {
		super(StructrTraits.EMAIL_MESSAGE);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<Iterable<NodeInterface>> attachedFilesProperty = new EndNodes(traitsInstance, ATTACHED_FILES_PROPERTY, StructrTraits.EMAIL_MESSAGE_HAS_ATTACHMENT_FILE).description("").description("files that were attached to this message");
		final Property<NodeInterface> mailboxProperty                 = new StartNode(traitsInstance, MAILBOX_PROPERTY, StructrTraits.MAILBOX_CONTAINS_EMAIL_MESSAGES_EMAIL_MESSAGE).description("").description("mailbox this message belongs to");

		final Property<String> subjectProperty                        = new StringProperty(SUBJECT_PROPERTY).indexed().description("subject of this message");
		final Property<String> fromProperty                           = new StringProperty(FROM_PROPERTY).indexed().description("sender name of this message");
		final Property<String> fromMailProperty                       = new StringProperty(FROM_MAIL_PROPERTY).indexed().description("sender address of this message");
		final Property<String> toProperty                             = new StringProperty(TO_PROPERTY).indexed().description("recipient of this message");
		final Property<String> ccProperty                             = new StringProperty(CC_PROPERTY).indexed().description("cc address of this message");
		final Property<String> bccProperty                            = new StringProperty(BCC_PROPERTY).indexed().description("bcc address of this message");
		final Property<String> replyToProperty                        = new StringProperty(REPLY_TO_PROPERTY).indexed().description("reply address of this message");
		final Property<String> contentProperty                        = new StringProperty(CONTENT_PROPERTY).description("plaintext content of this message");
		final Property<String> htmlContentProperty                    = new StringProperty(HTML_CONTENT_PROPERTY).description("html content of this message");
		final Property<String> folderProperty                         = new StringProperty(FOLDER_PROPERTY).indexed();
		final Property<String> headerProperty                         = new StringProperty(HEADER_PROPERTY);
		final Property<String> messageIdProperty                      = new StringProperty(MESSAGE_ID_PROPERTY).indexed().description("message id of this message");
		final Property<String> inReplyToProperty                      = new StringProperty(IN_REPLY_TO_PROPERTY).indexed().description("inReplyTo of this message");

		final Property<Date> receivedDateProperty                     = new DateProperty(RECEIVED_DATE_PROPERTY).indexed().description("date this message was received");
		final Property<Date> sentDateProperty                         = new DateProperty(SENT_DATE_PROPERTY).indexed().description("date this message was sent");

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
					SUBJECT_PROPERTY, FROM_PROPERTY, FROM_MAIL_PROPERTY, TO_PROPERTY, CC_PROPERTY, BCC_PROPERTY, REPLY_TO_PROPERTY,
					CONTENT_PROPERTY, HTML_CONTENT_PROPERTY, FOLDER_PROPERTY, HEADER_PROPERTY, MESSAGE_ID_PROPERTY, IN_REPLY_TO_PROPERTY,
					RECEIVED_DATE_PROPERTY, SENT_DATE_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	@Override
	public String getShortDescription() {
		return "This type is part of the MailService, which you can use to import emails from different mailboxes into Structr.";
	}

	@Override
	public String getLongDescription() {

		return """
		### How It Works
		An `EMailMessage` represents an email in the database and can be used to trigger actions in Structr when new emails arrive in a Mailbox. When the MailService is enabled, it queries all Mailbox instances in the database at regular intervals and stores new emails as EMailMessages in the database.

		### Common Use Cases
		A common use case is to implement an `onCreate()` lifecycle method on the type `EMailMessage` to react to incoming emails, associate them with support tickets etc.
		""";
	}
}
