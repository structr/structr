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
package org.structr.schema.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.Property;
import org.structr.schema.SchemaHelper.Type;

import java.lang.reflect.Constructor;

/**
 *
 */
public class CustomPropertyParser extends PropertyGenerator {

	private static final Logger logger = LoggerFactory.getLogger(CustomPropertyParser.class);

	private String propertyType         = null;
	private String valueType            = null;
	private String unqualifiedValueType = null;
	private String propertyParameters   = null;

	public CustomPropertyParser(final ErrorBuffer errorBuffer, final String className, final PropertyDefinition params) {
		super(errorBuffer, className, params);

		final String fqcn = params.getFqcn();
		if (fqcn != null) {

			boolean initialized = false;

			try {

				// instantiate class and initialize values
				final Class<Property> type = (Class<Property>)Class.forName(fqcn);
				if (type != null) {

					try {

						final Constructor<Property> constr = type.getConstructor(String.class);
						if (constr != null) {

							final Property property = constr.newInstance(params.getPropertyName());
							if (property != null) {

								this.propertyType         = type.getSimpleName();
								this.valueType            = property.valueType().getName();
								this.unqualifiedValueType = type.getSimpleName();

								initialized = true;
							}
						}

					} catch (Throwable ignore) {}

					if (!initialized) {

						try {

							final Constructor<Property> specialConstructor = type.getConstructor(String.class, String.class);
							if (specialConstructor != null) {

								final Property property = specialConstructor.newInstance(params.getPropertyName(), params.getFormat());
								if (property != null) {

									this.propertyType         = type.getSimpleName();
									this.valueType            = property.valueType().getName();
									this.unqualifiedValueType = type.getSimpleName();
									this.propertyParameters   = ", " + params.getFormat() + ".class";
								}
							}

						} catch (Throwable ignore) {}
					}
				}

			} catch (Throwable ignore) {

				logger.warn("Unable to instantiate {}: {}", fqcn, ignore.getMessage());
			}
		}
	}

	@Override
	public String getValueType() {
		return valueType;
	}

	@Override
	protected Object getDefaultValue() {
		return null;
	}

	@Override
	protected Property newInstance() throws FrameworkException {
		return null;
	}

	@Override
	public Type getKey() {
		return Type.Custom;
	}
}
