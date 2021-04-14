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
package org.structr.core.property;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import org.structr.api.Predicate;
import org.structr.api.search.SortType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.event.RuntimeUsageLog;
import org.structr.common.event.Usage;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaProperty;

/**
 */
public class UsageProperty extends AbstractReadOnlyProperty<Object> {

	private Class forType = null;

	public UsageProperty(final String name, final Class forType) {

		super(name);

		this.forType = forType;
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

		if (AbstractSchemaNode.class.equals(this.forType)) {

			final Map<String, Set<Usage>> data = getSchemaTypeUsage(obj);
			if (data != null) {

				final Map<String, Object> result = new LinkedHashMap<>();
				final Map<String, Object> tmp    = new LinkedHashMap<>();

				for (final Entry<String, Set<Usage>> entry : data.entrySet()) {

					tmp.put(entry.getKey(), resolve(securityContext, entry.getValue()));
				}

				// reverse result
				for (final Entry<String, Object> entry : tmp.entrySet()) {

					final List<Map<String, Object>> list = (List)entry.getValue();
					final String propertyKey             = entry.getKey();

					for (final Map<String, Object> usage : list) {

						final String uuid = (String)usage.get("id");

						Map<String, Object> mapped = (Map)result.get(uuid);
						if (mapped == null) {

							mapped = new LinkedHashMap<>(usage);
							result.put(uuid, mapped);
						}

						Map<String, String> properties = (Map)mapped.get("mapped");
						if (properties == null) {

							properties = new TreeMap<>();
							mapped.put("mapped", properties);
						}

						properties.put((String)usage.get("key"), propertyKey);

						mapped.remove("key");
					}
				}

				return result.values();
			}
		}

		if (SchemaProperty.class.equals(this.forType)) {
			return resolve(securityContext, getSchemaPropertyUsage(obj));
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

	@Override
	public Object getExampleValue(final String type, final String viewName) {
		return null;
	}

	// ----- private methods -----
	private Map<String, Set<Usage>> getSchemaTypeUsage(final GraphObject obj) {

		final Map<String, Map<String, Set<Usage>>> data = RuntimeUsageLog.getData();

		if (obj instanceof AbstractSchemaNode) {

			final AbstractSchemaNode node = (AbstractSchemaNode)obj;

			return data.get(node.getProperty(AbstractNode.name));
		}

		return null;
	}

	private Set<Usage> getSchemaPropertyUsage(final GraphObject obj) {

		final Map<String, Map<String, Set<Usage>>> data = RuntimeUsageLog.getData();

		if (obj instanceof SchemaProperty) {

			final SchemaProperty property = (SchemaProperty)obj;
			final AbstractSchemaNode node = (AbstractSchemaNode)property.getProperty(SchemaProperty.schemaNode);

			final Map<String, Set<Usage>> details = data.get(node.getProperty(AbstractNode.name));
			if (details != null) {

				return details.get(property.getName());
			}
		}

		return null;
	}

	private List<Object> resolve(final SecurityContext securityContext, final Set<Usage> usages) {

		if (usages == null) {
			return null;
		}

		final List<Object> result = new LinkedList<>();

		for (final Usage usage : usages) {

			try {
				result.add(usage.convert(securityContext));

			} catch (FrameworkException fex) {
				fex.printStackTrace();
			}
		}

		return result;
	}
}
