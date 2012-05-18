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
package org.structr.core.entity.relationships;

import java.security.Principal;
import org.structr.common.PropertyKey;
import org.structr.common.RelType;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.ResourceAccess;

/**
 *
 * @author Christian Morgner
 */
public class Grant extends AbstractRelationship {

	static {
		
		EntityContext.registerNamedRelation("grants", Grant.class, Principal.class, ResourceAccess.class, RelType.SECURITY);
		
	}
	
	public enum Key implements PropertyKey {
		user_id, resource_access_id
	}
	
	@Override
	public PropertyKey getStartNodeIdKey() {
		return Key.user_id;
	}

	@Override
	public PropertyKey getEndNodeIdKey() {
		return Key.resource_access_id;
	}
}
