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
package org.structr.core.traits;

import org.structr.core.entity.SchemaProperty;
import org.structr.core.property.PropertyKey;

public class PropertyInfo {

	private final boolean isAbstract;
	private final String propertyName;
	private final String propertyType;
	private final String sourceType;

	public PropertyInfo(final SchemaProperty fromSchemaProperty) {

		this.isAbstract   = fromSchemaProperty.isAbstract();
		this.propertyName = fromSchemaProperty.getPropertyName();
		this.propertyType = fromSchemaProperty.getPropertyType().name();
		this.sourceType   = fromSchemaProperty.getSchemaNode().getClassName();
	}

	public PropertyInfo(final PropertyKey fromPropertyKey) {

		this.isAbstract   = fromPropertyKey.isAbstract();
		this.propertyName = fromPropertyKey.jsonName();
		this.sourceType   = fromPropertyKey.getDeclaringTrait().getLabel();

		final String simpleName = fromPropertyKey.getClass().getSimpleName();
		if (simpleName.endsWith("Property")) {

			this.propertyType = simpleName.substring(0, simpleName.length() - 8);

		} else {

			this.propertyType = simpleName;
		}
	}

	public boolean isAbstract() {
		return this.isAbstract;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public String getPropertyType() {
		return propertyType;
	}

	public boolean canOverride(final PropertyInfo other) {

		if (this.isAbstract || other.isAbstract) {
			return true;
		}

		if (this.propertyName.equals(other.propertyName)) {

			if (this.sourceType.equals(other.sourceType)) {

				return true;
			}
		}

		return false;
	}
}
