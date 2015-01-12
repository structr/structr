/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.cloud.message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.structr.cloud.CloudConnection;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.SyncCommand;

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

		return "PushNodeRequestContainer(" + id + ")";
	}

	@Override
	public void onRequest(CloudConnection serverConnection) throws IOException, FrameworkException {
	}

	@Override
	public void onResponse(CloudConnection clientConnection) throws IOException, FrameworkException {
	}

	@Override
	public void afterSend(CloudConnection conn) {
	}

	@Override
	protected void deserializeFrom(DataInputStream inputStream) throws IOException {

		this.targetNodeId = (String)SyncCommand.deserialize(inputStream);

		super.deserializeFrom(inputStream);
	}

	@Override
	protected void serializeTo(DataOutputStream outputStream) throws IOException {

		SyncCommand.serialize(outputStream, targetNodeId);

		super.serializeTo(outputStream);
	}
}
