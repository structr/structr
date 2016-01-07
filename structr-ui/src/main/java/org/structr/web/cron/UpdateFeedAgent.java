/**
 * Copyright (C) 2010-2015 Structr GmbH
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

package org.structr.web.cron;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.agent.Agent;
import org.structr.agent.ReturnValue;
import org.structr.agent.Task;
import org.structr.web.entity.feed.DataFeed;

/**
 *
 */
public class UpdateFeedAgent<T extends DataFeed> extends Agent<T> {

	private static final Logger logger = Logger.getLogger(UpdateFeedAgent.class.getName());
	
	@Override
	public ReturnValue processTask(final Task task) throws Throwable {
		
		logger.log(Level.INFO, "Processing task {0}", task.getClass().getName());

		for (DataFeed feed : (List<T>) task.getNodes()) {
			
			logger.log(Level.INFO, "Updating data feed {0} if due", feed.getProperty(DataFeed.name));
			
			feed.updateIfDue();
		}

		return ReturnValue.Success;
	}

	@Override
	public Class getSupportedTaskType() {
		return UpdateFeedTask.class;
	}
	
}
