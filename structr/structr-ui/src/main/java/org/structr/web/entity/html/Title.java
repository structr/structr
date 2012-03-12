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

package org.structr.web.entity.html;

import org.neo4j.graphdb.Direction;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.core.EntityContext;
import org.structr.core.entity.RelationClass;
import org.structr.web.entity.Content;
import org.structr.web.entity.Element;

/**
 * @author Axel Morgner
 */
public class Title extends HtmlElement {

	static {
		EntityContext.registerPropertySet(org.structr.web.entity.html.Title.class, PropertyView.All,	HtmlElement.UiKey.values());
		EntityContext.registerPropertySet(org.structr.web.entity.html.Title.class, PropertyView.Public,	HtmlElement.UiKey.values());
		EntityContext.registerEntityRelation(org.structr.web.entity.html.Title.class, Head.class, RelType.CONTAINS, Direction.INCOMING, RelationClass.Cardinality.OneToOne);
		EntityContext.registerEntityRelation(org.structr.web.entity.html.Title.class, Content.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
	}
}
