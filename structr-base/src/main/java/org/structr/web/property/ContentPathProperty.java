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
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.ContentContainer;

import java.util.Map;

/**
 * A property which returns the complete folder path of a content container or item
 * including name. The path consists of the names of the parent elements,
 * concatenated by "/" as path separator.
 *
 *
 */
public class ContentPathProperty extends AbstractReadOnlyProperty<String> {

	public ContentPathProperty(String name) {
		super(name);
	}

	@Override
	public Class relatedType() {
		return null;
	}

	@Override
	public Class valueType() {
		return String.class;
	}

	@Override
	public String typeName() {
		return "String";
	}

	@Override
	public String getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		return getProperty(securityContext, obj, applyConverter, null);
	}

	@Override
	public String getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter, final Predicate<GraphObject> predicate) {

		ContentContainer parentContainer = ((ContentContainer)obj).getParent();
		String containerPath             = obj.getProperty(AbstractFile.name);

		if (containerPath == null) {
			containerPath = obj.getProperty(GraphObject.id);
		}

		while (parentContainer != null && !parentContainer.equals(obj) && !parentContainer.equals(parentContainer.getParent())) {

			containerPath = parentContainer.getName().concat("/").concat(containerPath);
			parentContainer = parentContainer.getParent();
		}

		return "/".concat(containerPath);
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public SortType getSortType() {
		return SortType.Integer;
	}

	// ----- OpenAPI -----
	@Override
	public Object getExampleValue(final String type, final String viewName) {
		return null;
	}

	@Override
	public Map<String, Object> describeOpenAPIOutputSchema(String type, String viewName) {
		return null;
	}
}
