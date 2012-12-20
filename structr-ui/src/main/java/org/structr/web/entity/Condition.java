/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.web.entity;

import org.neo4j.graphdb.Direction;

import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.NodeService;

//~--- JDK imports ------------------------------------------------------------

import javax.servlet.http.HttpServletRequest;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.core.property.CollectionProperty;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class Condition extends AbstractNode {

	public static final Property<String>            query    = new StringProperty("query");
	public static final CollectionProperty<Page>    pages    = new CollectionProperty<Page>("pages", Page.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<Element> elements = new CollectionProperty<Element>("elements", Element.class, RelType.CONTAINS, Direction.OUTGOING, false);
	
	public static final org.structr.common.View uiView = new org.structr.common.View(Condition.class, PropertyView.Ui,
		type, name, query
	);
	
	public static final org.structr.common.View publicView = new org.structr.common.View(Condition.class, PropertyView.Public,
		type, name, query
	);

	static {

		EntityContext.registerSearchablePropertySet(Condition.class, NodeService.NodeIndex.fulltext.name(), uiView.properties());
		EntityContext.registerSearchablePropertySet(Condition.class, NodeService.NodeIndex.keyword.name(),  uiView.properties());

	}

	//~--- get methods ----------------------------------------------------

	public boolean isSatisfied(HttpServletRequest request, AbstractRelationship rel) {

		String uuid          = rel.getProperty(Component.componentId);
		String requestedUuid = (String) request.getParameter("id");

		if (uuid != null && requestedUuid != null) {

			return uuid.equals(requestedUuid);
		}

		return false;

	}

}
