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
import java.util.List;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.ExportContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.RelationshipInterface;

/**
 * Encapsulates a pull request for a node
 *
 *
 * @author Christian Morgner
 */
public class PullRelationship extends RelationshipDataContainer {

	private String key    = null;
	private int nodeIndex = 0;

	public PullRelationship() {}

	public PullRelationship(final String key, final int nodeIndex) {

		this.key       = key;
		this.nodeIndex = nodeIndex;
	}

	@Override
	public void onRequest(CloudConnection serverConnection, ExportContext context) throws IOException, FrameworkException {

		final Object value = serverConnection.getValue(key + "Rels");
		if (value instanceof List) {

			final List<RelationshipInterface> relationships = (List<RelationshipInterface>)value;
			final RelationshipInterface relationship        = relationships.get(nodeIndex);

			serverConnection.send(new RelationshipDataContainer(relationship, nodeIndex));
		}
	}

	@Override
	public void onResponse(CloudConnection clientConnection, final ExportContext context) throws IOException, FrameworkException {

		clientConnection.storeRelationship(this);
		context.progress();

		clientConnection.send(ack());
	}

	@Override
	public void afterSend(CloudConnection conn) {
	}

	@Override
	public Object getPayload() {
		return null;
	}
}
