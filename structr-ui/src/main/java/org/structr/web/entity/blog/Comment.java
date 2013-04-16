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
import org.structr.web.entity.*;
import org.structr.core.property.Property;
import org.neo4j.graphdb.Direction;

import org.structr.common.PropertyView;
import org.structr.web.common.RelType;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeService.NodeIndex;

//~--- JDK imports ------------------------------------------------------------

import org.structr.core.entity.Person;
import org.structr.core.property.EntityProperty;
import org.structr.core.property.ISO8601DateProperty;
import org.structr.web.entity.dom.Content;

//~--- classes ----------------------------------------------------------------

/**
 * Entity bean to represent a blog comment
 * 
 * @author Axel Morgner
 *
 */
public class Comment extends AbstractNode {

	public static final EntityProperty<Content>          text = new EntityProperty<Content>("contents", Content.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Property<Date>            publishDate = new ISO8601DateProperty("publishDate");
	public static final EntityProperty<Person>         author = new EntityProperty<Person>("owner", Person.class, org.structr.web.common.RelType.AUTHOR, Direction.INCOMING, true);
	
	public static final org.structr.common.View uiView = new org.structr.common.View(Comment.class, PropertyView.Ui,
		type, name, publishDate, author
	);
	
	public static final org.structr.common.View publicView = new org.structr.common.View(User.class, PropertyView.Public,
		type, name, publishDate, author
	);
	
	static {

		EntityContext.registerSearchablePropertySet(User.class, NodeIndex.fulltext.name(), uiView.properties());
		EntityContext.registerSearchablePropertySet(User.class, NodeIndex.keyword.name(),  uiView.properties());
	}

	//~--- get methods ----------------------------------------------------

}
