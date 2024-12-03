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
package org.structr.core.property;

import org.apache.commons.lang.StringUtils;
import org.structr.api.Predicate;
import org.structr.api.search.SortType;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.graph.NodeInterface;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * A read-only property that returns the concatenated values of two other properties.
 *
 *
 */
public class ConcatProperty extends AbstractReadOnlyProperty<String> {

	private Set<String> propertyKeys = null;
	private String separator = null;

	public ConcatProperty(String name, String separator, String... propertyKeys) {

		super(name);

		this.propertyKeys.addAll(Arrays.asList(propertyKeys));
		this.separator = separator;
	}

	@Override
	public String getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		return getProperty(securityContext, obj, applyConverter, null);
	}

	@Override
	public String getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter, final Predicate<NodeInterface> predicate) {
		return StringUtils.join(propertyKeys.stream().map(k -> obj.getProperty(k)).toList(), separator);
	}

	@Override
	public String relatedType() {
		return null;
	}

	@Override
	public String valueType() {
		return "String";
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public SortType getSortType() {
		return SortType.Default;
	}

	// ----- OpenAPI -----
	@Override
	public Object getExampleValue(final String type, final String viewName) {
		return "concatenated string";
	}

	@Override
	public Map<String, Object> describeOpenAPIOutputSchema(String type, String viewName) {
		return null;
	}

	// ----- OpenAPI -----
	@Override
	public Map<String, Object> describeOpenAPIOutputType(final String type, final String viewName, final int level) {

		final Map<String, Object> map = new TreeMap<>();
		final String valueType         = valueType();

		if (valueType != null) {

			map.put("type",    valueType.toLowerCase());
			map.put("example", getExampleValue(type, viewName));

			if (this.readOnly) {
				map.put("readOnly", true);
			}
		}

		return map;
	}

	@Override
	public Map<String, Object> describeOpenAPIInputType(final String type, final String viewName, final int level) {

		final Map<String, Object> map = new TreeMap<>();
		final String valueType        = valueType();

		if (valueType != null) {

			map.put("type", valueType.toLowerCase());
			map.put("readOnly",    true);
			map.put("example", getExampleValue(type, viewName));
		}

		return map;
	}
}
