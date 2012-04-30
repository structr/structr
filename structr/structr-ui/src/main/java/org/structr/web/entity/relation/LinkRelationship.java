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
package org.structr.web.entity.relation;

import java.util.LinkedHashSet;
import java.util.Set;
import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractRelationship;
import org.structr.web.entity.Resource;
import org.structr.web.entity.html.A;

/**
 *
 * @author Christian Morgner
 */
public class LinkRelationship extends AbstractRelationship {

	public enum Key implements PropertyKey {
		sourceId, targetId, type
	}
	
	static {
		
		EntityContext.registerNamedRelation("hyperlink", LinkRelationship.class, A.class, Resource.class, RelType.LINK);
		EntityContext.registerPropertySet(LinkRelationship.class, PropertyView.Ui, Key.values());
	}
	
	@Override
	public PropertyKey getStartNodeIdKey() {
		return Key.sourceId;
	}

	@Override
	public PropertyKey getEndNodeIdKey() {
		return Key.targetId;
	}
        
}
