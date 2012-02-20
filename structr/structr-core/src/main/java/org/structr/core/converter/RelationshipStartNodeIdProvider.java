/*
 *  Copyright (C) 2012 Axel Morgner
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

package org.structr.core.converter;

import org.structr.core.GraphObject;
import org.structr.core.PropertyConverter;
import org.structr.core.Value;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;

/**
 *
 * @author Christian Morgner
 */
public class RelationshipStartNodeIdProvider extends PropertyConverter<GraphObject, String> {

	@Override
	public GraphObject convertForSetter(String source, Value value) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String convertForGetter(GraphObject source, Value value) {

		if(currentObject instanceof AbstractRelationship) {

			AbstractRelationship rel = (AbstractRelationship)currentObject;
			return rel.getStartNode().getStringProperty(AbstractNode.Key.uuid);
		}

		return null;
	}
}
