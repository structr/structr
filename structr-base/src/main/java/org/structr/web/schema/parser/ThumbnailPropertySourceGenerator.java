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
package org.structr.web.schema.parser;

import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaNode;
import org.structr.schema.SchemaHelper;
import org.structr.schema.SchemaHelper.Type;
import org.structr.schema.parser.PropertyDefinition;
import org.structr.schema.parser.PropertySourceGenerator;
import org.structr.web.entity.Image;
import org.structr.web.property.ThumbnailProperty;

import java.util.Map;

/**
 *
 *
 */
public class ThumbnailPropertySourceGenerator extends PropertySourceGenerator {

	static {

		SchemaHelper.parserMap.put(Type.Thumbnail, ThumbnailPropertySourceGenerator.class);
	}

	public ThumbnailPropertySourceGenerator(final ErrorBuffer errorBuffer, final String className, final PropertyDefinition params) {
		super(errorBuffer, className, params);
	}

	@Override
	public String getPropertyType() {
		return ThumbnailProperty.class.getSimpleName();
	}

	@Override
	public String getValueType() {
		return Image.class.getName();
	}

	@Override
	public String getUnqualifiedValueType() {
		return "Thumbnail";
	}

	@Override
	public String getPropertyParameters() {
		return "";
	}

	@Override
	public Type getKey() {
		return Type.Thumbnail;
	}

	@Override
	public void parseFormatString(final Map<String, SchemaNode> schemaNodes, final AbstractSchemaNode entity, final String expression) throws FrameworkException {
	}
}
