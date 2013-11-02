/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.entity;

import org.neo4j.graphdb.RelationshipType;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;

/**
 * Defines constants for structr's relationship entities.
 *
 * @author Christian Morgner
 */
public interface Relation<A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target> extends RelationshipInterface, RelationshipType {

	/**
	 * No cascading delete.
	 */
	public static final int NONE              = 0;
	
	/**
	 * Target node will be deleted if source node
	 * gets deleted.
	 */
	public static final int SOURCE_TO_TARGET  = 1;
	
	/**
	 * Source node will be deleted if target node
	 * gets deleted.
	 */
	public static final int TARGET_TO_SOURCE  = 2;
	
	/**
	 * Both nodes will be deleted whenever one of
	 * the two nodes gets deleted.
	 * 
	 */
	public static final int ALWAYS            = 3;
	
	/**
	 * Source and/or target nodes will be deleted
	 * if they become invalid.
	 */
	public static final int CONSTRAINT_BASED  = 4;

	
	public enum Cardinality { OneToOne, ManyToOne, OneToMany, ManyToMany }

	public enum Multiplicity { One, Many }
	
	public Class<A> getSourceType();
	public Class<B> getTargetType();
	
	//public RelationshipType getRelationshipType();
	
	public Multiplicity getSourceMultiplicity();
	public Multiplicity getTargetMultiplicity();
	
	public S getSource();
	public T getTarget();

	public int getCascadingDeleteFlag();
	
	public void ensureCardinality(final NodeInterface sourceNode, final NodeInterface targetNode) throws FrameworkException;
}
