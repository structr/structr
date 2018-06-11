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
package org.structr.websocket.command;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.Property;
import org.structr.core.scheduler.JobQueueManager;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.WebSocketMessage;

/**
 * Websocket command which lists currently running imports and optionally starts/pauses/resumes/cancels them
 *
 */
public class FileImportCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(FileImportCommand.class.getName());

	private static final Property<List> importJobsProperty = new GenericProperty<>("imports");

	//~--- static initializers --------------------------------------------

	static {

		StructrWebSocket.addCommand(FileImportCommand.class);

	}

	//~--- methods --------------------------------------------------------

	@Override
	public void processMessage(WebSocketMessage webSocketData) throws FrameworkException {

		setDoTransactionNotifications(true);

		final Map<String, Object> properties  = webSocketData.getNodeData();
		final String mode                     = (String) properties.get("mode");		// default: list    start | pause | resume | cancel | abort
		final Long jobId                      = (Long) properties.get("jobId");

		final JobQueueManager mgr = JobQueueManager.getInstance();

		final List<GraphObject> result = new LinkedList<>();

		switch (mode) {

			case "start":
				mgr.startJob(jobId);
				break;

			case "pause":
				mgr.pauseRunningJob(jobId);
				break;

			case "resume":
				mgr.resumePausedJob(jobId);
				break;

			case "abort":
				mgr.abortActiveJob(jobId);
				break;

			case "cancel":
				mgr.cancelQueuedJob(jobId);
				break;

			case "list":
			default:
				final GraphObjectMap importsContainer = new GraphObjectMap();

				importsContainer.put(importJobsProperty, mgr.listJobs());
				result.add(importsContainer);
		}

			webSocketData.setResult(result);
			webSocketData.setRawResultCount(1);
			getWebSocket().send(webSocketData, true);

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {
		return "FILE_IMPORT";
	}

}
