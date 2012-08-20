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



package org.structr.web.entity.relation;

import org.structr.common.PropertyKey;
import org.structr.common.RelType;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractRelationship;
import org.structr.web.entity.Content;
import org.structr.web.entity.html.HtmlElement;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedHashSet;
import java.util.Set;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class ContentRelationship extends AbstractRelationship {

	static {

		EntityContext.registerNamedRelation("data", ContentRelationship.class, HtmlElement.class, Content.class, RelType.CONTAINS);

		// not needed, overridden below
		// EntityContext.registerPropertySet(ContentRelationship.class, PropertyView.All,    ContentRelationship.Key.values());
		// EntityContext.registerPropertySet(ContentRelationship.class, PropertyView.Public, ContentRelationship.Key.values());

	}

	//~--- constant enums -------------------------------------------------

	public enum Key implements PropertyKey{ parentId, contentId, componentId, pageId, foo }

	//~--- get methods ----------------------------------------------------

	@Override
	public PropertyKey getStartNodeIdKey() {

		return Key.parentId;

	}

	@Override
	public PropertyKey getEndNodeIdKey() {

		return Key.contentId;

	}

	@Override
	public Iterable<String> getPropertyKeys(String propertyView) {

		Set<String> keys = new LinkedHashSet<String>();

		keys.add(Key.parentId.name());
		keys.add(Key.contentId.name());
		keys.add(Key.componentId.name());
		keys.add(Key.pageId.name());

		if (dbRelationship != null) {

			for (String key : dbRelationship.getPropertyKeys()) {

				keys.add(key);
			}

		}

		return keys;

	}

}
