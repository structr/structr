/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
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

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.RelationshipType;

import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

//~--- classes ----------------------------------------------------------------

/**
 * Describes a relation type between two node classes,
 * defined by source and destination type (node entity classes) and the
 * relationship type.
 *
 * Direction is always OUTGOING from source to destination by definition.
 *
 * A @see RelationshipMapping is marked with a combined key with name "type" of the form
 * "SourceType RELTYPE DestType".
 *
 *
 * @author Christian Morgner
 * @author Axel Morgner
 */
public class RelationshipMapping {

	private static final Logger logger = Logger.getLogger(RelationshipMapping.class.getName());

	//~--- fields ---------------------------------------------------------

	private Class destType           = null;
	private String name              = null;
	private RelationshipType relType = null;
	private Class sourceType         = null;

	//~--- constructors ---------------------------------------------------

	public RelationshipMapping(String name, Class sourceType, Class destType, RelationshipType relType) {

		this.sourceType = sourceType;
		this.destType   = destType;
		this.relType    = relType;
		this.name       = name;
	}

	//~--- methods --------------------------------------------------------

	public AbstractRelationship newEntityClass() {

		AbstractRelationship rel = null;
		Class type               = getEntityClass();

		try {
			rel = (AbstractRelationship) type.newInstance();
		} catch (Throwable t) {
			logger.log(Level.WARNING, "Unable to instantiate relationship entity class", t);
		}

		return rel;
	}

	//~--- get methods ----------------------------------------------------

	public RelationshipType getRelType() {
		return relType;
	}

	public Class getSourceType() {
		return sourceType;
	}

	public Class getDestType() {
		return destType;
	}

	public String getName() {
		return name;
	}

	public Class getEntityClass() {
		return EntityContext.getNamedRelationClass(sourceType, destType, relType);
	}

	public List<AbstractRelationship> getRelationships(GraphObject obj) throws FrameworkException {
                
                AbstractNode node = (AbstractNode) obj;

		Class combinedRelType               = getEntityClass();
		List<AbstractRelationship> relsFilteredByType = new LinkedList<AbstractRelationship>();

		// filter relationships for correct combined relationship type
		for (AbstractRelationship rel : node.getRelationships(relType, getDirectionForType(node.getClass()))) {

			if (rel.getClass().equals(combinedRelType)) {

				relsFilteredByType.add(rel);

			}

		}

		return relsFilteredByType;
	}

	// ----- private methods -----
	private Direction getDirectionForType(Class type) throws FrameworkException {

		Class localType = type;
		
		while(localType != null && !Object.class.equals(localType)) {
			
			if (localType.equals(sourceType)) {
				return Direction.OUTGOING;
				
			} else  if (localType.equals(destType)) {
				
				return Direction.INCOMING;
			}
			
			for(Class interfaceClass : EntityContext.getInterfacesForType(localType)) {
				
				if (interfaceClass.equals(sourceType)) {
					return Direction.OUTGOING;
					
				} else if (interfaceClass.equals(destType)) {
					return Direction.INCOMING;
				}
			}
			
			localType = localType.getSuperclass();
		}

		throw new FrameworkException(HttpServletResponse.SC_BAD_REQUEST, new ErrorBuffer());
	}

	//~--- set methods ----------------------------------------------------

	public void setRelType(RelationshipType relType) {
		this.relType = relType;
	}

	public void setSourceType(Class sourceType) {
		this.sourceType = sourceType;
	}

	public void setDestType(Class destType) {
		this.destType = destType;
	}

	public void setName(String name) {
		this.name = name;
	}
}
