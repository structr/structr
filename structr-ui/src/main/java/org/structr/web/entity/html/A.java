/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.entity.html;

import org.apache.commons.lang3.ArrayUtils;
import org.structr.core.property.Property;

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.web.common.HtmlProperty;
import org.structr.web.entity.LinkSource;

//~--- classes ----------------------------------------------------------------

/**
 *
 */
public class A extends LinkSource {

	public static final Property<String>            _href       = new HtmlProperty("href");
	public static final Property<String>            _target     = new HtmlProperty("target");
	public static final Property<String>            _ping       = new HtmlProperty("ping");
	public static final Property<String>            _rel        = new HtmlProperty("rel");
	public static final Property<String>            _media      = new HtmlProperty("media");
	public static final Property<String>            _hreflang   = new HtmlProperty("hreflang");
	public static final Property<String>            _type       = new HtmlProperty("type");
  
//	public static final Property<List<Content>> contents    = new EndNodes<>("contents", Content.class, RelType.CONTAINS, false);
//	public static final Property<List<Span>>    spans       = new EndNodes<>("spans", Span.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final Property<List<Img>>     imgs        = new EndNodes<>("imgs", Img.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final Property<List<Div>>     div         = new EndNodes<>("div", Div.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final Property<List<Section>> sections    = new EndNodes<>("sections", Section.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final Property<List<P> >     ps          = new EndNodes<>("ps", P.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final Property<List<H1>>     h1s         = new EndNodes<>("h1s", H1.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final Property<List<H2>>     h2s         = new EndNodes<>("h2s", H2.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final Property<List<H3>>     h3s         = new EndNodes<>("h3s", H3.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final Property<List<H4>>     h4s         = new EndNodes<>("h4s", H4.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final Property<List<H5>>     h5s         = new EndNodes<>("h5s", H5.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final Property<List<H6>>     h6s         = new EndNodes<>("h6s", H6.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final Property<List<Li>>      lis         = new EndNodes<>("lis", Li.class, RelType.CONTAINS, Direction.INCOMING, false);
// 
//	public static final EndNodes<Div>     divParents  = new EndNodes<Div>("divParents", Div.class, RelType.CONTAINS, Direction.INCOMING, false);
//	public static final EndNodes<P>       pParents    = new EndNodes<P>("pParents", P.class, RelType.CONTAINS, Direction.INCOMING, false);
// 

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
