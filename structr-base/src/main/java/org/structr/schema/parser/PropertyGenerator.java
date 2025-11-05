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
package org.structr.schema.parser;

import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.ErrorToken;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.schema.SchemaHelper.Type;

import java.util.LinkedList;
import java.util.List;

/**
 *
 *
 */
public abstract class PropertyGenerator<T> {

	protected PropertyDefinition source = null;
	private ErrorBuffer errorBuffer     = null;
	protected String className          = null;

	public abstract Type getPropertyType();
	public abstract String getValueType();
	protected abstract T getDefaultValue();
	protected abstract Property<T> newInstance() throws FrameworkException;

	public PropertyGenerator(final ErrorBuffer errorBuffer, final String className, final PropertyDefinition propertyDefinition) {

		this.errorBuffer = errorBuffer;
		this.className   = className;
		this.source      = propertyDefinition;
	}

	public IsValid getValidator() {
		return null;
	}

	public List<IsValid> getValidators(final String key) throws FrameworkException {

		final List<IsValid> validators = new LinkedList<>();

		if (source.isNotNull()) {

			validators.add(new IsValid() {

				@Override
				public Boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {
					return ValidationHelper.isValidPropertyNotNull(obj, obj.getTraits().key(key), errorBuffer);
				}
			});
		}

		if (source.isUnique()) {

			validators.add(new IsValid() {

				@Override
				public Boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {
					return ValidationHelper.isValidUniqueProperty(obj, obj.getTraits().key(key), errorBuffer);
				}
			});
		}

		return validators;
	}

	public String getClassName() {
		return className;
	}

	public void reportError(final ErrorToken error) {
		errorBuffer.add(error);
	}

	public ErrorBuffer getErrorBuffer() {
		return errorBuffer;
	}

	public PropertyKey createKey() {

		final Property propertyKey;
		try {
			propertyKey = newInstance();

		} catch (FrameworkException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		// no property was created
		if (propertyKey == null) {
			return null;
		}

		if (StringUtils.isNotBlank(source.getDbName())) {
			propertyKey.dbName(source.getDbName());
		}

		propertyKey.dynamic();

		if (StringUtils.isNotBlank(source.getDefaultValue())) {
			propertyKey.defaultValue(getDefaultValue());
		}

		if (StringUtils.isNotBlank(source.getFormat())) {
			propertyKey.format(source.getFormat());
		}

		if (StringUtils.isNotBlank(source.getReadFunction())) {
			propertyKey.readFunction(source.getReadFunction());
		}

		if (StringUtils.isNotBlank(source.getWriteFunction())) {
			propertyKey.writeFunction(source.getWriteFunction());
		}

		propertyKey.readFunctionWrapJS(source.getReadFunctionWrapJS());

		propertyKey.writeFunctionWrapJS(source.getWriteFunctionWrapJS());

		if (source.isSerializationDisabled()) {
			propertyKey.disableSerialization(source.isSerializationDisabled());
		}

		if (source.isAbstract()) {
			propertyKey.setIsAbstract(true);
		}

		if (StringUtils.isNotBlank(source.getTypeHint())) {
			propertyKey.typeHint(source.getTypeHint());
		}

		if (source.isUnique()) {
			propertyKey.unique(true);
		}

		if (source.isCompound()) {
			propertyKey.compound();
		}

		if (source.isNotNull()) {
			propertyKey.notNull(true);
		}

		if (source.isCachingEnabled()) {
			propertyKey.cachingEnabled(true);
		}

		if (source.isIndexed()) {

			if (StringUtils.isNotBlank(source.getDefaultValue())) {

				propertyKey.indexedWhenEmpty();

			} else {

				propertyKey.indexed();
			}
		}

		if (source.isFulltext()) {

			propertyKey.fulltextIndexed();
		}

		if (source.isReadOnly()) {
			propertyKey.readOnly();
		}

		/*
		final String[] transformators = source.getTransformators();
		if (transformators != null && transformators.length > 0) {

			propertyKey.transformators(");
			line.quoted(StringUtils.join(transformators, "\", \""));
			line.append(")");
		}
		*/

		if (StringUtils.isNotBlank(source.getHint())) {
			propertyKey.hint(source.getHint());
		}

		if (StringUtils.isNotBlank(source.getCategory())) {
			propertyKey.category(source.getCategory());
		}

		return propertyKey;
	}
}
