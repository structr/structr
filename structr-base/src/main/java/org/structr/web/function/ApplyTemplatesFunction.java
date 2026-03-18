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
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.datasources.Channel;
import org.structr.core.entity.DataAdapter;
import org.structr.core.entity.DataSource;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.schema.action.ActionContext;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.RenderContext;
import org.structr.web.datasource.DataField;
import org.structr.web.datasource.TagWithCSSInfo;
import org.structr.web.entity.ComponentConfiguration;
import org.structr.web.entity.Widget;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.traits.definitions.WidgetTraitDefinition;
import org.structr.websocket.command.CreateComponentCommand;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Convenience method to render named child nodes.
 */
public abstract class ApplyTemplatesFunction extends IncludeFunction {

	public void applyTemplates(final ActionContext ctx, final DataAdapter dataAdapter, final DOMNode domNode, final String tag, final String slot, final boolean inLoop) throws FrameworkException {

		final SecurityContext securityContext = ctx.getSecurityContext();
		final App app                         = StructrApp.getInstance(securityContext);
		final RenderContext innerCtx          = (RenderContext)ctx;
		final TagWithCSSInfo wrapper          = getWrapperElement(tag);
		final AsyncBuffer buffer              = innerCtx.getBuffer();
		final Boolean showLabels              = domNode.getShowLabelsFlagForComponent();
		final String requestedFieldSet        = domNode.getFieldSetForComponent();
		final String displayMode              = domNode.getDisplayModeForComponent(securityContext);
		final String reloadBehaviour          = domNode.getReloadBehaviourForComponent();
		final boolean useEditTemplate         = "input".equals(displayMode);
		final Object previousFieldValue       = innerCtx.getConstant("field");
		final DOMNode component               = domNode.getClosestComponent();
		final ComponentConfiguration config   = component.getComponentConfiguration();
		final Channel sourceChannel           = config.getDataSource();
		String selectedValue                  = null;

		// we can only be a subscriber if we are not called from renderEach(), and maybe the component can use a dimensions property to filter data sources?
		if (!inLoop && sourceChannel != null) {

			final String role = domNode.getRoleForComponent();
			if ("subscriber".equals(role)) {

				final String channelName = sourceChannel.getName();

				selectedValue = innerCtx.getChannelValue(channelName);
				if (selectedValue != null) {

					final String dataKey = dataAdapter.getDataKey();
					if (dataKey != null) {

						final NodeInterface node = app.getNodeById(selectedValue);
						if (node != null) {

							innerCtx.putDataObject(dataKey, node);

						} else {

							// show "no item" template or fallback and exit
							renderTemplate(app, innerCtx, "span-no-item", "<span class=\"empty col-span-6\">No item to display.</span>");
							return;
						}

					} else {

						logger.warn("{}: cannot store value for channel {}, data adapter {} does not define a dataKey.", getName(), channelName, dataAdapter.getName());
					}

				} else {

					// show "no item" template or fallback and exit
					renderTemplate(app, innerCtx, "span-no-item", "<span class=\"empty col-span-6\">No item to display.</span>");
					return;
				}
			}
		}

		// fetch augmented fields from data adapter
		final Map<String, DataField> augmentedFields = dataAdapter.augmentFields(innerCtx, sourceChannel);
		final List<String> fieldSetFromComponent     = splitAndTrim(requestedFieldSet, ",");

		for (final String field : fieldSetFromComponent) {

			final DataField augmentedField = augmentedFields.get(field);
			if (augmentedField != null) {

				final Set<String> slots = augmentedField.getSlots();

				if (slot != null && slots.isEmpty()) {
					renderTemplate(app, innerCtx, "span-missing-slot", "<span class=\"error col-span-6\">Field '" + field + "' in data adapter '" + dataAdapter.getName() + "' is missing 'slot' entry.</span>");

				} else {

					// no slot => iterate over all fields or just one slot, or name
					if (slot == null || slots.contains(slot) || slot.equals(augmentedField.getName())) {

						final Set<String> cssClasses    = new LinkedHashSet<>();
						final String editTemplate       = augmentedField.getEditTemplate();
						final String template           = augmentedField.getTemplate();
						final String valueSource        = augmentedField.getValue();
						final String label              = augmentedField.getLabel();
						final Boolean showLabelOverride = augmentedField.showLabel();
						Object value                    = null;

						// apply field-dependent CSS classes to the wrapper element
						augmentedField.applyCssClasses(cssClasses);

						// make field information available in context
						innerCtx.setConstant("field", augmentedField);

						// value present?
						if (valueSource != null) {

							value = innerCtx.getReferencedProperty(null, valueSource, null, 0, 0, 0);

							// make iterables permanent
							if (value instanceof Iterable) {
								value = Iterables.toList((Iterable) value);
							}

							innerCtx.setConstant("value", value);

						} else {

							logger.warn("{}: field {} from data source {} has no value expression and will therefore produce no output.", getName(), field, dataAdapter.getName());
						}

						if (wrapper != null) {
							wrapper.formatStartTag(buffer, Map.of(), cssClasses);
						}

						// render labels?
						if (label != null) {

							boolean doShow = showLabels != null && showLabels;

							if (showLabelOverride != null) {
								doShow = showLabelOverride;
							}

							if (doShow) {
								buffer.append("<label>" + label + "</label>");
							}
						}

						if (useEditTemplate && StringUtils.isEmpty(editTemplate)) {

							logger.warn("{}: field {} from data source {} cannot be used with displayMode input because it doesn't specify a value for `editTemplate`.", getName(), field, dataAdapter.getName());
							buffer.append("<span class=\"error\">No edit template.</span>");

						} else {

							final DOMNode templateNode = getTemplate(app, slot, useEditTemplate ? editTemplate : template);
							if (templateNode != null) {

								final DataAdapter previousDataAdapter = innerCtx.getCurrentAdapter();
								final Channel previousDataSource = innerCtx.getCurrentDataSource();
								final String previousReloadBehaviour = innerCtx.getCurrentReloadBehaviour();

								// we need to make the current data source available to the inner template
								innerCtx.setCurrentAdapter(dataAdapter);
								innerCtx.setCurrentDataSource(sourceChannel);
								innerCtx.setCurrentReloadBehaviour(reloadBehaviour);

								try {
									templateNode.render(innerCtx, 0);

								} finally {

									innerCtx.setCurrentAdapter(previousDataAdapter);
									innerCtx.setCurrentDataSource(previousDataSource);
									innerCtx.setCurrentReloadBehaviour(previousReloadBehaviour);
								}

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
		}

		// set previous value - if any
		innerCtx.setConstant("field", previousFieldValue);
	}

	public void applyLabels(final ActionContext ctx, final DataAdapter dataAdapter, final DOMNode domNode, final String templateWrapper, final String slot) throws FrameworkException {

		final RenderContext innerCtx          = (RenderContext) ctx;
		final DOMNode component               = domNode.getClosestComponent();
		final ComponentConfiguration config   = component.getComponentConfiguration();
		final Channel sourceChannel           = config.getDataSource();
		final Map<String, DataField> fields   = dataAdapter.augmentFields(innerCtx, sourceChannel);
		final TagWithCSSInfo wrapper          = getWrapperElement(templateWrapper);
		final AsyncBuffer buffer              = innerCtx.getBuffer();
		final String requestedFieldSet        = domNode.getFieldSetForComponent();
		final List<String> fieldSet           = splitAndTrim(requestedFieldSet, ",");

		if (fieldSet.isEmpty()) {
			logger.warn("{}: {} with ID {} doesn't specify a fieldSet, nothing will be rendered.", getName(), domNode.getType(), domNode.getUuid());
		}

		for (final String field : fieldSet) {

			final DataField dataField = fields.get(field);
			if (dataField != null) {

				final Set<String> slots = dataField.getSlots();

				// no slot => iterate over all fields or just one slot
				if (slot == null || slots.contains(slot)) {

					final String label = dataField.getLabel();

					innerCtx.setConstant("value", label);
					innerCtx.setConstant("field", dataField);

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

		if (StringUtils.isBlank(templateName)) {
			return null;
		}

		final DOMNode template = getNodeForInclude(app, joinNonNullStrings(slot, templateName));
		if (template != null) {

			return template;
		}

		// fallback to templateName only
		final DOMNode fallback = getNodeForInclude(app, templateName);
		if (fallback != null) {

			return fallback;
		}

		// no template found => try to find a render template widget and instantiate it
		// instantiate as superuser
		final SecurityContext superAdminContext = SecurityContext.getSuperUserInstance();
		final Traits widgetTraits               = Traits.of(StructrTraits.WIDGET);
		final NodeInterface widgetNode              = StructrApp.getInstance(superAdminContext).nodeQuery(StructrTraits.WIDGET)
			.key(widgetTraits.key(WidgetTraitDefinition.IS_RENDER_TEMPLATE_PROPERTY), true)
			.key(widgetTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), templateName)
			.getFirst();

		if (widgetNode != null) {

			final Page shadowPage = CreateComponentCommand.getOrCreateHiddenDocument();
			final Widget widget    = widgetNode.as(Widget.class);

			// try to expand widget
			Widget.expandWidget(superAdminContext, shadowPage, null, "http://localhost", Map.of("source", widget.getSource()), false);

			// try again
			final DOMNode expanded = getNodeForInclude(app, templateName);
			if (expanded != null) {

				return expanded;
			}
		}

		return null;
	}

	private void renderTemplate(final App app, final RenderContext ctx, final String name, final String fallbackHtml) throws FrameworkException {

		DOMNode template = getTemplate(app, null, name);
		if (template != null) {

			template.render(ctx, 0);
		}

		ctx.getBuffer().append(fallbackHtml);
	}
}