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

import org.apache.lucene.search.SortField;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.property.AbstractReadOnlyProperty;
import org.structr.core.validator.PathUniquenessValidator;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.Linkable;

/**
 * A property which returns the complete folder path of a {@link Linkable}
 * including name. The path consists of the names of the parent elements,
 * concatenated by "/" as path separator.
 *
 *
 */
public class PathProperty extends AbstractReadOnlyProperty<String> {

	public PathProperty(String name) {
		super(name);
	}

	public PathProperty(String name, PathUniquenessValidator validator) {
		super(name);

		addValidator(validator);
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
	public String getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter, final org.neo4j.helpers.Predicate<GraphObject> predicate) {
		return FileHelper.getFolderPath((AbstractFile) obj);
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public Integer getSortType() {
		return SortField.STRING;
	}
}
