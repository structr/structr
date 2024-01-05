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
package org.structr.core.graph;

import org.structr.api.graph.Relationship;
import org.structr.api.graph.RelationshipType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyMap;
import org.structr.schema.NonIndexed;

/**
 *
 *
 */
public interface RelationshipInterface extends GraphObject, NonIndexed {

	public void init(final SecurityContext securityContext, final Relationship dbRel, final Class entityType, final long transactionId);

	public void onRelationshipCreation();
	public void onRelationshipInstantiation();
	public void onRelationshipDeletion();

	public NodeInterface getSourceNode();
	public NodeInterface getTargetNode();
	public NodeInterface getSourceNodeAsSuperUser();
	public NodeInterface getTargetNodeAsSuperUser();
	public NodeInterface getOtherNode(final NodeInterface thisNode);
	public RelationshipType getRelType();

	public Relationship getRelationship();

	public PropertyMap getProperties() throws FrameworkException;

	public String getSourceNodeId();
	public void setSourceNodeId(final String startNodeId) throws FrameworkException;

	public String getTargetNodeId();
	public void setTargetNodeId(final String targetIdNode) throws FrameworkException;

	public int getCascadingDeleteFlag();

	public boolean isInternal();
}
