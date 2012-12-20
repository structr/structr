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
import org.structr.core.property.Property;

import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.View;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.StringProperty;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Linkable;
import org.structr.core.entity.Relation;
import org.structr.core.entity.Relation.Cardinality;
import org.structr.core.notion.PropertyNotion;
import org.structr.core.property.CollectionProperty;
import org.structr.core.property.EntityIdProperty;
import org.structr.core.property.EntityProperty;
import org.structr.web.common.HtmlProperty;
import org.structr.web.entity.Content;

//~--- classes ----------------------------------------------------------------

/**
 * @author Axel Morgner
 */
public class A extends HtmlElement {

	public static final Property<String>            _href       = new HtmlProperty("href");
	public static final Property<String>            _target     = new HtmlProperty("target");
	public static final Property<String>            _ping       = new HtmlProperty("ping");
	public static final Property<String>            _rel        = new HtmlProperty("rel");
	public static final Property<String>            _media      = new HtmlProperty("media");
	public static final Property<String>            _hreflang   = new HtmlProperty("hreflang");
	public static final Property<String>            _type       = new HtmlProperty("type");
  
	public static final CollectionProperty<Content> contents    = new CollectionProperty<Content>("contents", Content.class, RelType.CONTAINS, false);
	public static final CollectionProperty<Span>    spans       = new CollectionProperty<Span>("spans", Span.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Img>     imgs        = new CollectionProperty<Img>("imgs", Img.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Div>     div         = new CollectionProperty<Div>("div", Div.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Section> sections    = new CollectionProperty<Section>("sections", Section.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<P>       ps          = new CollectionProperty<P>("ps", P.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<H1>      h1s         = new CollectionProperty<H1>("h1s", H1.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<H2>      h2s         = new CollectionProperty<H2>("h2s", H2.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<H3>      h3s         = new CollectionProperty<H3>("h3s", H3.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<H4>      h4s         = new CollectionProperty<H4>("h4s", H4.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<H5>      h5s         = new CollectionProperty<H5>("h5s", H5.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<H6>      h6s         = new CollectionProperty<H6>("h6s", H6.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Li>      lis         = new CollectionProperty<Li>("lis", Li.class, RelType.CONTAINS, Direction.INCOMING, false);
 
	public static final CollectionProperty<Div>     divParents  = new CollectionProperty<Div>("divParents", Div.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<P>       pParents    = new CollectionProperty<P>("pParents", P.class, RelType.CONTAINS, Direction.INCOMING, false);
 
	public static final EntityProperty<Linkable>    linkable    = new EntityProperty<Linkable>("linkable", Linkable.class, RelType.LINK, Direction.OUTGOING, new PropertyNotion(AbstractNode.name), true);
	public static final Property<String>            linkableId  = new EntityIdProperty("linkableId", linkable);

	public static final View uiView = new View(A.class, PropertyView.Ui,
		linkableId, linkable
	);
	
	public static final View htmlView = new View(A.class, PropertyView.Html,
		_href, _target, _ping, _rel, _media, _hreflang, _type
	);
	
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
