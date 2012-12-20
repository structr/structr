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

import org.structr.core.property.Property;
import org.apache.commons.lang.ArrayUtils;

import org.neo4j.graphdb.Direction;

import org.structr.common.*;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Linkable;
import org.structr.core.notion.PropertyNotion;
import org.structr.core.property.CollectionProperty;
import org.structr.core.property.EntityIdProperty;
import org.structr.core.property.EntityProperty;
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
	
	public static final CollectionProperty<Head> heads      = new CollectionProperty<Head>("heads", Head.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final EntityProperty<Linkable> linkable   = new EntityProperty<Linkable>("linkable", Linkable.class, RelType.LINK, Direction.OUTGOING, new PropertyNotion(AbstractNode.name), true);
	public static final Property<String>         linkableId = new EntityIdProperty("linkableId", linkable);

	public static final View uiView = new View(Link.class, PropertyView.Ui,
		linkableId, linkable
	);
	
	public static final View htmlView = new View(Link.class, PropertyView.Html,
		_href, _rel, _media, _hreflang, _type, _sizes
	);

	@Override
	public Property[] getHtmlAttributes() {

		return (Property[]) ArrayUtils.addAll(super.getHtmlAttributes(), htmlView.properties());

	}

	@Override
	public boolean isVoidElement() {

		return true;

	}

}
