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

package org.structr.core.entity;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletResponse;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.RelationshipType;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;

/**
 *
 * @author Christian Morgner
 */
public class NamedRelation {

	private static final Logger logger = Logger.getLogger(NamedRelation.class.getName());

	private RelationshipType relType = null;
	private Class sourceType = null;
	private Class destType = null;
	private String name = null;

	public NamedRelation(String name, Class sourceType, Class destType, RelationshipType relType) {
		this.sourceType = sourceType;
		this.destType = destType;
		this.relType = relType;
		this.name = name;
	}

	public RelationshipType getRelType() {
		return relType;
	}

	public void setRelType(RelationshipType relType) {
		this.relType = relType;
	}

	public Class getSourceType() {
		return sourceType;
	}

	public void setSourceType(Class sourceType) {
		this.sourceType = sourceType;
	}

	public Class getDestType() {
		return destType;
	}

	public void setDestType(Class destType) {
		this.destType = destType;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Class getEntityClass() {
		return EntityContext.getNamedRelationClass(sourceType, destType, relType);
	}

	public AbstractRelationship newEntityClass() {

		AbstractRelationship rel = null;
		Class type = getEntityClass();

		try {
			rel = (AbstractRelationship)type.newInstance();

		} catch(Throwable t) {
			logger.log(Level.WARNING, "Unable to instantiate relationship entity class", t);
		}

		return rel;
	}

	public List<? extends GraphObject> getRelationships(GraphObject obj) throws FrameworkException {

		Class namedRelationType = getEntityClass();
		List<GraphObject> typeFilteredResults = new LinkedList<GraphObject>();

		// filter relationships for correct type
		for(GraphObject o : obj.getRelationships(relType, getDirectionForType(obj.getStringProperty(AbstractNode.Key.type.name())))) {
			if(o.getClass().equals(namedRelationType)) {
				typeFilteredResults.add(o);
			}
		}

		return typeFilteredResults;
	}

	// ----- private methods -----
	private Direction getDirectionForType(String type) throws FrameworkException {

		if(type.equals(sourceType.getSimpleName())) {
			return Direction.OUTGOING;
		}

		if(type.equals(destType.getSimpleName())) {
			return Direction.INCOMING;
		}

		throw new FrameworkException(HttpServletResponse.SC_BAD_REQUEST, new ErrorBuffer());
	}
}
