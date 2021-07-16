/*
 * Copyright (C) 2010-2021 Structr GmbH
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
package org.structr.cmis.wrapper;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.commons.lang.StringUtils;

/**
 *
 *
 */
public class FilteredPropertyList {

	private final Set<String> acceptedProperties = new LinkedHashSet<>();
	private final List<PropertyData<?>> list     = new LinkedList<>();
	private boolean doFilter                     = false;

	public FilteredPropertyList(final String propertyFilter) {

		initialize(propertyFilter);

		if (StringUtils.isNotBlank(propertyFilter) && !"*".equals(propertyFilter)) {
			doFilter = true;
		}
	}

	public List<PropertyData<?>> getList() {
		return list;
	}

	public void add(final PropertyData<?> property) {

		if (property != null && (!doFilter || acceptedProperties.contains(property.getId()))) {

			list.add(property);
		}
	}

	private void initialize(final String source) {

		if (source != null) {

			for (final String part : source.split("[,]+")) {

				final String trimmedPart = part.trim();
				if (!trimmedPart.isEmpty()) {

					acceptedProperties.add(trimmedPart);
				}
			}
		}
	}
}
