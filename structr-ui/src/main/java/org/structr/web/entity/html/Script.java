/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.entity.html;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.property.Property;
import org.apache.commons.lang.ArrayUtils;

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.web.entity.Linkable;
import org.structr.core.notion.PropertyNotion;
import org.structr.core.property.EndNode;
import org.structr.core.property.EntityIdProperty;
import org.structr.web.common.HtmlProperty;
import org.structr.web.entity.LinkSource;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.html.relation.ResourceLink;
import org.w3c.dom.Node;

//~--- classes ----------------------------------------------------------------

/**
 * @author Axel Morgner
 */
public class Script extends LinkSource {

	private static final Logger logger = Logger.getLogger(Script.class.getName());
	
	public static final Property<String> _src     = new HtmlProperty("src");
	public static final Property<String> _async   = new HtmlProperty("async");
	public static final Property<String> _defer   = new HtmlProperty("defer");
	public static final Property<String> _type    = new HtmlProperty("type");
	public static final Property<String> _charset = new HtmlProperty("charset");
		
//	public static final EndNodes<Content> contents = new EndNodes<Content>("contents", Content.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Head>    heads    = new EndNodes<Head>("heads", Head.class, RelType.CONTAINS, Direction.INCOMING, false);
//	public static final EndNodes<Div>     divs     = new EndNodes<Div>("divs", Div.class, RelType.CONTAINS, Direction.INCOMING, false);
 
	public static final Property<Linkable> linkable   = new EndNode<>("linkable", ResourceLink.class, new PropertyNotion(AbstractNode.name));
	public static final Property<String>   linkableId = new EntityIdProperty("linkableId", linkable);

	public static final View uiView = new View(Script.class, PropertyView.Ui,
		linkableId, linkable
	);

	public static final View htmlView = new View(Script.class, PropertyView.Html,
		_src, _async, _defer, _type, _charset
	);

	//~--- get methods ----------------------------------------------------

	@Override
	public Property[] getHtmlAttributes() {

		return (Property[]) ArrayUtils.addAll(super.getHtmlAttributes(), htmlView.properties());

	}
	
	@Override
	protected void handleNewChild(final Node newChild) {
		
		if (newChild instanceof Content) {

			final App app = StructrApp.getInstance(securityContext);
			try {
				app.beginTx();
				((Content)newChild).setProperty(Content.contentType, getProperty(_type));
				app.commitTx();
				
			} catch (FrameworkException fex) {
				
				logger.log(Level.WARNING, "Unable to set property on new child: {0}", fex.getMessage());
				
			} finally {
				
				app.finishTx();
			}
		}
	}
}
