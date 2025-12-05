/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.structr.core.property.Property;
import org.structr.schema.SchemaHelper;
import org.structr.schema.SchemaHelper.Type;
import org.structr.schema.parser.PropertyDefinition;
import org.structr.schema.parser.PropertyGenerator;
import org.structr.web.entity.Image;
import org.structr.web.property.ThumbnailProperty;

/**
 *
 *
 */
public class ThumbnailPropertySourceGenerator extends PropertyGenerator {

	static {

		SchemaHelper.generatorMap.put(Type.Thumbnail, (e, t, p) -> new ThumbnailPropertySourceGenerator(e, t, p));
	}

	public ThumbnailPropertySourceGenerator(final ErrorBuffer errorBuffer, final String className, final PropertyDefinition params) {
		super(errorBuffer, className, params);
	}

	@Override
	public String getValueType() {
		return Image.class.getName();
	}

	@Override
	protected Object getDefaultValue() {
		return null;
	}

	@Override
	protected Property newInstance() throws FrameworkException {
		return new ThumbnailProperty(source.getPropertyName()).format(source.getFormat());
	}

	@Override
	public Type getPropertyType() {
		return Type.Thumbnail;
	}
}
