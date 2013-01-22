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
package org.structr.common;

import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;

/**
 * Defines methods that are used by {@link NodeFactory} and
 * {@link RelationshipFactory} when creating nodes with unknown types / generic
 * nodes.
 *
 * @author Christian Morgner
 */
public interface GenericFactory {
	
	/**
	 * @return an uninitialized instance of a generic relationship
	 */
	public AbstractRelationship createGenericRelationship();
	
	/**
	 * @return an uninitialized instance of a generic node
	 */
	public AbstractNode createGenericNode();
	
	/**
	 * Indicates whether the given class is a generic type according to
	 * this class.
	 * 
	 * @param entityClass the type to check
	 * @return whether the given type is a generic type
	 */
	public boolean isGeneric(Class<?> entityClass);
}
