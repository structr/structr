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
package org.structr.mail.entity.relationship;

import org.structr.core.entity.OneToMany;
import org.structr.core.entity.Relation;
import org.structr.mail.entity.EMailMessage;
import org.structr.mail.entity.Mailbox;

public class MailboxCONTAINS_EMAILMESSAGESEMailMessage extends OneToMany<Mailbox, EMailMessage> {

	@Override
	public Class<Mailbox> getSourceType() {
		return Mailbox.class;
	}

	@Override
	public Class<EMailMessage> getTargetType() {
		return EMailMessage.class;
	}

	@Override
	public String name() {
		return "CONTAINS_EMAILMESSAGES";
	}

	@Override
	public int getCascadingDeleteFlag() {
		return Relation.SOURCE_TO_TARGET;
	}
}
