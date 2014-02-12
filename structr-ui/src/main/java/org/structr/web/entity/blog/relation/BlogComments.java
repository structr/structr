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
package org.structr.web.entity.blog.relation;

import org.structr.core.property.PropertyKey;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedHashSet;
import java.util.Set;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.OneToMany;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.web.entity.dom.DOMElement;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class BlogComments extends OneToMany<DOMElement, BlogComment> {

	public static final Property<String> parentId    = new StringProperty("parentId");
	public static final Property<String> contentId   = new StringProperty("contentId");
	public static final Property<String> componentId = new StringProperty("componentId");
	public static final Property<String> pageId      = new StringProperty("pageId");
	public static final Property<String> foo         = new StringProperty("foo");

	static {

		// StructrApp.getConfiguration().registerNamedRelation("data", ContentRelationship.class, DOMElement.class, Content.class, RelType.CONTAINS);

		// not needed, overridden below
		// StructrApp.getConfiguration().registerPropertySet(ContentRelationship.class, PropertyView.All,    ContentRelationship.Key.values());
		// StructrApp.getConfiguration().registerPropertySet(ContentRelationship.class, PropertyView.Public, ContentRelationship.Key.values());

	}

	@Override
	public Iterable<PropertyKey> getPropertyKeys(String propertyView) {

		Set<PropertyKey> keys = new LinkedHashSet<>();

		keys.add(parentId);
		keys.add(contentId);
		keys.add(componentId);
		keys.add(pageId);

		if (dbRelationship != null) {

			for (String key : dbRelationship.getPropertyKeys()) {

				keys.add(StructrApp.getConfiguration().getPropertyKeyForDatabaseName(entityType, key));
			}

		}

		return keys;

	}

	@Override
	public Class<DOMElement> getSourceType() {
		return DOMElement.class;
	}

	@Override
	public Class<BlogComment> getTargetType() {
		return BlogComment.class;
	}

	@Override
	public String name() {
		return "COMMENT";
	}

	@Override
	public Property<String> getSourceIdProperty() {
		return parentId;
	}

	@Override
	public Property<String> getTargetIdProperty() {
		return contentId;
	}

}
