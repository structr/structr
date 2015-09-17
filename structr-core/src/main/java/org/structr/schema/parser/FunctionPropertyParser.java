/**
 * Copyright (C) 2010-2015 Morgner UG (haftungsbeschränkt)
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
package org.structr.schema.parser;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.property.FunctionProperty;
import org.structr.schema.Schema;
import org.structr.schema.SchemaHelper.Type;

/**
 *
 * @author Christian Morgner
 */
public class FunctionPropertyParser extends PropertySourceGenerator {
	
	private static final Logger logger = Logger.getLogger(FunctionPropertyParser.class.getName());

	public FunctionPropertyParser(final ErrorBuffer errorBuffer, final String className, final PropertyDefinition params) {
		super(errorBuffer, className, params);
	}

	@Override
	public String getPropertyType() {
		return FunctionProperty.class.getSimpleName();
	}

	@Override
	public String getValueType() {
		return Object.class.getName();
	}

	@Override
	public String getUnqualifiedValueType() {
		return "Object";
	}

	@Override
	public String getPropertyParameters() {
		return "";
	}

	@Override
	public Type getKey() {
		return Type.Function;
	}

	@Override
	public void parseFormatString(final Schema entity, final String expression) throws FrameworkException {

		// Note: This is a temporary migration from the old format to the new readFunction property
		final String format = source.getFormat();

		if (format != null) {
			logger.log(Level.INFO, "Migrating format to readFunction");
			entity.getSchemaProperties().forEach(prop -> migrateFormat(prop, format));
		}
	}
	
	private void migrateFormat(final SchemaProperty prop, final String format) {
		
		if (getKey().equals(prop.getPropertyType())) {
		
			try {
				prop.setProperty(SchemaProperty.readFunction, format);
				prop.setProperty(SchemaProperty.format, null);
			} catch (FrameworkException ex) {
				logger.log(Level.WARNING, "Could not migrate format to readFunction", ex);
				ex.printStackTrace();
			}
		}
	}
}
