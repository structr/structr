/*
 *  Copyright (C) 2011 Axel Morgner
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

package org.structr.core;

import java.util.List;
import java.util.Map;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.RelationshipType;
import org.structr.common.PropertyView;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;

/**
 * A common base class for {@see AbstractNode} and {@see StructrRelationship}.
 *
 * @author Christian Morgner
 */
public interface GraphObject {

	// ----- common to both types -----
	public long getId();
	public String getType();

	public Iterable<String> getPropertyKeys(PropertyView propertyView);
	public void setProperty(String key, Object value);
	public Object getProperty(String key);
	public void removeProperty(String key);

	// ----- rels only -----
	public Long getStartNodeId();
	public Long getEndNodeId();

	// ----- nodes only -----
	public Map<RelationshipType, Long> getRelationshipInfo(Direction direction);
	public List<StructrRelationship> getRelationships(RelationshipType type, Direction dir);

	// ----- editing methods -----
	public boolean delete();

}
