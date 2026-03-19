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

import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.CaseHelper;
import org.structr.core.entity.DataAdapter;
import org.structr.core.entity.DataAdapterField;
import org.structr.core.function.TitleizeFunction;
import org.structr.web.common.RenderContext;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class DataField extends LinkedHashMap<String, Object> {

	public DataField(final String name) {

		put("name", name);
	}

	public String getName() {
		return (String) get("name");
	}

	public String getLabel() {
		return (String) get("label");
	}

	public String getValue() {
		return (String)	get("value");
	}

	public String getTemplate() {
		return (String)	get("template");
	}

	public String getEditTemplate() {
		return (String)	get("editTemplate");
	}

	public String getPropertyName() {
		return (String)	get("propertyName");
	}

	public Set<String> getSlots() {
		return (Set<String>) get("slots");
	}

	public Map<String, Object> getOptions() {
		return (Map<String, Object>) get("options");
	}

	public Map<String, Object> getConfig() {
		return (Map<String, Object>) get("config");
	}

	public Boolean showLabel() {
		return (Boolean) get("showLabel");
	}

	public void applyCssClasses(final Set<String> cssClasses) {

		final Map<String, Object> config = getConfig();
		if (config != null && config.containsKey("cols")) {

			cssClasses.add("col-span-" + config.get("cols"));
		}
	}

	public void augment(final DataAdapterField augmentation) {

		putIfNotEmpty(this, "template",     augmentation.getRenderTemplate());
		putIfNotEmpty(this, "editTemplate", augmentation.getEditTemplate());
		putIfNotEmpty(this, "label",        augmentation.getLabel());
		putIfNotEmpty(this, "value",        augmentation.getValue());
		putIfNotEmpty(this, "dataType",     augmentation.getDataType());

		// only adapter fields can be deleted in UI
		putIfAbsent("source", "adapter");

		// store field ID
		put("id", augmentation.getUuid());

		// detail config
		final Map<String, Object> config = augmentation.getConfig();
		if (config != null) {

			put("config", config);
		}

		/*
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
		*/
	}

	public static DataField from(final RenderContext renderContext, final DataAdapter adapter, final String name,  final FieldDefinition fieldDefinition, final DataAdapterField augmentation) throws FrameworkException {

		final DataField field = new DataField(name);

		field.put("propertyName", name);
		field.put("value",        adapter.getDataKey() + "." + name);
		field.put("label",        TitleizeFunction.titleize(CaseHelper.toUnderscore(name, false), "_"));

		// data from field definition
		if (fieldDefinition != null) {

			field.put("required",       fieldDefinition.isRequired());
			field.put("multiple",       fieldDefinition.isCollection());
			field.put("template",       fieldDefinition.renderTemplate());
			field.put("editTemplate",   fieldDefinition.editTemplate());
			field.put("dataType",       fieldDefinition.dataType());
			field.put("isCollection",   fieldDefinition.isCollection());
			field.put("source",         "datasource");

			if (fieldDefinition.hasOptions()) {

				String label = "name";
				String filter = null;

				// check options that transform schema info
				final Map<String, Object> options = null;//augmentation.getOptions();
				if (options != null && !options.isEmpty()) {

					if (options.containsKey("label")) {
						label = (String) options.get("label");
					}

					filter = (String) options.get("filter");
				}

				field.put("options", fieldDefinition.getOptions(renderContext, filter, label));
			}

		} else {

			field.put("dataType", "custom");
		}

		// data from augmentation field
		if (augmentation != null) {

			field.augment(augmentation);
		}

		return field;

	}

	// ----- private static methods -----
	private static void putIfNotEmpty(final DataField field, final String key, final String value) {

		if (StringUtils.isNotBlank(value)) {
			field.put(key, value);
		}
	}
}
