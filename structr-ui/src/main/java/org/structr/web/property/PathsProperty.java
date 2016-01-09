/**
 * Copyright (C) 2010-2016 Structr GmbH
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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.property;

import java.util.Set;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.property.AbstractPrimitiveProperty;
import org.structr.web.converter.PathsConverter;

/**
 *
 *
 */
public class PathsProperty extends AbstractPrimitiveProperty<Set<String>> {

	public PathsProperty(String name) {
		super(name);
	}

	@Override
	public String typeName() {
		return ""; // read-only
	}

	@Override
	public Class valueType() {
		return String.class;
	}

	@Override
	public PropertyConverter<Set<String>, ?> databaseConverter(SecurityContext securityContext) {
		return new PathsConverter(securityContext);
	}

	@Override
	public PropertyConverter<Set<String>, ?> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return new PathsConverter(securityContext, entity);
	}

	@Override
	public PropertyConverter<?, Set<String>> inputConverter(SecurityContext securityContext) {
		return null;
	}

	@Override
	public Object fixDatabaseProperty(Object value) {
		return null;
	}

	@Override
	public Integer getSortType() {
		return null;
	}
}
