/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.schema.export;

import org.structr.api.schema.JsonStringProperty;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.SchemaPropertyTraitDefinition;
import org.structr.schema.SchemaHelper.Type;

/**
 *
 *
 */
public class StructrPasswordProperty extends StructrPropertyDefinition implements JsonStringProperty {

	public StructrPasswordProperty(final StructrTypeDefinition parent, final String name) {
		super(parent, name);
	}

	@Override
	public String getType() {
		return "password";
	}

	@Override
	SchemaProperty createDatabaseSchema(final App app, final AbstractSchemaNode schemaNode) throws FrameworkException {

		final SchemaProperty property = super.createDatabaseSchema(app, schemaNode);
		final Traits traits           = Traits.of(StructrTraits.SCHEMA_PROPERTY);
		final PropertyMap properties  = new PropertyMap();

		properties.put(traits.key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), Type.Password.name());
		properties.put(traits.key(SchemaPropertyTraitDefinition.FORMAT_PROPERTY), getFormat());
		properties.put(traits.key(SchemaPropertyTraitDefinition.CONTENT_TYPE_PROPERTY), getContentType());

		property.setProperties(SecurityContext.getSuperUserInstance(), properties);

		return property;
	}

	@Override
	public JsonStringProperty setContentType(final String contentType) {
		return this;
	}

	@Override
	public String getContentType() {
		return "text/plain";
	}
}
