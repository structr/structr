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
package org.structr.common;

import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.RelationshipFactory;

/**
 * Defines methods that are used by {@link NodeFactory} and
 * {@link RelationshipFactory} when creating nodes.
 *
 *
 */
public interface FactoryDefinition {

	/**
	 * @return an uninitialized instance of a generic relationship
	 */
	public AbstractRelationship createGenericRelationship();
	public Class getGenericRelationshipType();

	/**
	 * @return an uninitialized instance of a generic node
	 */
	public AbstractNode createGenericNode();
	public Class getGenericNodeType();

	/**
	 * Indicates whether the given class is a generic type according to
	 * this class.
	 *
	 * @param entityClass the type to check
	 * @return whether the given type is a generic type
	 */
	public boolean isGeneric(Class<?> entityClass);

	/**
	 * Returns an entity name for the given node. A node type can be defined
	 * by the node's surroundings or by a given type property. Its up to the
	 * user of structr to specify this.
	 *
	 * @param node
	 * @return the entity name as returned by Class.getSimpleName()
	 */
	public Class determineNodeType(Node node);

	/**
	 * Returns an entity name for the given relationship. A relationship type
	 * can be defined by the relationship's surroundings or by a given type
	 * property. Its up to the user of Structr to specify this.
	 *
	 * @param relationship
	 * @return the entity name as returned by Class.getSimpleName()
	 */
	public Class determineRelationshipType(Relationship relationship);
}
