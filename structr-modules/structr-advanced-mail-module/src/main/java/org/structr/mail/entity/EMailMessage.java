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
package org.structr.mail.entity;

import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.MailTemplate;
import org.structr.core.property.*;
import org.structr.mail.entity.relationship.EMailMessageHAS_ATTACHMENTFile;
import org.structr.mail.entity.relationship.MailboxCONTAINS_EMAILMESSAGESEMailMessage;
import org.structr.schema.SchemaService;
import org.structr.web.entity.File;

import java.util.Date;

public class EMailMessage extends AbstractNode {

	public static final Property<Iterable<File>> attachedFilesProperty = new EndNodes<>("attachedFiles", EMailMessageHAS_ATTACHMENTFile.class);
	public static final Property<Mailbox> mailboxProperty              = new StartNode<>("mailbox", MailboxCONTAINS_EMAILMESSAGESEMailMessage.class);

	public static final Property<String> subjectProperty     = new StringProperty("subject").indexed();
	public static final Property<String> fromProperty        = new StringProperty("from").indexed();
	public static final Property<String> fromMailProperty    = new StringProperty("fromMail").indexed();
	public static final Property<String> toProperty          = new StringProperty("to").indexed();
	public static final Property<String> ccProperty          = new StringProperty("cc").indexed();
	public static final Property<String> bccProperty         = new StringProperty("bcc").indexed();
	public static final Property<String> replyToProperty     = new StringProperty("replyTo").indexed();
	public static final Property<String> contentProperty     = new StringProperty("content");
	public static final Property<String> htmlContentProperty = new StringProperty("htmlContent");
	public static final Property<String> folderProperty      = new StringProperty("folder").indexed();
	public static final Property<String> headerProperty      = new StringProperty("header");
	public static final Property<String> messageIdProperty   = new StringProperty("messageId").indexed();
	public static final Property<String> inReplyToProperty   = new StringProperty("inReplyTo").indexed();

	public static final Property<Date> receivedDateProperty = new DateProperty("receivedDate").indexed();
	public static final Property<Date> sentDateProperty     = new DateProperty("sentDate").indexed();

	public static final View defaultView = new View(EMailMessage.class, PropertyView.Public,
		id, type, subjectProperty, fromProperty, fromMailProperty, toProperty, ccProperty, bccProperty, replyToProperty,
		contentProperty, htmlContentProperty, folderProperty, headerProperty, messageIdProperty, inReplyToProperty,
		receivedDateProperty, sentDateProperty
	);

	public static final View uiView      = new View(EMailMessage.class, PropertyView.Ui,
		subjectProperty, fromProperty, fromMailProperty, toProperty, ccProperty, bccProperty, replyToProperty,
		contentProperty, htmlContentProperty, folderProperty, headerProperty, messageIdProperty, inReplyToProperty,
		receivedDateProperty, sentDateProperty, attachedFilesProperty
	);

	static {

		final JsonSchema schema = SchemaService.getDynamicSchema();
		final JsonType type     = schema.addType("EMailMessage");

		type.setExtends(EMailMessage.class);
	}
}
