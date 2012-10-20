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

//~--- classes ----------------------------------------------------------------

/**
 * @author Axel Morgner
 */
public class Link extends HtmlElement {

	public static final Property<String> _href     = new HtmlProperty("href");
	public static final Property<String> _rel      = new HtmlProperty("rel");
	public static final Property<String> _media    = new HtmlProperty("media");
	public static final Property<String> _hreflang = new HtmlProperty("hreflang");
	public static final Property<String> _type     = new HtmlProperty("type");
	public static final Property<String> _sizes    = new HtmlProperty("sizes");

	public static final Property<String>         linkableId = new Property<String>("linkable_id");
	public static final Property<List<Linkable>> linkable   = new Property<List<Linkable>>("linkable");

	public static final View uiView = new View(PropertyView.Ui,
		linkableId, linkable
	);
	
	public static final View htmlView = new View(PropertyView.Html,
		_href, _rel, _media, _hreflang, _type, _sizes
	);

	//~--- static initializers --------------------------------------------

	static {

//		EntityContext.registerPropertySet(Link.class, PropertyView.All, HtmlElement.UiKey.values());
//		EntityContext.registerPropertySet(Link.class, PropertyView.Public, HtmlElement.UiKey.values());
//		EntityContext.registerPropertySet(Link.class, PropertyView.Html, PropertyView.Html, htmlAttributes);
		
		EntityContext.registerEntityRelation(Link.class, Head.class, RelType.CONTAINS, Direction.INCOMING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Link.class, Linkable.class, RelType.LINK, Direction.OUTGOING, RelationClass.Cardinality.ManyToOne, new PropertyNotion(AbstractNode.name), RelationClass.DELETE_NONE);
		EntityContext.registerPropertyRelation(Link.class, linkableId, Linkable.class, RelType.LINK, Direction.OUTGOING, RelationClass.Cardinality.ManyToOne, new PropertyNotion(AbstractNode.uuid), RelationClass.DELETE_NONE);

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public Property[] getHtmlAttributes() {

		return (Property[]) ArrayUtils.addAll(super.getHtmlAttributes(), htmlView.properties());

	}

	@Override
	public boolean isVoidElement() {

		return true;

	}

}
