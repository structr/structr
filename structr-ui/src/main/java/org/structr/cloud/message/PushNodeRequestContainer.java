/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.cloud.message;

import java.io.IOException;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.ExportContext;
import org.structr.common.error.FrameworkException;

/**
 * Encapsulates a push request for a node
 *
 * @author Christian Morgner
 */
public class PushNodeRequestContainer extends DataContainer {

	private String targetNodeId = "";

	public PushNodeRequestContainer() {
		super(0);
	}

	public PushNodeRequestContainer(final String targetNodeId) {

		super(0);

		this.targetNodeId = targetNodeId;
	}

	/**
	 * @return the targetNodeId
	 */
	public String getTargetNodeId() {
		return targetNodeId;
	}

	/**
	 * @param targetNodeId the targetNodeId to set
	 */
	public void setTargetNodeId(String targetNodeId) {
		this.targetNodeId = targetNodeId;
	}

	@Override
	public String toString() {

		return "PushNodeRequestContainer(" + targetNodeId + ")";
	}

	@Override
	public void onRequest(CloudConnection serverConnection, ExportContext context) throws IOException, FrameworkException {
		serverConnection.send(ack());
	}

	@Override
	public void onResponse(CloudConnection clientConnection, ExportContext context) throws IOException, FrameworkException {
		context.progress();
	}

	@Override
	public void afterSend(CloudConnection conn) {
	}
}
