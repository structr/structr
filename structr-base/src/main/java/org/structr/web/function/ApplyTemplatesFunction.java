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

import org.apache.commons.lang3.StringUtils;
import org.structr.api.util.Iterables;
import org.structr.common.ChannelInput;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.datasources.Channel;
import org.structr.core.datasources.ChannelResult;
import org.structr.core.datasources.SortInfo;
import org.structr.core.entity.DataAdapter;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.script.Scripting;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
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

import java.util.*;

/**
 * Convenience method to render named child nodes.
 */
public abstract class ApplyTemplatesFunction extends IncludeFunction {

	public void applyTemplates(final ActionContext ctx, final DataAdapter dataAdapter, final DOMNode domNode, final String tag, final boolean inLoop) throws FrameworkException {

		final SecurityContext securityContext = ctx.getSecurityContext();
		final App app                         = StructrApp.getInstance(securityContext);
		final RenderContext innerCtx          = (RenderContext)ctx;
		final TagWithCSSInfo wrapper          = getWrapperElement(tag);
		final AsyncBuffer buffer              = innerCtx.getBuffer();
		final Boolean showLabels              = domNode.getShowLabelsFlagForComponent();
		final String requestedFieldSet        = domNode.getFieldSetForComponent();
		final String displayMode              = domNode.getDisplayModeForComponent(securityContext);
		final String reloadBehaviour          = domNode.getReloadBehaviourForComponent();
		final Object previousFieldValue       = innerCtx.getConstant("field");
		final DOMNode component               = domNode.getClosestComponent();
		final ComponentConfiguration config   = component.getComponentConfiguration();
		final Channel sourceChannel           = config.getDataSource();
		final boolean globalUseEditTemplate   = "input".equals(displayMode);

		if (!inLoop && sourceChannel != null) {

			final String role = domNode.getRoleForComponent();
			if ("subscriber".equals(role)) {

				final ChannelInput               channelInput = config.getChannelInput(innerCtx);
				final ChannelResult<GraphObject> result       = sourceChannel.getResult(innerCtx, channelInput, config.getTransform());
				final String                     dataKey      = dataAdapter.getDataKey();

				if (dataKey != null) {

					final GraphObject item = result.getFirst();
					if (item != null) {

						innerCtx.putDataObject(dataKey, item);

					} else {

						// show "no item" template or fallback and exit
						renderTemplate(app, innerCtx, "span-no-item", "<span class=\"empty col-span-6\">No item to display.</span>");
						return;
					}
				}
			}
		}

		// fetch augmented fields from data adapter
		final Map<String, DataField> augmentedFields = dataAdapter.augmentFields(innerCtx, sourceChannel, true);
		final List<String> fieldSetFromComponent     = Function.splitAndTrim(requestedFieldSet, ",");

		for (final String field : fieldSetFromComponent) {

			// special syntax for nested children
			if (field.startsWith("$") && field.length() > 1) {

				final List<RelationshipInterface> rels = domNode.getChildRelationships();
				if (rels.isEmpty()) {

					final DOMNode _syncedNode = domNode.getSharedComponent();
					// No child relationships, maybe this node is in sync with another node
					if (_syncedNode != null) {
						rels.addAll(_syncedNode.getChildRelationships());
					}
				}

				final String part = field.substring(1);
				switch (part) {

					case "*":
						// render all children
						for (final RelationshipInterface rel : rels) {
							final DOMNode subNode = rel.getTargetNode().as(DOMNode.class);
							subNode.render(innerCtx, 1);
						}
						break;

					default:
						// render specific child at index $n
						final Integer index = Function.parseInt(part);
						if (index != null) {
							final int zeroBasedIndex = index - 1;
							if (zeroBasedIndex >= 0 && zeroBasedIndex < rels.size()) {
								final RelationshipInterface rel = rels.get(zeroBasedIndex);
								final DOMNode subNode = rel.getTargetNode().as(DOMNode.class);
								subNode.render(innerCtx, 1);
							}
						}
						break;
				}

			} else {

				final DataField augmentedField = augmentedFields.get(field);
				if (augmentedField != null) {

					for (final Map.Entry<String, GraphObject> column : augmentedField.expandColumns(innerCtx).entrySet()) {

						final String      label             = column.getKey();
						final Set<String> cssClasses        = new LinkedHashSet<>();
						final String      editTemplate      = augmentedField.getEditTemplate();
						final String      template          = augmentedField.getTemplate();
						final String      valueSource       = augmentedField.getValue();
						final String      displayModeScript = augmentedField.getEditModeCondition();
						final Boolean     showLabelOverride = augmentedField.showLabel();
						final GraphObject columnValue       = column.getValue();
						Object            value             = null;

						// apply field-dependent CSS classes to the wrapper element
						augmentedField.applyCssClasses(cssClasses);

						// make field information available in context
						innerCtx.setConstant("field", augmentedField);

						// make column value available in context, if applicable
						if (columnValue != null) {
							innerCtx.setConstant(augmentedField.getColumnKey(), columnValue);
						}

						// reset useEditTemplate flag before evaluating script
						boolean useEditTemplate = globalUseEditTemplate;

						// display mode script?
						if (StringUtils.isNotBlank(displayModeScript)) {

							final Object booleanResult = Scripting.evaluate(innerCtx, null, "${" + displayModeScript + "}", "displayModeScript expression of data field '" + field + "' in component '" + component.getName() + "' of page '" + component.getOwnerDocument().getName() + "'");
							if (booleanResult != null) {

								// be lenient
								useEditTemplate = "true".equalsIgnoreCase(booleanResult.toString());
							}
						}

						// value present?
						if (valueSource != null) {

							value = Scripting.evaluate(innerCtx, null, "${" + valueSource + "}", "Value expression of data field '" + field + "' in component '" + component.getName() + "' of page '" + component.getOwnerDocument().getName() + "'");
							//value = innerCtx.getReferencedProperty(null, valueSource, null, 0, 0, 0);

							// make iterables permanent
							if (value instanceof Iterable) {
								value = Iterables.toList((Iterable) value);
							}

							innerCtx.setConstant("value", value);

						} else {

							logger.warn("{}: field '{}' from data source '{}' has no value expression and will therefore produce no output.", getName(), field, dataAdapter.getName());
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

							logger.warn("{}: field '{}' from data source '{}' cannot be used with displayMode 'input' because it doesn't specify a value for `editTemplate`.", getName(), field, dataAdapter.getName());
							buffer.append("<span class=\"error\">No edit template.</span>");

						} else {

							final DOMNode templateNode = getTemplate(app, useEditTemplate ? editTemplate : template);
							if (templateNode != null) {

								final DataAdapter previousDataAdapter     = innerCtx.getCurrentAdapter();
								final Channel     previousDataSource      = innerCtx.getCurrentDataSource();
								final String      previousReloadBehaviour = innerCtx.getCurrentReloadBehaviour();

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

		// no data source => nothing to do
		if (sourceChannel == null) {
			return;
		}

		final Map<String, DataField> fields   = dataAdapter.augmentFields(innerCtx, sourceChannel, true);
		final TagWithCSSInfo wrapper          = getWrapperElement(templateWrapper);
		final AsyncBuffer buffer              = innerCtx.getBuffer();
		final String requestedFieldSet        = domNode.getFieldSetForComponent();
		final List<String> fieldSet           = splitAndTrim(requestedFieldSet, ",");
		final String sortKey                  = sourceChannel.getSortKey();

		if (fieldSet.isEmpty()) {
			logger.warn("{}: {} with ID {} doesn't specify a fieldSet, nothing will be rendered.", getName(), domNode.getType(), domNode.getUuid());
		}

		for (final String field : fieldSet) {

			final DataField dataField = fields.get(field);
			if (dataField != null) {

				final Set<String> slots = dataField.getSlots();

				// no slot => iterate over all fields or just one slot
				if (slot == null || slots.contains(slot)) {

					for (final Map.Entry<String, GraphObject> column : dataField.expandColumns(innerCtx).entrySet()) {

						final String label            = column.getKey();
						final GraphObject columnValue = column.getValue();
						final SortInfo sortInfo       = getSortInfo(innerCtx, sortKey, dataField.getSortKey());

						innerCtx.setConstant("value", label);
						innerCtx.setConstant("field", dataField);

						if (columnValue != null) {
							innerCtx.setConstant(dataField.getColumnKey(), columnValue);
						}

						// format sort controls on labels
						final Map<String, String> data = new LinkedHashMap<>();
						data.put("data-structr-success-target", "[data-channel]");
						data.put("data-structr-events", "click");
						data.put("data-structr-target", sortKey);

						final Set<String> classes = new LinkedHashSet<>();
						if (sortInfo != null && !dataField.isCollection()) {

							classes.add("sw-sortable");

							data.put("data-" + sortKey, sortInfo.toString());

							if (sortInfo.active) {

								if (sortInfo.descending) {

									classes.add("descending");

								} else {

									classes.add("ascending");
								}
							}
						}

						if (wrapper != null) {
							wrapper.formatStartTag(buffer, data, classes);
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
	}

	protected DOMNode getTemplate(final App app, final String templateName) throws FrameworkException {

		if (StringUtils.isBlank(templateName)) {
			return null;
		}

		final DOMNode template = getNodeForInclude(app, templateName);
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
		final NodeInterface widgetNode          = StructrApp.getInstance(superAdminContext).nodeQuery(StructrTraits.WIDGET)
			.key(widgetTraits.key(WidgetTraitDefinition.IS_RENDER_TEMPLATE_PROPERTY), true)
			.key(widgetTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), templateName)
			.getFirst();

		if (widgetNode != null) {

			final Page shadowPage = CreateComponentCommand.getOrCreateHiddenDocument();
			final Widget widget   = widgetNode.as(Widget.class);

			// try to expand widget
			final DOMNode imported = Widget.expandWidget(shadowPage, null, "http://localhost", Map.of("source", widget.getSource()), false);

			if (imported != null) {

				imported.setVisibility(true, true);

				// make all children public
				for (final NodeInterface child : imported.getAllChildNodes()) {
					child.setVisibility(true, true);
				}
			}

			// try again
			final DOMNode expanded = getNodeForInclude(app, templateName);
			if (expanded != null) {

				return expanded;
			}
		}

		return null;
	}

	protected void renderTemplate(final App app, final RenderContext ctx, final String name, final String fallbackHtml) throws FrameworkException {

		DOMNode template = getTemplate(app, name);
		if (template != null) {

			template.render(ctx, 0);
		}

		ctx.getBuffer().append(fallbackHtml);
	}

	protected SortInfo getSortInfo(final RenderContext renderContext, final String requestParameterName, final String fieldName) {

		final SortInfo currentSortInfo = SortInfo.fromString(renderContext.getRequestParameter(requestParameterName));
		final SortInfo newSortInfo     = SortInfo.fromString(fieldName);

		if (currentSortInfo != null && newSortInfo != null) {

			if (currentSortInfo.sortKey.equals(newSortInfo.sortKey)) {

				// invert sort direction
				newSortInfo.descending = !currentSortInfo.descending;

				// is the current field the active sort key?
				newSortInfo.active = fieldName.equals(currentSortInfo.sortKey);
			}
		}

		return newSortInfo;
	}
}