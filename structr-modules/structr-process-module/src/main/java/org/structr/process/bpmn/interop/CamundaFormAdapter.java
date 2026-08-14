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
 * {@link BpmnVendorAdapter} for Camunda 7 embedded forms:
 * {@code <bpmn:userTask><bpmn:extensionElements><camunda:formData>
 * <camunda:formField id="…" label="…" type="…"><camunda:validation>
 * <camunda:constraint name="required"/></camunda:validation></camunda:formField>…}.
 *
 * <p>Camunda's is the largest install base and the fields are inline (unlike {@code formKey}
 * references, which point at externally deployed forms we cannot resolve from the file). The
 * field {@code id} becomes the Structr property name, {@code type} maps to a Structr property
 * type, and a {@code required} constraint puts the field in the writable set.</p>
 *
 * <p>Stateless and DOM-only, per the {@link BpmnVendorAdapter} contract -- no graph access.</p>
 */
public class CamundaFormAdapter implements BpmnVendorAdapter {

	private static final Logger logger = LoggerFactory.getLogger(CamundaFormAdapter.class);

	private static final String BPMN_NS    = "http://www.omg.org/spec/BPMN/20100524/MODEL";
	private static final String CAMUNDA_NS = "http://camunda.org/schema/1.0/bpmn";

	@Override
	public String vendorName() {

		return "Camunda";
	}

	@Override
	public boolean appliesTo(final Set<String> namespaceUris) {

		return namespaceUris.contains(CAMUNDA_NS);
	}

	@Override
	public List<VendorTaskForm> extractForms(final Element processEl) {

		final List<VendorTaskForm> forms = new ArrayList<>();

		// getElementsByTagNameNS is descendant-recursive, so user tasks nested in sub-processes
		// are covered too -- desirable, since their steps also get a div + form.
		final NodeList userTasks = processEl.getElementsByTagNameNS(BPMN_NS, "userTask");

		for (int i = 0, len = userTasks.getLength(); i < len; i++) {

			final Element userTask = (Element) userTasks.item(i);
			final String  taskId   = StringUtils.trimToNull(userTask.getAttribute("id"));

			if (taskId == null) {

				continue;
			}

			final Element formData = Xml.firstChildNS(Xml.firstChildByLocalName(userTask, "extensionElements"), CAMUNDA_NS, "formData");
			if (formData == null) {

				continue;
			}

			final List<VendorFormField> fields = new ArrayList<>();

			for (final Element formField : Xml.childrenNS(formData, CAMUNDA_NS, "formField")) {

				final String key = StringUtils.trimToNull(formField.getAttribute("id"));
				if (key == null) {

					logger.warn("Camunda formField without id on user task '{}' -- skipped", taskId);
					continue;
				}

				final String  label      = StringUtils.trimToNull(formField.getAttribute("label"));
				final String  structrType = mapType(formField.getAttribute("type"));
				final boolean required    = hasConstraint(formField, "required");
				final boolean readOnly    = hasConstraint(formField, "readonly");

				fields.add(new VendorFormField(key, label, structrType, required, readOnly));
			}

			if (!fields.isEmpty()) {

				forms.add(new VendorTaskForm(taskId, fields));
			}
		}

		return forms;
	}

	/**
	 * Map a Camunda form-field type to a Structr SchemaProperty type. Unknown types fall back to
	 * String -- a form still renders, just as text. (Camunda enum fields carry their options as
	 * child elements; a first-pass String keeps the field usable without synthesizing an enum
	 * type, which can be a follow-up.)
	 */
	private static String mapType(final String camundaType) {

		if (StringUtils.isBlank(camundaType)) {

			return VendorFormField.DEFAULT_TYPE;
		}

		// "string" and "enum" map to the default (String); an enum's options are a possible follow-up.

		return switch (camundaType.trim().toLowerCase()) {
			case "long"    -> "Long";
			case "boolean" -> "Boolean";
			case "date"    -> "Date";
			default        -> VendorFormField.DEFAULT_TYPE;
		};
	}

	/** True if the formField declares {@code <camunda:validation><camunda:constraint name="…"/>}. */
	private static boolean hasConstraint(final Element formField, final String constraintName) {

		final Element validation = Xml.firstChildNS(formField, CAMUNDA_NS, "validation");
		if (validation == null) {

			return false;
		}

		for (final Element constraint : Xml.childrenNS(validation, CAMUNDA_NS, "constraint")) {

			if (constraintName.equalsIgnoreCase(constraint.getAttribute("name"))) {

				return true;
			}
		}

		return false;
	}
}
