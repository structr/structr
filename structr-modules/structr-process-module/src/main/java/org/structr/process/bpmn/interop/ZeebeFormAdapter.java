/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.process.bpmn.interop;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link BpmnVendorAdapter} for Zeebe / Camunda 8 embedded forms. Zeebe stores forms as a form-js
 * JSON schema inside {@code <zeebe:userTaskForm id="…">{json}</zeebe:userTaskForm>} (in the
 * process's extension elements), and a user task references one via
 * {@code <zeebe:formDefinition formKey="camunda-forms:bpmn:<id>"/>}.
 *
 * <p>form-js is the closest thing to a cross-vendor form schema, so we parse it directly with our
 * own reader (Gson) -- no bpmn.io library, per the interop pillars ({@link BpmnVendorAdapter}). A
 * form-js component contributes a field only when it has a {@code key} (layout components such as
 * {@code text} / {@code button} have none and are skipped).</p>
 *
 * <p>Stateless and DOM/JSON-only, per the {@link BpmnVendorAdapter} contract.</p>
 */
public class ZeebeFormAdapter implements BpmnVendorAdapter {

	private static final Logger logger = LoggerFactory.getLogger(ZeebeFormAdapter.class);

	private static final String BPMN_NS  = "http://www.omg.org/spec/BPMN/20100524/MODEL";
	private static final String ZEEBE_NS = "http://camunda.org/schema/zeebe/1.0";

	@Override
	public String vendorName() {

		return "Zeebe";
	}

	@Override
	public boolean appliesTo(final Set<String> namespaceUris) {

		return namespaceUris.contains(ZEEBE_NS);
	}

	@Override
	public List<VendorTaskForm> extractForms(final Element processEl) {

		// 1. Collect the embedded form-js schemas by their id (they live in extensionElements).
		final Map<String, JsonObject> formsById = new HashMap<>();
		final NodeList formEls                  = processEl.getElementsByTagNameNS(ZEEBE_NS, "userTaskForm");

		for (int i = 0; i < formEls.getLength(); i++) {

			final Element formEl = (Element) formEls.item(i);
			final String  id     = StringUtils.trimToNull(formEl.getAttribute("id"));
			final String  json   = StringUtils.trimToNull(formEl.getTextContent());

			if (id == null || json == null) {

				continue;
			}

			try {

				final JsonElement parsed = JsonParser.parseString(json);
				if (parsed.isJsonObject()) {

					formsById.put(id, parsed.getAsJsonObject());
				}

			} catch (final Exception e) {

				logger.warn("Zeebe userTaskForm '{}' is not valid JSON -- skipped: {}", id, e.getMessage());
			}
		}

		if (formsById.isEmpty()) {

			return List.of();
		}

		// 2. For each user task, resolve its formDefinition -> form-js schema -> fields.
		final List<VendorTaskForm> forms = new ArrayList<>();
		final NodeList userTasks         = processEl.getElementsByTagNameNS(BPMN_NS, "userTask");

		for (int i = 0; i < userTasks.getLength(); i++) {

			final Element userTask = (Element) userTasks.item(i);
			final String  taskId   = StringUtils.trimToNull(userTask.getAttribute("id"));

			if (taskId == null) {

				continue;
			}

			final Element formDef = Xml.firstChildNS(Xml.firstChildByLocalName(userTask, "extensionElements"), ZEEBE_NS, "formDefinition");
			if (formDef == null) {

				continue;
			}

			final String formId = formIdFromKey(formDef.getAttribute("formKey"));
			if (formId == null) {

				continue;
			}

			final JsonObject schema = formsById.get(formId);
			if (schema == null) {

				logger.warn("Zeebe user task '{}' references unknown form id '{}' -- no fields extracted", taskId, formId);
				continue;
			}

			final List<VendorFormField> fields = fieldsFromSchema(schema);
			if (!fields.isEmpty()) {

				forms.add(new VendorTaskForm(taskId, fields));
			}
		}

		return forms;
	}

	/** {@code camunda-forms:bpmn:<id>} -> {@code <id>}; null when the key is blank/malformed. */
	private static String formIdFromKey(final String formKey) {

		final String key = StringUtils.trimToNull(formKey);
		if (key == null) {

			return null;
		}

		return StringUtils.trimToNull(StringUtils.substringAfterLast(key, ":"));
	}

	private static List<VendorFormField> fieldsFromSchema(final JsonObject schema) {

		final List<VendorFormField> fields = new ArrayList<>();
		final JsonElement components       = schema.get("components");

		if (components == null || !components.isJsonArray()) {

			return fields;
		}

		for (final JsonElement el : components.getAsJsonArray()) {

			if (!el.isJsonObject()) {

				continue;
			}

			final JsonObject component = el.getAsJsonObject();

			// Only input components carry a key; layout components (text/button/spacer) do not.
			final String key = optString(component, "key");
			if (key == null) {

				continue;
			}

			final String  label      = optString(component, "label");
			final String  structrType = mapType(optString(component, "type"));
			final boolean required    = component.has("validate") && component.get("validate").isJsonObject() && optBool(component.getAsJsonObject("validate"), "required");
			final boolean readOnly    = optBool(component, "readonly") || optBool(component, "disabled");

			fields.add(new VendorFormField(key, label, structrType, required, readOnly));
		}

		return fields;
	}

	/** Map a form-js component type to a Structr SchemaProperty type; unknown -> String. */
	private static String mapType(final String formJsType) {

		if (StringUtils.isBlank(formJsType)) {

			return VendorFormField.DEFAULT_TYPE;
		}

		// textfield / textarea / radio / select / taglist all map to the default (String).

		return switch (formJsType.trim().toLowerCase()) {
			case "number"   -> "Double";
			case "checkbox" -> "Boolean";
			case "datetime" -> "Date";
			default         -> VendorFormField.DEFAULT_TYPE;
		};
	}

	private static String optString(final JsonObject obj, final String key) {

		final JsonElement el = obj.get(key);

		return (el != null && el.isJsonPrimitive()) ? StringUtils.trimToNull(el.getAsString()) : null;
	}

	private static boolean optBool(final JsonObject obj, final String key) {

		final JsonElement el = obj.get(key);

		try {

			return el != null && el.isJsonPrimitive() && el.getAsBoolean();

		} catch (final Exception e) {

			return false;
		}
	}
}
