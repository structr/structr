/*
 * Copyright (C) 2010-2024 Structr GmbH
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
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.relationship.SchemaNodeView;
import org.structr.core.entity.relationship.SchemaViewProperty;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.notion.PropertySetNotion;
import org.structr.core.property.*;

/**
 *
 *
 */
public class SchemaView extends SchemaReloadingNode {

	public static final String schemaViewNamePattern    = "[a-zA-Z_][a-zA-Z0-9_]*";

	public static final Property<AbstractSchemaNode>   schemaNode             = new StartNode<>("schemaNode", SchemaNodeView.class, new PropertySetNotion(AbstractNode.id, AbstractNode.name, SchemaNode.isBuiltinType));
	public static final Property<Iterable<SchemaProperty>> schemaProperties   = new EndNodes<>("schemaProperties", SchemaViewProperty.class, new PropertySetNotion(AbstractNode.id, AbstractNode.name, SchemaProperty.isBuiltinProperty));
	public static final Property<Boolean>              isBuiltinView        = new BooleanProperty("isBuiltinView");
	public static final Property<String>               nonGraphProperties   = new StringProperty("nonGraphProperties");
	public static final Property<String>               sortOrder            = new StringProperty("sortOrder");

	public static final View defaultView = new View(SchemaProperty.class, PropertyView.Public,
		id, name, schemaNode, schemaProperties, nonGraphProperties
	);

	public static final View uiView = new View(SchemaProperty.class, PropertyView.Ui,
		id, name, schemaNode, schemaProperties, nonGraphProperties, isBuiltinView, sortOrder
	);

	public static final View schemaView = new View(SchemaView.class, "schema",
		id, name, schemaNode, schemaProperties, nonGraphProperties, isBuiltinView, sortOrder
	);

	public static final View exportView = new View(SchemaView.class, "export",
		id, type, name, schemaNode, nonGraphProperties, isBuiltinView, sortOrder
	);

	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {

		boolean valid = super.isValid(errorBuffer);

		valid &= ValidationHelper.isValidStringMatchingRegex(this, name, schemaViewNamePattern, errorBuffer);

		return valid;
	}

	@Override
	public boolean reloadSchemaOnCreate() {
		return true;
	}

	@Override
	public boolean reloadSchemaOnModify(final ModificationQueue modificationQueue) {
		return true;
	}

	@Override
	public boolean reloadSchemaOnDelete() {
		return true;
	}

	@Override
	public void onModification(SecurityContext securityContext, ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		super.onModification(securityContext, errorBuffer, modificationQueue);

		if (getProperty(schemaNode) == null) {
			StructrApp.getInstance().delete(this);
		}
	}
}
