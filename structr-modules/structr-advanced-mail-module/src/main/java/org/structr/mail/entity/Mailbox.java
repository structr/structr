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

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.Export;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.*;
import org.structr.mail.entity.relationship.MailboxCONTAINS_EMAILMESSAGESEMailMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Mailbox extends AbstractNode {

	public enum Protocol {
		pop3, imaps
	}

	public static final Property<Iterable<EMailMessage>> emailsProperty   = new EndNodes<>("emails", MailboxCONTAINS_EMAILMESSAGESEMailMessage.class);
	public static final Property<String> hostProperty                     = new StringProperty("host").indexed().notNull();
	public static final Property<String> userProperty                     = new StringProperty("user").indexed().notNull();
	public static final Property<String> overrideMailEntityTypeProperty   = new StringProperty("overrideMailEntityType").indexed();
	public static final Property<String> passwordProperty                 = new EncryptedStringProperty("password").indexed();
	public static final Property<String[]> foldersProperty                = new ArrayProperty("folders", String.class).indexed();
	public static final Property<String> mailProtocolProperty             = new EnumProperty("mailProtocol", Protocol.class).indexed().notNull();
	public static final Property<Integer> portProperty                    = new IntProperty("port").indexed();
	public static final Property<Object> availableFoldersOnServerProperty = new FunctionProperty("availableFoldersOnServer").readFunction("{return Structr.this.getAvailableFoldersOnServer()}");

	public static final View defaultView = new View(Mailbox.class, PropertyView.Public,
		id, type, name
	);

	public static final View uiView      = new View(Mailbox.class, PropertyView.Ui,
		hostProperty, userProperty, overrideMailEntityTypeProperty, passwordProperty, foldersProperty, mailProtocolProperty, portProperty, availableFoldersOnServerProperty
	);

	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {

		boolean valid = super.isValid(errorBuffer);

		valid &= ValidationHelper.isValidPropertyNotNull(this, Mailbox.foldersProperty, errorBuffer);
		valid &= ValidationHelper.isValidPropertyNotNull(this, Mailbox.hostProperty, errorBuffer);
		valid &= ValidationHelper.isValidPropertyNotNull(this, Mailbox.userProperty, errorBuffer);
		valid &= ValidationHelper.isValidPropertyNotNull(this, Mailbox.mailProtocolProperty, errorBuffer);

		return valid;
	}

	public String getHost() {
		return getProperty(hostProperty);
	}

	public String getUser() {
		return getProperty(userProperty);
	}

	public String getPassword() {
		return getProperty(passwordProperty);
	}

	public String getOverrideMailEntityType() {
		return getProperty(overrideMailEntityTypeProperty);
	}

	public String[] getFolders() {
		return getProperty(foldersProperty);
	}

	public Object getMailProtocol() {
		return getProperty(mailProtocolProperty);
	}

	public Integer getPort() {
		return getProperty(portProperty);
	}

	@Export
	public List<String> getAvailableFoldersOnServer(final SecurityContext securityContext) {

		final Iterable<String> result = StructrApp.getInstance(securityContext).command(org.structr.mail.service.FetchFoldersCommand.class).execute(this);
		if (result != null) {

			return StreamSupport.stream(result.spliterator(), false).collect(Collectors.toList());

		} else {

			return new ArrayList<>();
		}
	}
}
