/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or License, or (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.web.entity;

import java.util.Collections;
import java.util.List;
import org.neo4j.graphdb.Direction;
import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.RelationClass;
import org.structr.core.node.CypherQueryCommand;
import org.structr.core.node.NodeService;

/**
 *
 * @author Christian Morgner
 */
public class View extends AbstractNode {

	public enum Key implements PropertyKey{ type, name, query }


	static {

		EntityContext.registerPropertySet(View.class, PropertyView.All, Key.values());
		EntityContext.registerPropertySet(View.class, PropertyView.Public, Key.values());
		EntityContext.registerPropertySet(View.class, PropertyView.Ui, Key.values());
		
		EntityContext.registerEntityRelation(View.class, Page.class, RelType.CONTAINS, Direction.INCOMING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(View.class, Element.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		
		EntityContext.registerSearchablePropertySet(View.class, NodeService.NodeIndex.fulltext.name(), Key.values());
		EntityContext.registerSearchablePropertySet(View.class, NodeService.NodeIndex.keyword.name(), Key.values());

	}	
	
	public List<GraphObject> getComponents() {
		
		try {
			
			String query = getStringProperty("query");

			return (List<GraphObject>)Services.command(securityContext, CypherQueryCommand.class).execute(query);
			
		} catch(Throwable t) {
			t.printStackTrace();
		}
		
		return Collections.emptyList();
	}
}
