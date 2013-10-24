/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.web.entity.html;

import org.structr.web.entity.dom.DOMElement;
import org.apache.commons.lang.ArrayUtils;
import org.neo4j.graphdb.Direction;
import org.structr.core.property.Property;

import org.structr.common.PropertyView;
import org.structr.web.common.RelType;
import org.structr.common.View;
import org.structr.core.entity.AbstractNode;
import org.structr.web.entity.Linkable;
import org.structr.core.notion.PropertyNotion;
import org.structr.core.property.Endpoints;
import org.structr.core.property.EntityIdProperty;
import org.structr.core.property.End;
import org.structr.web.common.HtmlProperty;
import org.structr.web.entity.dom.Content;

//~--- classes ----------------------------------------------------------------

/**
 * @author Axel Morgner
 */
public class A extends DOMElement {

	public static final Property<String>            _href       = new HtmlProperty("href");
	public static final Property<String>            _target     = new HtmlProperty("target");
	public static final Property<String>            _ping       = new HtmlProperty("ping");
	public static final Property<String>            _rel        = new HtmlProperty("rel");
	public static final Property<String>            _media      = new HtmlProperty("media");
	public static final Property<String>            _hreflang   = new HtmlProperty("hreflang");
	public static final Property<String>            _type       = new HtmlProperty("type");
  
	public static final Endpoints<Content> contents    = new Endpoints<Content>("contents", Content.class, RelType.CONTAINS, false);
	public static final Endpoints<Span>    spans       = new Endpoints<Span>("spans", Span.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Img>     imgs        = new Endpoints<Img>("imgs", Img.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Div>     div         = new Endpoints<Div>("div", Div.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Section> sections    = new Endpoints<Section>("sections", Section.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<P>       ps          = new Endpoints<P>("ps", P.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<H1>      h1s         = new Endpoints<H1>("h1s", H1.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<H2>      h2s         = new Endpoints<H2>("h2s", H2.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<H3>      h3s         = new Endpoints<H3>("h3s", H3.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<H4>      h4s         = new Endpoints<H4>("h4s", H4.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<H5>      h5s         = new Endpoints<H5>("h5s", H5.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<H6>      h6s         = new Endpoints<H6>("h6s", H6.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Li>      lis         = new Endpoints<Li>("lis", Li.class, RelType.CONTAINS, Direction.INCOMING, false);
 
	public static final Endpoints<Div>     divParents  = new Endpoints<Div>("divParents", Div.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final Endpoints<P>       pParents    = new Endpoints<P>("pParents", P.class, RelType.CONTAINS, Direction.INCOMING, false);
 
	public static final End<Linkable>    linkable    = new End<Linkable>("linkable", Linkable.class, RelType.LINK, Direction.OUTGOING, new PropertyNotion(AbstractNode.name), true);
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
