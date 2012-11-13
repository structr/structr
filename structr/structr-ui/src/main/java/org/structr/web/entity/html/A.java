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
import org.structr.common.property.Property;

import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.View;
import org.structr.common.property.GenericProperty;
import org.structr.common.property.StringProperty;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Linkable;
import org.structr.core.entity.RelationClass;
import org.structr.core.entity.RelationClass.Cardinality;
import org.structr.core.notion.PropertyNotion;
import org.structr.web.common.HtmlProperty;
import org.structr.web.entity.Content;

//~--- classes ----------------------------------------------------------------

/**
 * @author Axel Morgner
 */
public class A extends HtmlElement {

	public static final Property<String> _href     = new HtmlProperty("href");
	public static final Property<String> _target   = new HtmlProperty("target");
	public static final Property<String> _ping     = new HtmlProperty("ping");
	public static final Property<String> _rel      = new HtmlProperty("rel");
	public static final Property<String> _media    = new HtmlProperty("media");
	public static final Property<String> _hreflang = new HtmlProperty("hreflang");
	public static final Property<String> _type     = new HtmlProperty("type");

	public static final Property<String>         linkableId = new StringProperty("linkable_id");
	public static final Property<List<Linkable>> linkable   = new GenericProperty<List<Linkable>>("linkable");

	public static final View uiView = new View(A.class, PropertyView.Ui,
		linkableId, linkable
	);
	
	public static final View htmlView = new View(A.class, PropertyView.Html,
		_href, _target, _ping, _rel, _media, _hreflang, _type
	);
	
	//~--- static initializers --------------------------------------------

	static {

//		EntityContext.registerPropertySet(A.class, PropertyView.Ui, UiKey.values());
//		EntityContext.registerPropertySet(A.class, PropertyView.Html, PropertyView.Html, htmlAttributes);
		
		EntityContext.registerEntityRelation(A.class, Content.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(A.class, Span.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(A.class, Img.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(A.class, Div.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(A.class, Section.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(A.class, P.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(A.class, H1.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(A.class, H2.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(A.class, H3.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(A.class, H4.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(A.class, H5.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(A.class, H6.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(A.class, Div.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(A.class, P.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(A.class, Li.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);

		EntityContext.registerEntityRelation(A.class, Linkable.class, RelType.LINK, Direction.OUTGOING, Cardinality.ManyToOne, new PropertyNotion(AbstractNode.name), RelationClass.DELETE_NONE);
		EntityContext.registerPropertyRelation(A.class, linkableId, Linkable.class, RelType.LINK, Direction.OUTGOING, Cardinality.ManyToOne, new PropertyNotion(AbstractNode.uuid), RelationClass.DELETE_NONE);

	}

	//~--- methods --------------------------------------------------------

	@Override
	public boolean avoidWhitespace() {

		return true;

	}

	@Override
	public Property[] getHtmlAttributes() {

		return (Property[]) ArrayUtils.addAll(super.getHtmlAttributes(), htmlView.properties());

	}
}
