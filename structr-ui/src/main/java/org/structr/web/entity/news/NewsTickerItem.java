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


package org.structr.web.entity.news;

import java.util.Date;
import org.structr.core.property.Property;
import org.neo4j.graphdb.Direction;

import org.structr.common.PropertyView;
import org.structr.web.common.RelType;
import org.structr.core.entity.AbstractNode;

//~--- JDK imports ------------------------------------------------------------

import org.structr.core.entity.Principal;
import org.structr.core.notion.PropertySetNotion;
import org.structr.core.property.Forward;
import org.structr.core.property.ISO8601DateProperty;
import org.structr.core.property.PropertyKey;
import org.structr.web.entity.dom.Content;

//~--- classes ----------------------------------------------------------------

/**
 * Entity bean to represent a short news item, typically used in a news ticker
 * 
 * @author Axel Morgner
 *
 */
public class NewsTickerItem extends AbstractNode {

	public static final Forward<Content>	text		= new Forward("text", Content.class, RelType.CONTAINS, Direction.OUTGOING, new PropertySetNotion(true, uuid, Content.content), false);
	public static final Property<Date>		publishDate	= new ISO8601DateProperty("publishDate").indexed();
	public static final Forward<Principal>   author		= new Forward("author", Principal.class, org.structr.web.common.RelType.AUTHOR, Direction.INCOMING, true);
	
	public static final org.structr.common.View uiView = new org.structr.common.View(NewsTickerItem.class, PropertyView.Ui,
		type, name, publishDate, author, text
	);
	
	public static final org.structr.common.View publicView = new org.structr.common.View(NewsTickerItem.class, PropertyView.Public,
		type, name, publishDate, author, text
	);

	//~--- get methods ----------------------------------------------------

	@Override
	public Object getPropertyForIndexing(final PropertyKey key) {

		if (key.equals(text)) {
			
			Content content = getProperty(text);
			
			if (content != null) {
				return content.getProperty(Content.content);
			}
		}
		
		return super.getPropertyForIndexing(key);

	}

}
