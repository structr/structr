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

import org.structr.common.trace.EntryFrame;

/**
 * A scheduled fetch of a mailbox, storing its new messages, with their lifecycle methods.
 */
public class MailFetchFrame extends EntryFrame {

	public MailFetchFrame(final String mailboxId) {
		super(mailboxId, null);
	}

	@Override
	public String kind() {
		return "MAIL_FETCH";
	}

	@Override
	public String describe() {
		return "mail fetch of mailbox " + getName();
	}
}
