/**
 * Copyright (C) 2010-2017 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.entity;

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.relationship.SchemaNodeMethod;
import org.structr.core.notion.PropertySetNotion;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.schema.action.ActionEntry;

/**
 *
 *
 */
public class SchemaMethod extends SchemaReloadingNode implements Favoritable {

	public static final Property<AbstractSchemaNode> schemaNode      = new StartNode<>("schemaNode", SchemaNodeMethod.class, new PropertySetNotion(AbstractNode.id, AbstractNode.name));
	public static final Property<String>             virtualFileName = new StringProperty("virtualFileName");
	public static final Property<String>             source          = new StringProperty("source");
	public static final Property<String>             comment         = new StringProperty("comment");

	public static final View defaultView = new View(SchemaMethod.class, PropertyView.Public,
		name, schemaNode, source, comment, isFavoritable
	);

	public static final View uiView = new View(SchemaMethod.class, PropertyView.Ui,
		name, schemaNode, source, comment, isFavoritable
	);

	public static final View exportView = new View(SchemaMethod.class, "export",
		id, type, schemaNode, name, source, comment
	);

	public ActionEntry getActionEntry() {
		return new ActionEntry("___" + getProperty(AbstractNode.name), getProperty(SchemaMethod.source));
	}

	// ----- interface Favoritable -----
	@Override
	public String getContext() {

		final AbstractSchemaNode parent = getProperty(SchemaMethod.schemaNode);
		final StringBuilder buf = new StringBuilder();

		if (parent != null) {

			buf.append(parent.getProperty(SchemaNode.name));
			buf.append(".");
			buf.append(getProperty(name));
		}

		return buf.toString();
	}

	@Override
	public String getFavoriteContent() {
		return getProperty(SchemaMethod.source);
	}

	@Override
	public String getFavoriteContentType() {
		return "application/x-structr-javascript";
	}

	@Override
	public void setFavoriteContent(String content) throws FrameworkException {
		setProperty(SchemaMethod.source, content);
	}
}
