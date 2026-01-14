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
package org.structr.feed.cron;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.agent.Agent;
import org.structr.agent.ReturnValue;
import org.structr.agent.Task;
import org.structr.common.SecurityContext;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.feed.entity.DataFeed;

/**
 *
 */
public class UpdateFeedAgent<T extends DataFeed> extends Agent<T> {

	private static final Logger logger = LoggerFactory.getLogger(UpdateFeedAgent.class.getName());

	@Override
	public ReturnValue processTask(final Task<T> task) throws Throwable {

		logger.debug("Processing task {}", task.getClass().getName());

		final App app = StructrApp.getInstance();
		try (final Tx tx = app.tx(true, true, false)) {

			for (DataFeed feed : task.getWorkObjects()) {

				logger.debug("Updating data feed {} if due", (Object)feed.getProperty(feed.getTraits().key(NodeInterfaceTraitDefinition.NAME_PROPERTY)));

				feed.updateIfDue(SecurityContext.getSuperUserInstance());
			}

			tx.success();
		}

		return ReturnValue.Success;
	}

	@Override
	public Class getSupportedTaskType() {
		return UpdateFeedTask.class;
	}

	@Override
	public boolean createEnclosingTransaction() {
		return false;
	}
}
