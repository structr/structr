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
import org.structr.common.error.ErrorBuffer;
import org.structr.common.SecurityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;

/**
 * An interface that allows you to be notified when a
 * GraphObject is modified, with the option to veto the modification.
 * In order to use this interface, you must register your
 * implementation in {@see EntityContext}.
 *
 * @author Christian Morgner
 */
public interface VetoableGraphObjectListener {

	public boolean begin(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer);
	public boolean commit(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer);
	public boolean rollback(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer);

	public boolean propertyModified(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer, AbstractNode entity, String key, Object oldValue, Object newValue);

	public boolean relationshipCreated(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer, AbstractRelationship relationship);
	public boolean relationshipDeleted(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer, AbstractRelationship relationship);

	public boolean graphObjectCreated(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer, GraphObject graphObject);
	public boolean graphObjectModified(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer, GraphObject graphObject);
	public boolean graphObjectDeleted(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer, long id, Map<String, Object> properties);

	public boolean wasVisited(List<GraphObject> traversedNodes, long transactionKey, ErrorBuffer errorBuffer, SecurityContext securityContext);
}
