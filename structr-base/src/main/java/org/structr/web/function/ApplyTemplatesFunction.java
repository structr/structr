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

import org.apache.tika.utils.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.DataProvider;
import org.structr.core.entity.DataSource;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.RenderContext;
import org.structr.web.datasource.DataField;
import org.structr.web.entity.dom.DOMNode;

import java.util.*;

/**
 * Convenience method to render named child nodes.
 */
public abstract class ApplyTemplatesFunction extends IncludeFunction {

	public void applyTemplates(final ActionContext ctx, final DataSource dataSource, final DOMNode domNode, final String tag, final String slot, final boolean inLoop) throws FrameworkException {

		final SecurityContext securityContext = ctx.getSecurityContext();
		final App app                         = StructrApp.getInstance(securityContext);
		final Map<String, DataField> fields   = dataSource.getFields(securityContext);
		final RenderContext innerCtx          = (RenderContext)ctx;
		final TagWithCSSInfo wrapper          = getWrapperElement(tag);
		final AsyncBuffer buffer              = innerCtx.getBuffer();
		final Boolean showLabels              = domNode.getShowLabelsFlagForComponent();
		final String requestedFieldSet        = domNode.getFieldSetForComponent();
		final List<String> fieldSet           = dataSource.getFieldSet(securityContext, requestedFieldSet);
		final String displayMode              = domNode.getDisplayModeForComponent(securityContext);
		final boolean useEditTemplate         = "input".equals(displayMode);
		final Object previousFieldValue       = innerCtx.getConstant("field");
		final DataProvider dataProvider       = dataSource.getDataProvider();
		final String channel                  = dataSource.getDataKey();
		String selectedValue                  = null;

		// we can only be a subscriber if the fields are not displayed in a loop
		// something about dimensions is also true here...
		if (!inLoop && channel != null) {

			final String role = domNode.getRoleForComponent();
			if ("subscriber".equals(role)) {

				selectedValue = innerCtx.getRequestParameter(channel);
				if (selectedValue != null) {

					final String dataKey = dataSource.getDataKey();
					if (dataKey != null) {

						final NodeInterface node = app.getNodeById(selectedValue);
						if (node != null) {

							innerCtx.putDataObject(dataKey, node);

						} else {

							// no item
							buffer.append("<span class=\"empty\">No item to display.</span>");
							return;
						}

					} else {

						logger.warn("Cannot store value for channel {}, data source {} does not define a dataKey.", channel, dataSource.getName());
					}

				} else {

					// show "no item" element and exit (make configurable?)
					buffer.append("<span class=\"empty\">No item to display.</span>");
					return;
				}
			}
		}

		for (final String field : fieldSet) {

			final DataField dataField = fields.get(field);
			if (dataField != null) {

				final Set<String> slots = dataField.getSlots();

				// no slot => iterate over all fields or just one slot
				if (slot == null || slots.contains(slot)) {

					final Set<String> cssClasses = new LinkedHashSet<>();
					final String editTemplate    = dataField.getEditTemplate();
					final String template        = dataField.getTemplate();
					final String valueSource     = dataField.getValue();
					final String label           = dataField.getLabel();
					Object value                 = null;

					// apply field-dependent CSS classes to the wrapper element
					dataField.applyCssClasses(cssClasses);

					// make field information available in context
					innerCtx.setConstant("field", dataField.evaluate(securityContext, dataProvider));

					// value present?
					if (valueSource != null) {

						value = innerCtx.getReferencedProperty(null, valueSource, null, 0, new EvaluationHints(), 0, 0);
						innerCtx.setConstant("value", value);
					}

					if (wrapper != null) {
						wrapper.formatStartTag(buffer, Map.of(), cssClasses);
					}

					// render labels?
					if (showLabels != null && showLabels && label != null) {

						buffer.append("<label>" + label + "</label>");
					}

					if (useEditTemplate && StringUtils.isEmpty(editTemplate)) {

						logger.warn("Field {} from data source {} cannot be used with displayMode input because it doesn't specify a value for `editTemplate`.", field, dataSource.getName());
						buffer.append("<span class=\"error\">No edit template.</span>");

					} else {

						final DOMNode templateNode = getTemplate(app, slot, useEditTemplate ? editTemplate : template);
						if (templateNode != null) {

							renderNode(securityContext, ctx, innerCtx, new Object[0], app, templateNode, true);

						} else {

							if (value != null) {
								buffer.append(value.toString());
							}
						}
					}

					if (wrapper != null) {
						wrapper.formatEndTag(buffer);
					}
				}
			}
		}

		// set previous value - if any
		innerCtx.setConstant("field", previousFieldValue);
	}

	public void applyLabels(final ActionContext ctx, final DataSource dataSource, final DOMNode domNode, final String templateWrapper, final String slot) throws FrameworkException {

		final SecurityContext securityContext = ctx.getSecurityContext();
		final RenderContext innerCtx          = new RenderContext((RenderContext)ctx);
		final Map<String, DataField> fields   = dataSource.getFields(securityContext);
		final TagWithCSSInfo wrapper          = getWrapperElement(templateWrapper);
		final AsyncBuffer buffer              = innerCtx.getBuffer();
		final String requestedFieldSet        = domNode.getFieldSetForComponent();
		final List<String> fieldSet           = dataSource.getFieldSet(securityContext, requestedFieldSet);
		final DataProvider dataProvider       = dataSource.getDataProvider();

		for (final String field : fieldSet) {

			final DataField dataField = fields.get(field);
			if (dataField != null) {

				final Set<String> slots = dataField.getSlots();

				// no slot => iterate over all fields or just one slot
				if (slot == null || slots.contains(slot)) {

					final String label = dataField.getLabel();

					innerCtx.setConstant("value", label);
					innerCtx.setConstant("field", dataField.evaluate(securityContext, dataProvider));

					if (wrapper != null) {
						wrapper.formatStartTag(buffer);
					}

					if (label != null) {
						buffer.append(label);
					}

					if (wrapper != null) {
						wrapper.formatEndTag(buffer);
					}
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
}