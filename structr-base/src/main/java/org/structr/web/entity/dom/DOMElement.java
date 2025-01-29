/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.web.entity.dom;

import com.google.common.base.CaseFormat;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.RenderContext;
import org.structr.web.common.RenderContext.EditMode;
import org.structr.web.entity.User;
import org.structr.web.entity.dom.relationship.DOMElementINPUT_ELEMENTParameterMapping;
import org.structr.web.entity.dom.relationship.DOMElementRELOADSDOMElement;
import org.structr.web.entity.dom.relationship.DOMElementTRIGGERED_BYActionMapping;
import org.structr.web.entity.dom.relationship.DOMNodeCONTAINSDOMNode;
import org.structr.web.entity.event.ActionMapping;
import org.structr.web.entity.event.ParameterMapping;

import java.util.Map;
import org.structr.web.traits.definitions.dom.DOMElementTraitDefinition;

public interface DOMElement extends DOMNode  {

	String GET_HTML_ATTRIBUTES_CALL = "return (Property[]) org.apache.commons.lang3.ArrayUtils.addAll(super.getHtmlAttributes(), _html_View.properties());";

	String lowercaseBodyName = "body";

	String EVENT_ACTION_MAPPING_PARAMETER_HTMLEVENT = "htmlEvent";
	String EVENT_ACTION_MAPPING_PARAMETER_STRUCTRIDEXPRESSION = "structrIdExpression";
	String EVENT_ACTION_MAPPING_PARAMETER_STRUCTRTARGET = "structrTarget";
	String EVENT_ACTION_MAPPING_PARAMETER_STRUCTRDATATYPE = "structrDataType";
	String EVENT_ACTION_MAPPING_PARAMETER_STRUCTRMETHOD = "structrMethod";
	String EVENT_ACTION_MAPPING_PARAMETER_CHILDID = "childId";
	String EVENT_ACTION_MAPPING_PARAMETER_SOURCEOBJECT = "sourceObject";
	String EVENT_ACTION_MAPPING_PARAMETER_SOURCEPROPERTY = "sourceProperty";
	int HtmlPrefixLength = PropertyView.Html.length();

	// ----- public methods -----
	String getTag();
	String getHtmlId();
	String getHtmlName();
	String getEventMapping();
	String getRenderingMode();
	String getDelayOrInterval();
	String getDataReloadTarget();

	void setAttribute(final String key, final String value) throws FrameworkException;

	boolean isManualReloadTarget();
	Iterable<DOMElement> getReloadSources();

	Iterable<PropertyKey> getHtmlAttributes();
	Iterable<String> getHtmlAttributeNames();

	void openingTag(final AsyncBuffer out, final String tag, final RenderContext.EditMode editMode, final RenderContext renderContext, final int depth) throws FrameworkException;

	boolean isInsertable();
	boolean isFromWidget();

	Iterable<ActionMapping> getTriggeredActions();
	Iterable<ParameterMapping> getParameterMappings();

	Map<String, Object> getMappedEvents();

	static int intOrOne(final String source) {

		if (source != null) {

			try {

				return Integer.valueOf(source);

			} catch (Throwable t) {
			}

			// render end tag, if needed (= if not singleton tags)
			if (StringUtils.isNotBlank(_tag) && (!isVoid) || (isVoid && synced != null && EditMode.DEPLOYMENT.equals(editMode))) {

				// only insert a newline + indentation before the closing tag if any child-element used a newline
				final boolean isTemplate     = synced != null && EditMode.DEPLOYMENT.equals(editMode);

				if (anyChildNodeCreatesNewLine || isTemplate) {

					out.append(DOMNode.indent(depth, renderContext));
				}

				if (synced != null && EditMode.DEPLOYMENT.equals(editMode)) {

					out.append("</structr:component>");

				} else if (isTemplate) {

					out.append("</structr:template>");

				} else {

					out.append("</").append(_tag).append(">");
				}
			}
		}
	}

