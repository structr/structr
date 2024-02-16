/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.websocket.command;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.Property;
import org.structr.core.scheduler.JobQueueManager;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.WebSocketMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Websocket command which lists currently running imports and optionally starts/pauses/resumes/cancels them
 *
 */
public class FileImportCommand extends AbstractCommand {

	private static final Property<List> importJobsProperty = new GenericProperty<>("imports");

	static {

		StructrWebSocket.addCommand(FileImportCommand.class);
	}

	@Override
	public void processMessage(WebSocketMessage webSocketData) throws FrameworkException {

		setDoTransactionNotifications(true);

		final String mode                     = webSocketData.getNodeDataStringValue("mode");		// default: list    start | pause | resume | cancel | abort | cancelAllAfter
		final Long jobId                      = webSocketData.getNodeDataLongValue("jobId");

		final JobQueueManager mgr = JobQueueManager.getInstance();

		final List<GraphObject> result = new ArrayList<>();

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

			case "cancelAllAfter":
				mgr.cancelAllQueuedJobsAfter(jobId);
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

	@Override
	public String getCommand() {
		return "FILE_IMPORT";
	}
}
