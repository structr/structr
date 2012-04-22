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

/**
 * @author Axel Morgner
 */
public class H5 extends HtmlElement {

	static {
		EntityContext.registerPropertySet(H5.class, PropertyView.All,		HtmlElement.UiKey.values());
		EntityContext.registerPropertySet(H5.class, PropertyView.Public,	HtmlElement.UiKey.values());
		EntityContext.registerPropertySet(H5.class, PropertyView.Html, PropertyView.Html,	HtmlElement.htmlAttributes);

		EntityContext.registerEntityRelation(H5.class, Content.class,	RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
	}
}
