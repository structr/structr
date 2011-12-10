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
import org.structr.common.SecurityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;

/**
 * An interface that allows you to be notified when a
 * GraphObject is modified, with the option to veto the modification.
 * In order to use this interface, you must register your
 * implementation in {@see EntityContext}.
 *
 * @author Christian Morgner
 */
public interface VetoableGraphObjectListener {

	public void begin(SecurityContext securityContext, long transactionKey);
	public void commit(SecurityContext securityContext, long transactionKey);
	public void rollback(SecurityContext securityContext, long transactionKey);

	public void propertyModified(SecurityContext securityContext, long transactionKey, AbstractNode entity, String key, Object oldValue, Object newValue);

	public void relationshipCreated(SecurityContext securityContext, long transactionKey, AbstractNode startNode, AbstractNode endNode, StructrRelationship relationship);
	public void relationshipDeleted(SecurityContext securityContext, long transactionKey, StructrRelationship relationship);

	public void graphObjectCreated(SecurityContext securityContext, long transactionKey, GraphObject graphObject);
	public void graphObjectModified(SecurityContext securityContext, long transactionKey, GraphObject graphObject);
	public void graphObjectDeleted(SecurityContext securityContext, long transactionKey, long id, Map<String, Object> properties);

	public void wasVisited(List<GraphObject> traversedNodes, long transactionKey, SecurityContext securityContext);
}
