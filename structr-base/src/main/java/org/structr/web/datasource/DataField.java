/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.web.datasource;

import org.structr.common.error.FrameworkException;
import org.structr.core.datasources.Channel;
import org.structr.core.entity.DataSource;
import org.structr.web.common.RenderContext;

import java.util.*;

public class DataField {

	private final Map<String, Object> options = new LinkedHashMap<>();
	private final Map<String, Object> config  = new LinkedHashMap<>();
	private final Set<String> slots           = new LinkedHashSet<>();

	private final String label;
	private final String name;
	private final String value;
	private final String template;
	private final String editTemplate;
	private final String propertyName;
	private final Boolean showLabel;

	public DataField(final String name, final String label, final String value, final String template, final String editTemplate, final String propertyName, final Boolean showLabel) {

		this.name         = name;
		this.label        = label;
		this.value        = value;
		this.template     = template;
		this.editTemplate = editTemplate;
		this.propertyName = propertyName;
		this.showLabel    = showLabel;
	}

	public String getName() {
		return name;
	}

	public String getLabel() {
		return label;
	}

	public String getValue() {
		return value;
	}

	public String getTemplate() {
		return template;
	}

	public String getEditTemplate() {
		return editTemplate;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public Set<String> getSlots() {
		return slots;
	}

	public Map<String, Object> getOptions() {
		return options;
	}

	public Map<String, Object> getConfig() {
		return config;
	}

	public Boolean showLabel() {
		return showLabel;
	}

	/**
	 * Evaluates the field options and returns a map with the field information
	 * usable by the frontend components that render the edit template.
	 *
	 * @param renderContext
	 * @param dataSource
	 * @return
	 * @throws FrameworkException
	 */
	public Map<String, Object> evaluate(final RenderContext renderContext, final Channel dataSource) throws FrameworkException {

		final Map<String, Object> result = new LinkedHashMap<>();

		result.put("label",        getLabel());
		result.put("value",        getValue());
		result.put("template",     getTemplate());
		result.put("editTemplate", getEditTemplate());
		result.put("config",       getConfig());

		final String propertyName = getPropertyName();
		if (propertyName != null) {

			result.put("propertyName", propertyName);

			// fetch options from data provider
			final FieldDefinition fieldDefinition = dataSource.getFields(renderContext).get(propertyName);
			if (fieldDefinition != null) {

				// add required flag
				result.put("required", fieldDefinition.isRequired());

				// multiple?
				result.put("multiple", fieldDefinition.isCollection());

				if (fieldDefinition.hasOptions()) {

					String label = "name";
					String filter = null;

					// check options that transform schema info
					if (!options.isEmpty()) {

						if (options.containsKey("label")) {
							label = (String) options.get("label");
						}

						filter = (String) options.get("filter");
					}

					result.put("options", fieldDefinition.getOptions(renderContext, filter, label));
				}
			}
		}

		return result;
	}

	public static DataField fromMap(final String name, final Map<String, Object> map) {

		final DataField field = new DataField(
			name,
			(String) map.get("label"),
			(String) map.get("value"),
			(String) map.get("template"),
			(String) map.get("editTemplate"),
			(String) map.get("propertyName"),
			(Boolean) map.get("showLabel")
		);

		// combine "slot" and "slots"
		final List<String> slots = (List) map.get("slots");
		if (slots != null) {

			field.getSlots().addAll(slots);
		}

		final String slot = (String) map.get("slot");
		if (slot != null) {

			field.getSlots().add(slot);
		}

		// options
		final Object o = map.get("options");
		if (o != null && o instanceof Map m) {

			field.getOptions().putAll(m);
		}

		// config
		final Object c = map.get("config");
		if (c != null && c instanceof Map m) {

			field.getConfig().putAll(m);
		}

		return field;
	}

	public void applyCssClasses(final Set<String> cssClasses) {

		if (config.containsKey("cols")) {

			cssClasses.add("col-span-" + config.get("cols"));
		}
	}
}
