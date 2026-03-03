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
package org.structr.web.function;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.DataSource;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.dom.DOMNode;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Convenience method to render named child nodes.
 */
public abstract class ApplyTemplatesFunction extends IncludeFunction {

	public void applyTemplates(final ActionContext ctx, final DataSource dataSource, final String tag, final String slot) throws FrameworkException {

		final SecurityContext securityContext  = ctx.getSecurityContext();
		final App app                          = StructrApp.getInstance(securityContext);
		final List<Map<String, Object>> fields = dataSource.getFields(securityContext);
		final RenderContext innerCtx           = (RenderContext)ctx;
		final TagWithCSSInfo wrapper           = getWrapperElement(tag);
		final AsyncBuffer buffer               = innerCtx.getBuffer();

		for (final Map<String, Object> field : fields) {

			final Set<String> slots = getSlots(field);

			// no slot => iterate over all fields or just one slot
			if (slot == null || slots.contains(slot)) {

				final List<String> hideIn  = (List<String>) field.get("hideIn");
				final String templateName  = (String) field.get("template");
				final String valueSource   = (String) field.get("value");
				Object value               = null;

				if (hideIn != null && wrapper != null && wrapper.matches(hideIn)) {
					continue;
				}

				// value present?
				if (valueSource != null) {

					value = innerCtx.getReferencedProperty(null, valueSource, null, 0, new EvaluationHints(), 0, 0);
					innerCtx.setConstant("value", value);
				}

				innerCtx.setConstant("field", field);

				if (wrapper != null) {
					wrapper.formatStartTag(buffer);
				}

				final DOMNode templateNode = getTemplate(app, slot, templateName);
				if (templateNode != null) {

					renderNode(securityContext, ctx, innerCtx, new Object[0], app, templateNode, true);

				} else {

					if (value != null) {
						innerCtx.getBuffer().append(value.toString());
					}
				}

				if (wrapper != null) {
					wrapper.formatEndTag(buffer);
				}
			}
		}
	}

	public void applyLabels(final ActionContext ctx, final DataSource dataSource, final String templateWrapper, final String slot) throws FrameworkException {

		final SecurityContext securityContext  = ctx.getSecurityContext();
		final RenderContext innerCtx           = new RenderContext((RenderContext)ctx);
		final List<Map<String, Object>> fields = dataSource.getFields(securityContext);
		final TagWithCSSInfo wrapper           = getWrapperElement(templateWrapper);
		final AsyncBuffer buffer               = innerCtx.getBuffer();

		for (final Map<String, Object> field : fields) {

			// no slot => iterate over all fields or just one slot
			if (slot == null || slot.equals(field.get("slot"))) {

				final List<String> hideIn  = (List<String>) field.get("hideIn");
				Object value               = field.get("label");

				if (hideIn != null && wrapper != null && wrapper.matches(hideIn)) {
					continue;
				}

				innerCtx.setConstant("value", value);
				innerCtx.setConstant("field", field);

				if (wrapper != null) {
					wrapper.formatStartTag(buffer);
				}

				if (value != null) {
					innerCtx.getBuffer().append(value.toString());
				}

				if (wrapper != null) {
					wrapper.formatEndTag(buffer);
				}
			}
		}
	}

	private DOMNode getTemplate(final App app, final String slot, final String templateName) throws FrameworkException {

		final DOMNode template = getNodeForInclude(app, joinNonNullStrings(slot, templateName));
		if (template != null) {

			return template;
		}

		// fallback to templateName only
		final DOMNode fallback = getNodeForInclude(app, templateName);
		if (fallback != null) {

			return fallback;
		}

		return null;
	}

	private Set<String> getSlots(final Map<String, Object> field) {

		final Set<String> result = new LinkedHashSet<>();

		if (field.containsKey("slot") && field.containsKey("slots")) {
			throw new RuntimeException("Field specification must not contain both `slot` and `slots` entry, please use only one of the two.");
		}

		final Object slot = field.get("slot");
		if (slot != null && slot instanceof String s) {

			result.add(s);
		}

		final Object slots = field.get("slots");
		if (slots != null && slots instanceof List list) {

			result.addAll(list);
		}

		return result;
	}
}