	public void openingTag(final AsyncBuffer out, final String tag, final EditMode editMode, final RenderContext renderContext, final int depth) throws FrameworkException {

		final boolean hasSharedComponent         = getProperty(DOMElement.hasSharedComponent);
		final DOMElement _sharedComponentElement = hasSharedComponent ? (DOMElement) getSharedComponent() : null;

		if (_sharedComponentElement != null && EditMode.DEPLOYMENT.equals(editMode)) {

			out.append("<structr:component src=\"");

			final String _name = _sharedComponentElement.getProperty(AbstractNode.name);
			out.append(_name != null ? _name.concat("-").concat(_sharedComponentElement.getUuid()) : _sharedComponentElement.getUuid());

			out.append("\"");

			renderSharedComponentConfiguration(out, editMode);

			// include data-* attributes in template
			renderCustomAttributes(out, renderContext.getSecurityContext(), renderContext);

		} else {

			out.append("<").append(tag);

			final ConfigurationProvider config = StructrApp.getConfiguration();
			final Class type  = getEntityType();
			final String uuid = getUuid();

			final List<PropertyKey> htmlAttributes = new ArrayList<>();


			getNode().getPropertyKeys().forEach((key) -> {
				if (key.startsWith(PropertyView.Html)) {
					htmlAttributes.add(config.getPropertyKeyForJSONName(type, key));
				}
			});

			if (EditMode.DEPLOYMENT.equals(editMode)) {
				Collections.sort(htmlAttributes);
			}

			for (PropertyKey attribute : htmlAttributes) {

				String value = null;

				if (EditMode.DEPLOYMENT.equals(editMode)) {

					value = (String)getProperty(attribute);

				} else {

					value = getPropertyWithVariableReplacement(renderContext, attribute);
				}

				if (!(EditMode.RAW.equals(editMode) || EditMode.WIDGET.equals(editMode))) {

					value = DOMNode.escapeForHtmlAttributes(value);
				}

				if (value != null) {

					String key = attribute.jsonName().substring(PropertyView.Html.length());

					out.append(" ").append(key).append("=\"").append(value).append("\"");

				}
			}

			// make repeater data object ID available
			final GraphObject repeaterDataObject = renderContext.getDataObject();
			if (repeaterDataObject != null && StringUtils.isNotBlank(getDataKey())) {

				out.append(" data-repeater-data-object-id=\"").append(repeaterDataObject.getUuid()).append("\"");
			}

			// include arbitrary data-* attributes
			renderSharedComponentConfiguration(out, editMode);
			renderCustomAttributes(out, renderContext.getSecurityContext(), renderContext);

			// new: managed attributes (like selected
			renderManagedAttributes(out, renderContext.getSecurityContext(), renderContext);

			// include special mode attributes
			switch (editMode) {

				case SHAPES:
				case SHAPES_MINIATURES:

					final boolean isInsertable = getProperty(DOMElement.dataInsertProperty);
					final boolean isFromWidget = getProperty(DOMElement.fromWidgetProperty);

					if (isInsertable || isFromWidget) {
						out.append(" data-structr-id=\"").append(uuid).append("\"");
					}
					break;

				case CONTENT:

					if (depth == 0) {

						String pageId = renderContext.getPageId();

						if (pageId != null) {

							out.append(" data-structr-page=\"").append(pageId).append("\"");
						}
					}

					out.append(" data-structr-id=\"").append(uuid).append("\"");
					break;

				case RAW:

					out.append(" ").append("data-structr-hash").append("=\"").append(getIdHash()).append("\"");
					break;

				case WIDGET:
				case DEPLOYMENT:

					final String eventMapping = getEventMapping();
					if (eventMapping != null) {

						out.append(" ").append("data-structr-meta-event-mapping").append("=\"").append(StringEscapeUtils.escapeHtml(eventMapping)).append("\"");
					}
					break;

				case NONE:

					// Get actions in superuser context
					final DOMElement thisElementWithSuperuserContext = StructrApp.getInstance().get(DOMElement.class, uuid);
					final Iterable<ActionMapping> triggeredActions   = thisElementWithSuperuserContext.getProperty(DOMElement.triggeredActionsProperty);
					final List<ActionMapping> list                   = Iterables.toList(triggeredActions);
					boolean outputStructrId                          = false;

					if (!list.isEmpty()) {

						// all active elements need data-structr-id
						outputStructrId = true;

						// why only the first one?!
						final ActionMapping triggeredAction = list.get(0);
						final String options                = triggeredAction.getProperty(ActionMapping.optionsProperty);

						// support for configuration options
						if (StringUtils.isNotBlank(options)) {
							out.append(" data-structr-options=\"").append(uuid).append("\"");
						}

						String eventsString = null;
						final Map<String, Object> mapping = getMappedEvents();
						if (mapping != null) {
							eventsString = StringUtils.join(mapping.keySet(), ",");
						}

						// append all stored action mapping keys as data-structr-<key> attributes
						for (final Property<String> key : Set.of(ActionMapping.eventProperty, ActionMapping.actionProperty, ActionMapping.methodProperty, ActionMapping.dataTypeProperty, ActionMapping.idExpressionProperty)) {

							final String value = triggeredAction.getPropertyWithVariableReplacement(renderContext, key);
							if (StringUtils.isNotBlank(value)) {

								final String keyHyphenated = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, key.jsonName());
								out.append(" data-structr-" + keyHyphenated + "=\"").append(value).append("\"");
							}

							if (key.equals(ActionMapping.eventProperty)) {

								eventsString = (String) value;
							}
						}

						if (eventsString != null) {
							out.append(" data-structr-events=\"").append(eventsString).append("\"");
						}

						renderDialogAttributes(renderContext, out, triggeredAction);
						renderSuccessNotificationAttributes(renderContext, out, triggeredAction);
						renderFailureNotificationAttributes(renderContext, out, triggeredAction);
						renderSuccessBehaviourAttributes(renderContext, out, triggeredAction);
						renderFailureBehaviourAttributes(renderContext, out, triggeredAction);


//						{ // TODO: Migrate tree handling to new action mapping
//							// toggle-tree-item
//							if (mapping.containsValue("toggle-tree-item")) {
//
//								final String targetValue = thisElement.getPropertyWithVariableReplacement(renderContext, targetKey);
//								final String key = thisElement.getTreeItemSessionIdentifier(targetValue);
//								final boolean open = thisElement.getSessionAttribute(renderContext.getSecurityContext(), key) != null;
//
//								out.append(" data-tree-item-state=\"").append(open ? "open" : "closed").append("\"");
//							}
//						}


						// **************************************************************************+
						// parameters
						// **************************************************************************+

						// TODO: Add support for multiple triggered actions.
						//  At the moment, backend and frontend code only support one triggered action,
						// even though the data model has a ManyToMany rel between triggerElements and triggeredActions
						for (final ParameterMapping parameterMapping : triggeredAction.getProperty(ActionMapping.parameterMappings)) {

							final String parameterType = parameterMapping.getProperty(StructrApp.key(ParameterMapping.class, "parameterType"));
							final String parameterName = parameterMapping.getPropertyWithVariableReplacement(renderContext, StructrApp.key(ParameterMapping.class, "parameterName"));

							if (parameterType == null || parameterName == null) {
								// Ignore incomplete parameter mapping
								continue;
							}

							final String nameAttributeHyphenated = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, parameterName);

							switch (parameterType) {

								case "user-input":
									final DOMElement element   = parameterMapping.getProperty(ParameterMapping.inputElement);

									if (element != null) {

										final String elementCssId = element.getPropertyWithVariableReplacement(renderContext, DOMElement.htmlIdProperty);

										if (elementCssId != null) {

											out.append(" data-").append(nameAttributeHyphenated).append("=\"css(#").append(elementCssId).append(")\"");

										} else {

											out.append(" data-").append(nameAttributeHyphenated).append("=\"id(").append(element.getUuid()).append(")\"");
										}

									}
									break;

								case "constant-value":
									final String constantValue = parameterMapping.getProperty(ParameterMapping.constantValue);
									// Could be 'json(...)' or a simple value
									out.append(" data-").append(nameAttributeHyphenated).append("=\"").append(DOMNode.escapeForHtmlAttributes(constantValue)).append("\"");
									break;

								case "script-expression":
									final String scriptExpression = parameterMapping.getPropertyWithVariableReplacement(renderContext, ParameterMapping.scriptExpression);
									out.append(" data-").append(nameAttributeHyphenated).append("=\"").append(DOMNode.escapeForHtmlAttributes(scriptExpression)).append("\"");
									break;

								case "page-param":
									// Name of the request parameter for pager 'page'
									final String action = triggeredAction.getProperty(ActionMapping.actionProperty);
									final String value  = renderContext.getRequestParameter(parameterName);
									// special handling for pagination (migrated code)
									switch (action) {

										case "prev-page":
										case "previous-page":
											out.append(" data-structr-target=\"").append(parameterName).append("\"");
											final int prev = DOMElement.intOrOne(value);
											out.append(" data-").append(parameterName).append("=\"").append(String.valueOf(Math.max(1, prev - 1))).append("\"");
											break;

										case "next-page":
											out.append(" data-structr-target=\"").append(parameterName).append("\"");
											final int next = DOMElement.intOrOne(value);
											out.append(" data-").append(parameterName).append("=\"").append(String.valueOf(next + 1)).append("\"");
											break;

										case "first-page":
											out.append(" data-structr-target=\"").append(parameterName).append("\"");
											out.append(" data-").append(parameterName).append("=\"1\"");
											break;

										case "last-page":
											// should we really count all objects?
											out.append(" data-structr-target=\"").append(parameterName).append("\"");
											out.append(" data-").append(parameterName).append("=\"1000\"");
											break;

										default:
											break;
									}

									break;
								case "pagesize-param":
									// TODO: Implement additional parameter for page size
									// Name of the request parameter for pager 'pageSize'
									break;

								default:

							}
						}
					}

					if (isTargetElement(thisElementWithSuperuserContext)) {

						outputStructrId = true;

						// make current object ID available in reload targets
						final GraphObject current = renderContext.getDetailsDataObject();
						if (current != null) {

							out.append(" data-current-object-id=\"").append(current.getUuid()).append("\"");
						}

						// realization: all dynamic parameters must be stored on the reload target!
						final HttpServletRequest request = renderContext.getRequest();
						if (request != null) {

							final Map<String, String[]> parameters = request.getParameterMap();

							for (final Entry<String, String[]> entry : parameters.entrySet()) {

								final String key      = entry.getKey();
								final String[] values = entry.getValue();

								if (values.length > 0 && !RequestParameterBlacklist.contains(key)) {

									out.append(" data-request-").append(DOMElement.toHtmlAttributeName(key)).append("=\"").append(values[0]).append("\"");
								}
							}
						}

						final String encodedRenderState = renderContext.getEncodedRenderState();
						if (encodedRenderState != null) {

							out.append(" data-structr-render-state=\"").append(encodedRenderState).append("\"");
						}

					}

					if (getProperty(DOMElement.renderingModeProperty) != null) {

						out.append(" data-structr-delay-or-interval=\"").append(getProperty(DOMElement.delayOrIntervalProperty)).append("\"");

						outputStructrId = true;
					}

					if (renderContext.isTemplateRoot(uuid)) {

						// render template ID into output so it can be re-used
						out.append(" data-structr-template-id=\"").append(renderContext.getTemplateId()).append("\"");

					}

					final Iterable<? extends ParameterMapping> parameterMappings = thisElementWithSuperuserContext.getProperty(DOMElement.parameterMappingsProperty);

					outputStructrId |= (thisElementWithSuperuserContext instanceof TemplateElement || parameterMappings.iterator().hasNext());

					// output data-structr-id only once
					if (outputStructrId) {
						out.append(" data-structr-id=\"").append(uuid).append("\"");
					}

					break;
	 		}
		}

		return 1;
	}

	static String toHtmlAttributeName(final String camelCaseName) {

		final StringBuilder buf = new StringBuilder();

		camelCaseName.chars().forEach(c -> {

			if (Character.isUpperCase(c)) {

				buf.append("-");
				c = Character.toLowerCase(c);

			}

			buf.append(Character.toString(c));
		});

		return buf.toString();
	}

	static org.jsoup.nodes.Element getMatchElement(final DOMElement domElement) {

		final NodeInterface node = domElement;
		final Traits traits      = node.getTraits();
		final String tag         = domElement.getTag();

		if (StringUtils.isNotBlank(tag)) {

			final org.jsoup.nodes.Element element = new org.jsoup.nodes.Element(tag);
			final String classes                  = domElement.getCssClass();

			if (classes != null) {

				for (final String css : classes.split(" ")) {

					if (StringUtils.isNotBlank(css)) {

						element.addClass(css.trim());
					}
				}
			}

			final String name = node.getProperty(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY));
			if (name != null) {
				element.attr("name", name);
			}

			final String htmlId = node.getProperty(traits.key(DOMElementTraitDefinition._HTML_ID_PROPERTY));
			if (htmlId != null) {

				element.attr("id", htmlId);
			}

			return element;
		}

		return null;
	}
}
