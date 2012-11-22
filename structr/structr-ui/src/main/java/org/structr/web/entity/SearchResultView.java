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

import org.neo4j.graphdb.Direction;

import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.core.EntityContext;
import org.structr.core.entity.RelationClass;
import org.structr.core.graph.NodeService;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public class SearchResultView extends View {

	
	public static final org.structr.common.View uiView = new org.structr.common.View(SearchResultView.class, PropertyView.Ui,
		type, name, query
	);
	
	public static final org.structr.common.View publicView = new org.structr.common.View(SearchResultView.class, PropertyView.Public,
		type, name, query
	);
	
	
	static {

//		EntityContext.registerPropertySet(SearchResultView.class, PropertyView.All, Key.values());
//		EntityContext.registerPropertySet(SearchResultView.class, PropertyView.Public, Key.values());
//		EntityContext.registerPropertySet(SearchResultView.class, PropertyView.Ui, Key.values());
		
		EntityContext.registerEntityRelation(SearchResultView.class, Page.class, RelType.CONTAINS, Direction.INCOMING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(SearchResultView.class, Element.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		
		EntityContext.registerSearchablePropertySet(SearchResultView.class, NodeService.NodeIndex.fulltext.name(), uiView.properties());
		EntityContext.registerSearchablePropertySet(SearchResultView.class, NodeService.NodeIndex.keyword.name(),  uiView.properties());

	}
}
