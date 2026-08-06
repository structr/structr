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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * {@link BpmnVendorAdapter} for Flowable / Activiti inline user-task forms:
 * {@code <bpmn:userTask><bpmn:extensionElements>
 * <flowable:formProperty id="…" name="…" type="…" required="true" writable="true"/>…}.
 *
 * <p>Flowable and its Activiti predecessor share the element ({@code formProperty}) under different
 * namespaces, so this adapter accepts both. {@code writable="false"} maps to a read-only field
 * (excluded from the writable view); the {@code type} attribute maps to a Structr property type.</p>
 *
 * <p>Stateless and DOM-only, per the {@link BpmnVendorAdapter} contract.</p>
 */
public class FlowableFormAdapter implements BpmnVendorAdapter {

	private static final Logger logger = LoggerFactory.getLogger(FlowableFormAdapter.class);

	private static final String BPMN_NS     = "http://www.omg.org/spec/BPMN/20100524/MODEL";
	private static final String FLOWABLE_NS = "http://flowable.org/bpmn";
	private static final String ACTIVITI_NS = "http://activiti.org/bpmn";
	private static final Set<String> FORM_NAMESPACES = Set.of(FLOWABLE_NS, ACTIVITI_NS);

	@Override
	public String vendorName() {

		return "Flowable";
	}

	@Override
	public boolean appliesTo(final Set<String> namespaceUris) {

		return namespaceUris.contains(FLOWABLE_NS) || namespaceUris.contains(ACTIVITI_NS);
	}

	@Override
	public List<VendorTaskForm> extractForms(final Element processEl) {

		final List<VendorTaskForm> forms = new ArrayList<>();
		final NodeList userTasks         = processEl.getElementsByTagNameNS(BPMN_NS, "userTask");

		for (int i = 0; i < userTasks.getLength(); i++) {

			final Element userTask = (Element) userTasks.item(i);
			final String  taskId   = StringUtils.trimToNull(userTask.getAttribute("id"));

			if (taskId == null) {

				continue;
			}

			final Element ext = Xml.firstChildByLocalName(userTask, "extensionElements");
			if (ext == null) {

				continue;
			}

			final List<VendorFormField> fields = new ArrayList<>();

			for (final Element prop : Xml.childrenAnyNS(ext, FORM_NAMESPACES, "formProperty")) {

				final String key = StringUtils.trimToNull(prop.getAttribute("id"));
				if (key == null) {

					logger.warn("Flowable formProperty without id on user task '{}' -- skipped", taskId);
					continue;
				}

				final String  label      = StringUtils.trimToNull(prop.getAttribute("name"));
				final String  structrType = mapType(prop.getAttribute("type"));
				final boolean required    = "true".equalsIgnoreCase(prop.getAttribute("required"));

				// writable defaults to true in Flowable; only an explicit "false" is read-only.
				final boolean readOnly    = "false".equalsIgnoreCase(prop.getAttribute("writable"));

				fields.add(new VendorFormField(key, label, structrType, required, readOnly));
			}

			if (!fields.isEmpty()) {

				forms.add(new VendorTaskForm(taskId, fields));
			}
		}

		return forms;
	}

	/** Map a Flowable form-property type to a Structr SchemaProperty type; unknown -> String. */
	private static String mapType(final String flowableType) {

		if (StringUtils.isBlank(flowableType)) {

			return VendorFormField.DEFAULT_TYPE;
		}

		// "string" and "enum" map to the default (String).

		return switch (flowableType.trim().toLowerCase()) {
			case "long"    -> "Long";
			case "boolean" -> "Boolean";
			case "date"    -> "Date";
			case "double"  -> "Double";
			default        -> VendorFormField.DEFAULT_TYPE;
		};
	}
}
