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

import java.util.logging.Logger;
import org.neo4j.graphdb.Direction;
import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.core.EntityContext;
import org.structr.core.entity.RelationClass.Cardinality;
import org.structr.core.node.NodeService;

/**
 *
 * @author Christian Morgner
 */
public class ResourceAccess extends AbstractNode {

	private static final Logger logger = Logger.getLogger(ResourceAccess.class.getName());

	private String cachedUriPart = null;
	
	//~--- static initializers --------------------------------------------

	static {

		EntityContext.registerPropertySet(ResourceAccess.class, PropertyView.All, Key.values());
		EntityContext.registerPropertySet(ResourceAccess.class, PropertyView.Ui, Key.values());

		EntityContext.registerSearchablePropertySet(ResourceAccess.class, NodeService.NodeIndex.fulltext.name(), File.Key.values());
		EntityContext.registerSearchablePropertySet(ResourceAccess.class, NodeService.NodeIndex.keyword.name(), File.Key.values());
		
		EntityContext.registerEntityRelation(ResourceAccess.class, Principal.class, RelType.SECURITY, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Principal.class, ResourceAccess.class, RelType.SECURITY, Direction.OUTGOING, Cardinality.ManyToMany);
		

	}

	//~--- constant enums -------------------------------------------------

	public enum Key implements PropertyKey {

		uri

	}

	//~--- methods --------------------------------------------------------

	public String getUriPart() {
		
		if(cachedUriPart == null) {
			cachedUriPart = getStringProperty(Key.uri);
		}
		
		return cachedUriPart;
	}
}
