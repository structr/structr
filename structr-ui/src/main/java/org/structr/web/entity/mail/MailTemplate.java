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


package org.structr.web.entity.mail;

import org.structr.web.entity.news.*;
import org.structr.core.property.Property;
import org.neo4j.graphdb.Direction;

import org.structr.common.PropertyView;
import org.structr.web.common.RelType;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeService.NodeIndex;
import org.structr.core.notion.PropertySetNotion;

//~--- JDK imports ------------------------------------------------------------

import org.structr.core.property.EntityProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.core.validator.TypeUniquenessValidator;
import org.structr.web.entity.dom.Content;

//~--- classes ----------------------------------------------------------------

/**
 * Entity bean to represent a template with placeholders, to be used for sending e-mails
 * 
 * @author Axel Morgner
 *
 */
public class MailTemplate extends AbstractNode {

	public static final EntityProperty<Content>          text = new EntityProperty<Content>("text", Content.class, RelType.CONTAINS, Direction.OUTGOING, new PropertySetNotion(uuid, name), false);
	public static final Property<String>               locale = new StringProperty("locale");
	
	public static final org.structr.common.View uiView = new org.structr.common.View(NewsTickerItem.class, PropertyView.Ui,
		type, name, text
	);
	
	public static final org.structr.common.View publicView = new org.structr.common.View(NewsTickerItem.class, PropertyView.Public,
		type, name, text
	);
	
	static {

		EntityContext.registerSearchablePropertySet(MailTemplate.class, NodeIndex.fulltext.name(), uiView.properties());
		EntityContext.registerSearchablePropertySet(MailTemplate.class, NodeIndex.keyword.name(),  uiView.properties());
		EntityContext.registerPropertyValidator(MailTemplate.class, name, new TypeUniquenessValidator(MailTemplate.class));
	}

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
