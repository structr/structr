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

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.ExportContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.SyncCommand;

/**
 * Serializable data container for a relationship to be transported over network.
 *
 * To be initialized with {@link AbstractRelationship} in constructor.
 *
 * @author axel
 */
public class RelationshipDataContainer extends DataContainer implements Comparable<RelationshipDataContainer> {

	protected String sourceStartNodeId;
	protected String sourceEndNodeId;
	protected String relationshipId;
	protected String relType;

	public RelationshipDataContainer() {}

	public RelationshipDataContainer(final RelationshipInterface relationship, final int sequenceNumber) throws FrameworkException {

		super(sequenceNumber);

		relType = relationship.getClass().getSimpleName();

		sourceStartNodeId = relationship.getSourceNode().getUuid();
		sourceEndNodeId   = relationship.getTargetNode().getUuid();
		relationshipId    = relationship.getUuid();

		collectProperties(relationship.getRelationship());
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
	public void onRequest(CloudConnection serverConnection, ExportContext context) throws IOException, FrameworkException {

		serverConnection.storeRelationship(this);
		serverConnection.send(ack());

		context.progress();
	}

	@Override
	public void onResponse(CloudConnection clientConnection, ExportContext context) throws IOException, FrameworkException {
		context.progress();
	}

	@Override
	public void afterSend(CloudConnection connection) {
	}

	@Override
	protected void deserializeFrom(Reader reader) throws IOException {

		this.sourceStartNodeId = (String)SyncCommand.deserialize(reader);
		this.sourceEndNodeId   = (String)SyncCommand.deserialize(reader);
		this.relationshipId    = (String)SyncCommand.deserialize(reader);
		this.relType           = (String)SyncCommand.deserialize(reader);
		
		super.deserializeFrom(reader);
	}

	@Override
	protected void serializeTo(Writer writer) throws IOException {

		SyncCommand.serialize(writer, sourceStartNodeId);
		SyncCommand.serialize(writer, sourceEndNodeId);
		SyncCommand.serialize(writer, relationshipId);
		SyncCommand.serialize(writer, relType);

		super.serializeTo(writer);
	}
}
