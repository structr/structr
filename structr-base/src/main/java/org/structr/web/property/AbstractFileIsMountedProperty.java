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
package org.structr.web.property;

import org.structr.api.Predicate;
import org.structr.api.search.SortType;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.property.AbstractReadOnlyProperty;
import org.structr.core.traits.StructrTraits;
import org.structr.web.entity.AbstractFile;

import java.util.Map;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;

public class AbstractFileIsMountedProperty extends AbstractReadOnlyProperty<Boolean> {

	public AbstractFileIsMountedProperty() {
		super(AbstractFileTraitDefinition.IS_MOUNTED_PROPERTY);
	}

	@Override
	public Class valueType() {
		return Boolean.class;
	}

	@Override
	public String relatedType() {
		return null;
	}

	@Override
	public Boolean getProperty(final SecurityContext securityContext, final GraphObject obj, final boolean applyConverter) {
		return getProperty(securityContext, obj, applyConverter, null);
	}

	@Override
	public Boolean getProperty(final SecurityContext securityContext, final GraphObject obj, final boolean applyConverter, final Predicate<GraphObject> predicate) {

		if (obj != null && obj.is(StructrTraits.ABSTRACT_FILE)){

			return obj.as(AbstractFile.class).isMounted();
		}

		return false;
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public boolean isArray() {
		return false;
	}

	@Override
	public SortType getSortType() {
		return SortType.Default;
	}

	@Override
	public Object getExampleValue(String type, String viewName) {
		return null;
	}

	@Override
	public Map<String, Object> describeOpenAPIOutputSchema(String type, String viewName) {
		return Map.of();
	}
}
