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

import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.RelationClass;
import org.structr.core.node.NodeService;

//~--- JDK imports ------------------------------------------------------------

import javax.servlet.http.HttpServletRequest;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class Condition extends AbstractNode {

	static {

		EntityContext.registerPropertySet(Condition.class, PropertyView.All, Condition.Key.values());
		EntityContext.registerPropertySet(Condition.class, PropertyView.Public, Condition.Key.values());
		EntityContext.registerPropertySet(Condition.class, PropertyView.Ui, Condition.Key.values());
		EntityContext.registerEntityRelation(Condition.class, Page.class, RelType.CONTAINS, Direction.INCOMING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Condition.class, Element.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerSearchablePropertySet(Condition.class, NodeService.NodeIndex.fulltext.name(), Condition.Key.values());
		EntityContext.registerSearchablePropertySet(Condition.class, NodeService.NodeIndex.keyword.name(), Condition.Key.values());

	}

	//~--- constant enums -------------------------------------------------

	public enum Key implements PropertyKey{ type, name, query }

	//~--- get methods ----------------------------------------------------

	public boolean isSatisfied(HttpServletRequest request, AbstractRelationship rel) {

		String uuid          = rel.getStringProperty("componentId");
		String requestedUuid = (String) request.getParameter("id");

		if (uuid != null && requestedUuid != null) {

			return uuid.equals(requestedUuid);
		}

		return false;

	}

}
