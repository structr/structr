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


package org.structr.web.entity.blog;

import java.util.Date;
import java.util.List;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.neo4j.graphdb.Direction;

import org.structr.common.PropertyView;
import org.structr.web.common.RelType;
import org.structr.core.entity.AbstractNode;

//~--- JDK imports ------------------------------------------------------------

import org.structr.core.property.EndNodes;
import org.structr.core.property.ISO8601DateProperty;
import org.structr.core.property.PropertyKey;
import org.structr.web.entity.dom.Content;

//~--- classes ----------------------------------------------------------------

/**
 * Entity bean to represent a blog post
 * 
 * @author Axel Morgner
 *
 */
public class Post extends AbstractNode {

	public static final Property<String>             title       = new StringProperty("title").indexed();
	public static final EndNodes<Content>  sections    = new EndNodes<Content>("sections", Content.class, RelType.CONTAINS, Direction.OUTGOING, true);
	public static final EndNodes<Comment>  comments    = new EndNodes<Comment>("comments", Comment.class, org.structr.web.common.RelType.COMMENT, Direction.OUTGOING, true);
	public static final Property<Date>               publishDate = new ISO8601DateProperty("publishDate").indexed();
	
	public static final org.structr.common.View uiView = new org.structr.common.View(Post.class, PropertyView.Ui,
		type, name, title, sections, publishDate, owner, comments
	);
	
	public static final org.structr.common.View publicView = new org.structr.common.View(Post.class, PropertyView.Public,
		type, name, title, sections, publishDate, owner, comments
	);

	//~--- get methods ----------------------------------------------------

	@Override
	public Object getPropertyForIndexing(final PropertyKey key) {

		if (key.equals(sections)) {
			
			StringBuilder buf = new StringBuilder();
			
			List<Content> _sections = getProperty(sections);
			for (Content section : _sections) {
				buf.append(section.getProperty(Content.content));
			}
			
			return buf.toString();
		}
		
		return super.getPropertyForIndexing(key);

	}

}
