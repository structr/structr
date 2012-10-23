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



package org.structr.web.entity.html;

import org.structr.common.property.Property;
import java.util.List;
import org.apache.commons.lang.ArrayUtils;

import org.neo4j.graphdb.Direction;
import org.structr.common.*;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Linkable;
import org.structr.core.entity.RelationClass;
import org.structr.core.notion.PropertyNotion;
import org.structr.web.common.HtmlProperty;
import org.structr.web.entity.Content;

//~--- classes ----------------------------------------------------------------

/**
 * @author Axel Morgner
 */
public class Script extends HtmlElement {

	public static final Property<String> _src     = new HtmlProperty("src");
	public static final Property<String> _async   = new HtmlProperty("async");
	public static final Property<String> _defer   = new HtmlProperty("defer");
	public static final Property<String> _type    = new HtmlProperty("type");
	public static final Property<String> _charset = new HtmlProperty("charset");

	public static final Property<String>         linkableId = new Property<String>("linkable_id");
	public static final Property<List<Linkable>> linkable   = new Property<List<Linkable>>("linkable");

	public static final View uiView = new View(Script.class, PropertyView.Ui,
		linkableId, linkable
	);

	public static final View htmlView = new View(Script.class, PropertyView.Html,
		_src, _async, _defer, _type, _charset
	);

	
	//~--- static initializers --------------------------------------------

	static {

//		EntityContext.registerPropertySet(Script.class, PropertyView.All, HtmlElement.UiKey.values());
//		EntityContext.registerPropertySet(Script.class, PropertyView.Public, HtmlElement.UiKey.values());
//		EntityContext.registerPropertySet(Script.class, PropertyView.Html, PropertyView.Html, htmlAttributes);
		
		EntityContext.registerEntityRelation(Script.class, Content.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Script.class, Head.class, RelType.CONTAINS, Direction.INCOMING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Script.class, Div.class, RelType.CONTAINS, Direction.INCOMING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Script.class, Linkable.class, RelType.LINK, Direction.OUTGOING, RelationClass.Cardinality.ManyToOne, new PropertyNotion(AbstractNode.name),
			RelationClass.DELETE_NONE);
		EntityContext.registerPropertyRelation(Script.class, Script.linkableId, Linkable.class, RelType.LINK, Direction.OUTGOING, RelationClass.Cardinality.ManyToOne,
			new PropertyNotion(AbstractNode.uuid), RelationClass.DELETE_NONE);

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public Property[] getHtmlAttributes() {

		return (Property[]) ArrayUtils.addAll(super.getHtmlAttributes(), htmlView.properties());

	}
}
