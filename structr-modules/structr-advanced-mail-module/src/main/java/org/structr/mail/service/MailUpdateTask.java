/**
 * Copyright (C) 2010-2018 Structr GmbH
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

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.agent.AbstractTask;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.mail.entity.Mailbox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MailUpdateTask<T extends Mailbox> extends AbstractTask<T> {

	private static final Logger logger = LoggerFactory.getLogger(MailUpdateTask.class.getName());

	public MailUpdateTask() {
		super("MailUpdateTask", null);
	}

	@Override
	public List<T> getWorkObjects() {

		try {

			List<T> newList = new ArrayList<>();

			StructrApp.getInstance().get(Mailbox.class).forEach( (m) -> {
				newList.add((T) m);
			});

			return newList;

		} catch (FrameworkException ex) {

			logger.error("Unable to fetch list of Mails: {}", ex.getMessage());
		}

		return Collections.EMPTY_LIST;
	}
}
