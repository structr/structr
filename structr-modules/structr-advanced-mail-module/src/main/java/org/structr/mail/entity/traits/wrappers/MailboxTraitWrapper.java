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
package org.structr.mail.entity.traits.wrappers;

import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.mail.entity.Mailbox;
import org.structr.mail.entity.traits.definitions.MailboxTraitDefinition;

public class MailboxTraitWrapper extends AbstractNodeTraitWrapper implements Mailbox {

	public MailboxTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public String getHost() {
		return wrappedObject.getProperty(traits.key(MailboxTraitDefinition.HOST_PROPERTY));
	}

	public String getUser() {
		return wrappedObject.getProperty(traits.key(MailboxTraitDefinition.USER_PROPERTY));
	}

	public String getPassword() {
		return wrappedObject.getProperty(traits.key(MailboxTraitDefinition.PASSWORD_PROPERTY));
	}

	public String getOverrideMailEntityType() {
		return wrappedObject.getProperty(traits.key(MailboxTraitDefinition.OVERRIDE_MAIL_ENTITY_TYPE_PROPERTY));
	}

	public String[] getFolders() {
		return wrappedObject.getProperty(traits.key(MailboxTraitDefinition.FOLDERS_PROPERTY));
	}

	public Object getMailProtocol() {
		return wrappedObject.getProperty(traits.key(MailboxTraitDefinition.MAIL_PROTOCOL_PROPERTY));
	}

	public Integer getPort() {
		return wrappedObject.getProperty(traits.key(MailboxTraitDefinition.PORT_PROPERTY));
	}
}
