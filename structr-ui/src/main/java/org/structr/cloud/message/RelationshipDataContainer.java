/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.cloud.message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.structr.cloud.CloudConnection;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.SyncCommand;

/**
 * Serializable data container for a relationship to be transported over network.
 *
 * To be initialized with {@link AbstractRelationship} in constructor.
 *
 *
 */
public class RelationshipDataContainer extends DataContainer implements Comparable<RelationshipDataContainer> {

	protected String sourceStartNodeId;
	protected String sourceEndNodeId;
	protected String relationshipId;
	protected String relType;

	public RelationshipDataContainer() {}

	public RelationshipDataContainer(final RelationshipInterface relationship, final int sequenceNumber) throws FrameworkException {
		this(relationship, sequenceNumber, relationship.getRelationship().getPropertyKeys());
	}

	public RelationshipDataContainer(final RelationshipInterface relationship, final int sequenceNumber, final Iterable<String> propertyKeys) throws FrameworkException {

		super(sequenceNumber);

		relType = relationship.getClass().getSimpleName();

		sourceStartNodeId = relationship.getSourceNode().getUuid();
		sourceEndNodeId   = relationship.getTargetNode().getUuid();
		relationshipId    = relationship.getUuid();

		collectProperties(relationship.getRelationship(), propertyKeys);
	}

	/**
	 * Return name
	 *
	 * @return type class
	 */
	public String getType() {
		return relType;
	}

	/**
	 * Return id of start node in source instance
	 *
	 * @return start node id
	 */
	public String getSourceStartNodeId() {
		return sourceStartNodeId;
	}

	/**
	 * Return id of end node in source instance
	 *
	 * @return end node id
	 */
	public String getSourceEndNodeId() {
		return sourceEndNodeId;
	}

	/**
	 * Return id of relationship entity
	 *
	 * @return relationship id
	 */
	public String getRelationshipId() {
		return relationshipId;
	}

	@Override
	public int compareTo(RelationshipDataContainer t) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean equals(final Object t) {

		RelationshipDataContainer other = (RelationshipDataContainer)t;

		return other.hashCode() == this.hashCode();

//		return (this.getName().equals(other.getName()) && this.getSourceStartNodeId().equals(other.getSourceStartNodeId()) && this.getSourceEndNodeId().equals(other.getSourceEndNodeId()));
	}

	@Override
	public int hashCode() {

		int hashCode = 123;

		if (getType() != null) {
			hashCode += this.getType().hashCode() ^ 11;
		}

		if (getSourceStartNodeId() != null) {
			hashCode += this.getSourceStartNodeId().hashCode() ^ 12;
		}

		if (getSourceEndNodeId() != null) {
			hashCode += this.getSourceEndNodeId().hashCode() ^ 13;
		}

		return hashCode;
	}

	@Override
	public void onRequest(CloudConnection serverConnection) throws IOException, FrameworkException {
		serverConnection.storeRelationship(this);
		sendKeepalive(serverConnection);
	}

	@Override
	public void onResponse(CloudConnection clientConnection) throws IOException, FrameworkException {
	}

	@Override
	public void afterSend(CloudConnection connection) {
	}

	@Override
	protected void deserializeFrom(DataInputStream inputStream) throws IOException {

		this.sourceStartNodeId = (String)SyncCommand.deserialize(inputStream);
		this.sourceEndNodeId   = (String)SyncCommand.deserialize(inputStream);
		this.relationshipId    = (String)SyncCommand.deserialize(inputStream);
		this.relType           = (String)SyncCommand.deserialize(inputStream);

		super.deserializeFrom(inputStream);
	}

	@Override
	protected void serializeTo(DataOutputStream outputStream) throws IOException {

		SyncCommand.serialize(outputStream, sourceStartNodeId);
		SyncCommand.serialize(outputStream, sourceEndNodeId);
		SyncCommand.serialize(outputStream, relationshipId);
		SyncCommand.serialize(outputStream, relType);

		super.serializeTo(outputStream);
	}
}
