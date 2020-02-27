/**
 * Copyright (C) 2010-2020 Structr GmbH
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

import java.lang.reflect.Method;
import org.apache.commons.lang3.StringUtils;
import org.structr.api.Predicate;
import org.structr.api.search.SortType;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.property.AbstractReadOnlyProperty;

/**
 */
public class MethodProperty extends AbstractReadOnlyProperty<Object> {

	private Method method = null;

	public MethodProperty(final String name) {
		super(name);
	}

	@Override
	public Class valueType() {
		return Object.class;
	}

	@Override
	public Class relatedType() {
		return null;
	}

	@Override
	public Object getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		return getProperty(securityContext, obj, applyConverter, null);
	}

	@Override
	public Object getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter, Predicate<GraphObject> predicate) {

		if (method == null) {

			loadMethod();
		}

		if (method != null) {

			try {

				return method.invoke(obj);

			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

		return null;
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public SortType getSortType() {
		return SortType.Default;
	}

	// ----- private methods -----
	private void loadMethod() {

		// load method with information from format string
		if (StringUtils.isNotBlank(this.format)) {

			final String[] parts = this.format.split(",");
			if (parts.length == 2) {

				final String fqcn       = parts[0].trim();
				final String methodName = parts[1].trim();

				try {

					final Class type = Class.forName(fqcn);
					this.method      = type.getMethod(methodName);

				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		}
	}
}
