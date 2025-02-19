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
package org.structr.web.traits.definitions.dom;

import com.google.common.base.CaseFormat;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.util.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.InstanceMethod;
import org.structr.core.api.Methods;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Relation;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.*;
import org.structr.core.script.Scripting;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.core.traits.operations.propertycontainer.GetPropertyKeys;
import org.structr.rest.api.RESTCall;
import org.structr.rest.servlet.AbstractDataServlet;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Actions;
import org.structr.schema.action.EvaluationHints;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.EventContext;
import org.structr.web.common.RenderContext;
import org.structr.web.common.RenderContext.EditMode;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.event.ActionMapping;
import org.structr.web.entity.event.ParameterMapping;
import org.structr.web.function.InsertHtmlFunction;
import org.structr.web.function.RemoveDOMChildFunction;
import org.structr.web.function.ReplaceDOMChildFunction;
import org.structr.web.resource.LoginResourceHandler;
import org.structr.web.resource.LogoutResourceHandler;
import org.structr.web.resource.RegistrationResourceHandler;
import org.structr.web.resource.ResetPasswordResourceHandler;
import org.structr.web.servlet.HtmlServlet;
import org.structr.web.traits.operations.*;
import org.structr.web.traits.wrappers.dom.DOMElementTraitWrapper;

import java.util.*;
import java.util.Map.Entry;

import static org.structr.web.entity.dom.DOMElement.lowercaseBodyName;
import static org.structr.web.entity.dom.DOMNode.EVENT_ACTION_MAPPING_CATEGORY;
import static org.structr.web.entity.dom.DOMNode.PAGE_CATEGORY;

public class DOMElementTraitDefinition extends AbstractNodeTraitDefinition {

	private static final Set<String> RequestParameterBlacklist = Set.of(HtmlServlet.ENCODED_RENDER_STATE_PARAMETER_NAME);

	/*
		final LicenseManager licenseManager = Services.getInstance().getLicenseManager();
		if (licenseManager == null || licenseManager.isModuleLicensed("api-builder")) {

			type.addViewProperty(PropertyView.Public, "flow");
			type.addViewProperty(PropertyView.Ui, "flow");
		}

		type.addPropertyGetter("tag", String.class);

		type.overrideMethod("onCreation",             true,  DOMElement.class.getName() + ".onCreation(this, arg0, arg1);");
		type.overrideMethod("onModification",         true,  DOMElement.class.getName() + ".onModification(this, arg0, arg1, arg2);");

		type.overrideMethod("updateFromNode",         false, DOMElement.class.getName() + ".updateFromNode(this, arg0);");
		type.overrideMethod("getHtmlAttributes",      false, "return _html_View.properties();");
		type.overrideMethod("getHtmlAttributeNames",  false, "return " + DOMElement.class.getName() + ".getHtmlAttributeNames(this);");
		type.overrideMethod("getOffsetAttributeName", false, "return " + DOMElement.class.getName() + ".getOffsetAttributeName(this, arg0, arg1);");
		type.overrideMethod("getLocalName",           false, "return null;");
		type.overrideMethod("getAttributes",          false, "return this;");
		type.overrideMethod("contentEquals",          false, "return false;");
		type.overrideMethod("hasAttributes",          false, "return getLength() > 0;");
		type.overrideMethod("getLength",              false, "return getHtmlAttributeNames().size();");
		type.overrideMethod("getTagName",             false, "return getTag();");
		type.overrideMethod("getNodeName",            false, "return getTagName();");
		type.overrideMethod("setNodeValue",           false, "");
		type.overrideMethod("getNodeValue",           false, "return null;");
		type.overrideMethod("getNodeType",            false, "return ELEMENT_NODE;");
		type.overrideMethod("getPropertyKeys",        false, "final Set<PropertyKey> allProperties = new LinkedHashSet<>(); final Set<PropertyKey> htmlAttrs = super.getPropertyKeys(arg0); for (final PropertyKey attr : htmlAttrs) { allProperties.add(attr); } allProperties.addAll(getDataPropertyKeys()); return allProperties;");
		type.overrideMethod("getCssClass",            false, "return getProperty(_html_classProperty);");

		type.overrideMethod("openingTag",             false, DOMElement.class.getName() + ".openingTag(this, arg0, arg1, arg2, arg3, arg4);");
		type.overrideMethod("renderContent",          false, DOMElement.class.getName() + ".renderContent(this, arg0, arg1);");
		type.overrideMethod("setIdAttribute",         false, DOMElement.class.getName() + ".setIdAttribute(this, arg0, arg1);");
		type.overrideMethod("setAttribute",           false, DOMElement.class.getName() + ".setAttribute(this, arg0, arg1);");
		type.overrideMethod("removeAttribute",        false, DOMElement.class.getName() + ".removeAttribute(this, arg0);");
		type.overrideMethod("doImport",               false, "return "+ DOMElement.class.getName() + ".doImport(this, arg0);");
		type.overrideMethod("getAttribute",           false, "return " + DOMElement.class.getName() + ".getAttribute(this, arg0);");
		type.overrideMethod("getAttributeNode",       false, "return " + DOMElement.class.getName() + ".getAttributeNode(this, arg0);");
		type.overrideMethod("setAttributeNode",       false, "return " + DOMElement.class.getName() + ".setAttributeNode(this, arg0);");
		type.overrideMethod("removeAttributeNode",    false, "return " + DOMElement.class.getName() + ".removeAttributeNode(this, arg0);");
		type.overrideMethod("getElementsByTagName",   false, "return " + DOMElement.class.getName() + ".getElementsByTagName(this, arg0);");
		type.overrideMethod("setIdAttributeNS",       false, "throw new UnsupportedOperationException(\"Namespaces not supported.\");");
		type.overrideMethod("setIdAttributeNode",     false, "throw new UnsupportedOperationException(\"Attribute nodes not supported in HTML5.\");");
		type.overrideMethod("hasAttribute",           false, "return getAttribute(arg0) != null;");
		type.overrideMethod("hasAttributeNS",         false, "return false;");
		type.overrideMethod("getAttributeNS",         false, "");
		type.overrideMethod("setAttributeNS",         false, "");
		type.overrideMethod("removeAttributeNS",      false, "");
		type.overrideMethod("getAttributeNodeNS",     false, "return null;");
		type.overrideMethod("setAttributeNodeNS",     false, "return null;");
		type.overrideMethod("getElementsByTagNameNS", false, "return null;");

		// NamedNodeMap
		type.overrideMethod("getNamedItemNS",         false, "return null;");
		type.overrideMethod("setNamedItemNS",         false, "return null;");
		type.overrideMethod("removeNamedItemNS",      false, "return null;");
		type.overrideMethod("getNamedItem",           false, "return getAttributeNode(arg0);");
		type.overrideMethod("setNamedItem",           false, "return " + DOMElement.class.getName() + ".setNamedItem(this, arg0);");
		type.overrideMethod("removeNamedItem",        false, "return " + DOMElement.class.getName() + ".removeNamedItem(this, arg0);");
		type.overrideMethod("getContextName",         false, "return " + DOMElement.class.getName() + ".getContextName(this);");
		type.overrideMethod("item",                   false, "return " + DOMElement.class.getName() + ".item(this, arg0);");

		// W3C Element
		type.overrideMethod("getSchemaTypeInfo",      false, "return null;");

		// view configuration
		type.addViewProperty(PropertyView.Public, "isDOMNode");
		type.addViewProperty(PropertyView.Public, "pageId");
		type.addViewProperty(PropertyView.Public, "parent");
		type.addViewProperty(PropertyView.Public, "sharedComponentId");
		type.addViewProperty(PropertyView.Public, "syncedNodesIds");
		type.addViewProperty(PropertyView.Public, "name");
		type.addViewProperty(PropertyView.Public, "children");
		type.addViewProperty(PropertyView.Public, "dataKey");
		type.addViewProperty(PropertyView.Public, "cypherQuery");
		type.addViewProperty(PropertyView.Public, "xpathQuery");
		type.addViewProperty(PropertyView.Public, "restQuery");
		type.addViewProperty(PropertyView.Public, "functionQuery");

		type.addViewProperty(PropertyView.Ui, "hideOnDetail");
		type.addViewProperty(PropertyView.Ui, "hideOnIndex");
		type.addViewProperty(PropertyView.Ui, "sharedComponentConfiguration");
		type.addViewProperty(PropertyView.Ui, "isDOMNode");
		type.addViewProperty(PropertyView.Ui, "pageId");
		type.addViewProperty(PropertyView.Ui, "parent");
		type.addViewProperty(PropertyView.Ui, "sharedComponentId");
		type.addViewProperty(PropertyView.Ui, "syncedNodesIds");
		type.addViewProperty(PropertyView.Ui, "data-structr-id");
		type.addViewProperty(PropertyView.Ui, "renderDetails");
		type.addViewProperty(PropertyView.Ui, "children");
		type.addViewProperty(PropertyView.Ui, "childrenIds");
		type.addViewProperty(PropertyView.Ui, "showForLocales");
		type.addViewProperty(PropertyView.Ui, "hideForLocales");
		type.addViewProperty(PropertyView.Ui, "showConditions");
		type.addViewProperty(PropertyView.Ui, "hideConditions");
		type.addViewProperty(PropertyView.Ui, "dataKey");
		type.addViewProperty(PropertyView.Ui, "cypherQuery");
		type.addViewProperty(PropertyView.Ui, "xpathQuery");
		type.addViewProperty(PropertyView.Ui, "restQuery");
		type.addViewProperty(PropertyView.Ui, "functionQuery");

		final LicenseManager licenseManager = Services.getInstance().getLicenseManager();
		if (licenseManager == null || licenseManager.isModuleLicensed("api-builder")) {

			type.addViewProperty(PropertyView.Public, "flow");
			type.addViewProperty(PropertyView.Ui, "flow");
		}

	}}

	String getTag();
	String getOffsetAttributeName(final String name, final int offset);

	void openingTag(final AsyncBuffer out, final String tag, final EditMode editMode, final RenderContext renderContext, final int depth) throws FrameworkException;

	Property[] getHtmlAttributes();
	List<String> getHtmlAttributeNames();
	String getEventMapping();
	*/


	public DOMElementTraitDefinition() {
		super("DOMElement");
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(

			OnCreation.class,
			new OnCreation() {

				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {
					updateReloadTargets(graphObject.as(DOMElement.class));
				}
			},

			OnModification.class,
			new OnModification() {

				@Override
				public void onModification(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {
					updateReloadTargets(graphObject.as(DOMElement.class));
				}
			}
		);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			GetPropertyKeys.class,
			new GetPropertyKeys() {

				@Override
				public Set<PropertyKey> getPropertyKeys(final GraphObject graphObject, final String propertyView) {

					final Set<PropertyKey> htmlAttrs = getSuper().getPropertyKeys(graphObject, propertyView);
					final Set<PropertyKey> allProperties = new LinkedHashSet<>();

					for (final PropertyKey attr : htmlAttrs) {
						allProperties.add(attr);
					}

					final DOMNode domElement = graphObject.as(DOMNode.class);

					allProperties.addAll(domElement.getDataPropertyKeys());

					return allProperties;
				}
			},

			GetContextName.class,
			new GetContextName() {

				@Override
				public String getContextName(final NodeInterface node) {

					final Traits traits = node.getTraits();
					final String _name = node.getProperty(traits.key("name"));

					if (_name != null) {

						return _name;
					}

					final DOMElement elem = node.as(DOMElement.class);

					return elem.getTag();
				}
			},

			GetCssClass.class,
			new GetCssClass() {

				@Override
				public String getCssClass(final NodeInterface node) {

					final Traits traits = node.getTraits();

					return node.getProperty(traits.key("_html_class"));
				}
			},

			UpdateFromNode.class,
			new UpdateFromNode() {

				@Override
				public void updateFromNode(final NodeInterface thisNode, final DOMNode node2) throws FrameworkException {

					final PropertyMap properties = new PropertyMap();
					final NodeInterface wrapped = node2;
					final DOMElement newNode = wrapped.as(DOMElement.class);
					final Traits traits = wrapped.getTraits();

					for (final PropertyKey htmlProp : newNode.getHtmlAttributes()) {
						properties.put(htmlProp, wrapped.getProperty(htmlProp));
					}

					// copy tag
					properties.put(traits.key("tag"), newNode.getTag());

					thisNode.setProperties(thisNode.getSecurityContext(), properties);
				}
			},



			DoImport.class,
			new DoImport() {

				@Override
				public DOMElement doImport(final DOMNode node, final Page newPage) throws FrameworkException {

					final DOMElement element        = node.as(DOMElement.class);
					final NodeInterface wrappedThis = node;
					final DOMElement newElement     = newPage.createElement(element.getTag());
					final NodeInterface wrappedNew  = newElement;

					// copy attributes
					for (PropertyKey key : element.getHtmlAttributes()) {

						wrappedNew.setProperty(key, wrappedThis.getProperty(key));
					}

					return newElement;
				}
			},

			RenderContent.class,
			new RenderContent() {

				@Override
				public void renderContent(final DOMNode node, final RenderContext renderContext, final int depth) throws FrameworkException {

					final DOMElement elem = node.as(DOMElement.class);

					if (!elem.shouldBeRendered(renderContext)) {
						return;
					}

					// final variables
					final SecurityContext securityContext = renderContext.getSecurityContext();
					final AsyncBuffer out                 = renderContext.getBuffer();
					final EditMode editMode               = renderContext.getEditMode(securityContext.getUser(false));
					final boolean hasSharedComponent      = elem.hasSharedComponent();
					final DOMNode synced                  = hasSharedComponent ? elem.getSharedComponent() : null;
					final boolean isVoid                  = elem.isVoidElement();
					final String _tag                     = elem.getTag();

					// non-final variables
					boolean anyChildNodeCreatesNewLine = false;

					if (depth > 0 && !elem.avoidWhitespace()) {

						out.append(DOMNode.indent(depth, renderContext));

					}

					if (StringUtils.isNotBlank(_tag)) {

						if (EditMode.DEPLOYMENT.equals(editMode)) {

							// Determine if this element's visibility flags differ from
							// the flags of the page and render a <!-- @structr:private -->
							// comment accordingly.
							if (elem.renderDeploymentExportComments(out, false)) {

								// restore indentation
								if (depth > 0 && !elem.avoidWhitespace()) {
									out.append(DOMNode.indent(depth, renderContext));
								}
							}
						}

						elem.openingTag(out, _tag, editMode, renderContext, depth);

						try {

							// in body?
							if (lowercaseBodyName.equals(elem.getTag())) {
								renderContext.setInBody(true);
							}

							final String renderingMode = elem.getRenderingMode();
							boolean lazyRendering = false;

							// lazy rendering can only work if this node is not requested as a partial
							if (renderContext.getPage() != null && renderingMode != null) {
								lazyRendering = true;
							}

							// disable lazy rendering in deployment mode
							if (EditMode.DEPLOYMENT.equals(editMode)) {
								lazyRendering = false;
							}

							// only render children if we are not in a shared component scenario, not in deployment mode and it's not rendered lazily
							if (!lazyRendering && (synced == null || !EditMode.DEPLOYMENT.equals(editMode))) {

								// fetch children
								final List<RelationshipInterface> rels = elem.getChildRelationships();
								if (rels.isEmpty()) {

									// No child relationships, maybe this node is in sync with another node
									if (synced != null) {

										DOMNode.prefetchDOMNodes(synced.getUuid());

										rels.addAll(synced.getChildRelationships());
									}
								}

								// apply configuration for shared component if present
								final String _sharedComponentConfiguration = elem.getSharedComponentConfiguration();
								if (StringUtils.isNotBlank(_sharedComponentConfiguration)) {

									Scripting.evaluate(renderContext, elem, "${" + _sharedComponentConfiguration.trim() + "}", "sharedComponentConfiguration", node.getUuid());
								}

								for (final RelationshipInterface rel : rels) {

									final DOMNode subNode = rel.getTargetNode().as(DOMNode.class);

									if (subNode.is("DOMElement")) {
										anyChildNodeCreatesNewLine = (anyChildNodeCreatesNewLine || !(subNode.avoidWhitespace()));
									}

									subNode.render(renderContext, depth + 1);

								}

							}

						} catch (Throwable t) {

							out.append("Error while rendering node ").append(elem.getUuid()).append(": ").append(t.getMessage());

							final Logger logger = LoggerFactory.getLogger(DOMElement.class);
							logger.warn("", t);
						}

						// render end tag, if needed (= if not singleton tags)
						if (StringUtils.isNotBlank(_tag) && (!isVoid) || (isVoid && synced != null && EditMode.DEPLOYMENT.equals(editMode))) {

							// only insert a newline + indentation before the closing tag if any child-element used a newline
							final boolean isTemplate = synced != null && EditMode.DEPLOYMENT.equals(editMode);

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
			},

			OpeningTag.class,
			new OpeningTag() {

				@Override
				public void openingTag(final DOMElement node, final AsyncBuffer out, final String tag, final EditMode editMode, final RenderContext renderContext, final int depth) throws FrameworkException {

					final boolean hasSharedComponent      = node.hasSharedComponent();
					final DOMNode _sharedComponentElement = hasSharedComponent ? node.getSharedComponent() : null;
					final NodeInterface wrappedNode       = node;
					final Traits traits                   = wrappedNode.getTraits();

					if (_sharedComponentElement != null && EditMode.DEPLOYMENT.equals(editMode)) {

						out.append("<structr:component src=\"");

						final String _name = _sharedComponentElement.getProperty(traits.key("name"));
						out.append(_name != null ? _name.concat("-").concat(_sharedComponentElement.getUuid()) : _sharedComponentElement.getUuid());

						out.append("\"");

						node.renderSharedComponentConfiguration(out, editMode);

						// include data-* attributes in template
						node.renderCustomAttributes(out, renderContext.getSecurityContext(), renderContext);

					} else {

						out.append("<").append(tag);

						final String uuid = node.getUuid();

						final List<PropertyKey> htmlAttributes = new ArrayList<>();

						wrappedNode.getNode().getPropertyKeys().forEach((key) -> {
							if (key.startsWith(PropertyView.Html)) {
								htmlAttributes.add(traits.key(key));
							}
						});

						if (EditMode.DEPLOYMENT.equals(editMode)) {
							Collections.sort(htmlAttributes);
						}

						for (PropertyKey attribute : htmlAttributes) {

							String value = null;

							if (EditMode.DEPLOYMENT.equals(editMode)) {

								value = (String) wrappedNode.getProperty(attribute);

							} else {

								value = wrappedNode.getPropertyWithVariableReplacement(renderContext, attribute);
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
						if (repeaterDataObject != null && StringUtils.isNotBlank(node.getDataKey())) {

							out.append(" data-repeater-data-object-id=\"").append(repeaterDataObject.getUuid()).append("\"");
						}

						// include arbitrary data-* attributes
						node.renderSharedComponentConfiguration(out, editMode);
						node.renderCustomAttributes(out, renderContext.getSecurityContext(), renderContext);

						// new: managed attributes (like selected
						node.renderManagedAttributes(out, renderContext.getSecurityContext(), renderContext);

						// include special mode attributes
						switch (editMode) {

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

								out.append(" ").append("data-structr-hash").append("=\"").append(node.getIdHash()).append("\"");
								break;

							case WIDGET:
							case DEPLOYMENT:

								final String eventMapping = node.getEventMapping();
								if (eventMapping != null) {

									out.append(" ").append("data-structr-meta-event-mapping").append("=\"").append(StringEscapeUtils.escapeHtml(eventMapping)).append("\"");
								}
								break;

							case NONE:

								// Get actions in superuser context
								final DOMElement thisElementWithSuperuserContext = StructrApp.getInstance().getNodeById("DOMElement", uuid).as(DOMElement.class);
								final Iterable<ActionMapping> triggeredActions   = thisElementWithSuperuserContext.getTriggeredActions();
								final List<ActionMapping> list                   = Iterables.toList(triggeredActions);
								boolean outputStructrId = false;

								if (!list.isEmpty()) {

									// all active elements need data-structr-id
									outputStructrId = true;

									// why only the first one?!
									final ActionMapping triggeredAction = list.get(0);
									final NodeInterface actionNode      = triggeredAction;
									final String options                = triggeredAction.getOptions();
									final Traits eamTraits              = actionNode.getTraits();

									// support for configuration options
									if (StringUtils.isNotBlank(options)) {
										out.append(" data-structr-options=\"").append(uuid).append("\"");
									}

									String eventsString = null;
									final Map<String, Object> mapping = node.getMappedEvents();
									if (mapping != null) {
										eventsString = StringUtils.join(mapping.keySet(), ",");
									}

									// append all stored action mapping keys as data-structr-<key> attributes
									for (final String key : Set.of("event", "action", "method", "dataType", "idExpression")) {

										final String value = actionNode.getPropertyWithVariableReplacement(renderContext, eamTraits.key(key));
										if (StringUtils.isNotBlank(value)) {

											final String keyHyphenated = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, key);
											out.append(" data-structr-" + keyHyphenated + "=\"").append(value).append("\"");
										}

										if (key.equals("event")) {

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

									/*
									{ // TODO: Migrate tree handling to new action mapping
										// toggle-tree-item
										if (mapping.containsValue("toggle-tree-item")) {

											final String targetValue = thisElement.getPropertyWithVariableReplacement(renderContext, targetKey);
											final String key = thisElement.getTreeItemSessionIdentifier(targetValue);
											final boolean open = thisElement.getSessionAttribute(renderContext.getSecurityContext(), key) != null;

											out.append(" data-tree-item-state=\"").append(open ? "open" : "closed").append("\"");
										}
									}
									*/

									final Traits parameterMappingTraits = Traits.of("ParameterMapping");
									final PropertyKey<String> scriptExpressionKey = parameterMappingTraits.key("scriptExpression");
									final PropertyKey<String> parameterTypeKey = parameterMappingTraits.key("parameterType");
									final PropertyKey<String> parameterNameKey = parameterMappingTraits.key("parameterName");
									final PropertyKey<String> htmlIdKey = traits.key("_html_id");


									// **************************************************************************+
									// parameters
									// **************************************************************************+

									// TODO: Add support for multiple triggered actions.
									//  At the moment, backend and frontend code only support one triggered action,
									// even though the data model has a ManyToMany rel between triggerElements and triggeredActions
									for (final ParameterMapping parameterMapping : triggeredAction.getParameterMappings()) {

										final NodeInterface parameterMappingNode = parameterMapping;
										final String parameterType = parameterMappingNode.getProperty(parameterTypeKey);
										final String parameterName = parameterMappingNode.getPropertyWithVariableReplacement(renderContext, parameterNameKey);

										if (parameterType == null || parameterName == null) {
											// Ignore incomplete parameter mapping
											continue;
										}

										final String nameAttributeHyphenated = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, parameterName);

										switch (parameterType) {

											case "user-input":

												final DOMElement element = parameterMapping.getInputElement();
												if (element != null) {

													final String elementCssId = element.getPropertyWithVariableReplacement(renderContext, htmlIdKey);

													if (elementCssId != null) {

														out.append(" data-").append(nameAttributeHyphenated).append("=\"css(#").append(elementCssId).append(")\"");

													} else {

														out.append(" data-").append(nameAttributeHyphenated).append("=\"id(").append(element.getUuid()).append(")\"");
													}

												}
												break;

											case "constant-value":
												final String constantValue = parameterMapping.getConstantValue();
												// Could be 'json(...)' or a simple value
												out.append(" data-").append(nameAttributeHyphenated).append("=\"").append(DOMNode.escapeForHtmlAttributes(constantValue)).append("\"");
												break;

											case "script-expression":
												final String scriptExpression = parameterMappingNode.getPropertyWithVariableReplacement(renderContext, scriptExpressionKey);
												out.append(" data-").append(nameAttributeHyphenated).append("=\"").append(DOMNode.escapeForHtmlAttributes(scriptExpression)).append("\"");
												break;

											case "page-param":
												// Name of the request parameter for pager 'page'
												final String action = triggeredAction.getAction();
												final String value = renderContext.getRequestParameter(parameterName);
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

											final String key = entry.getKey();
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

								if (node.getRenderingMode() != null) {

									out.append(" data-structr-delay-or-interval=\"").append(node.getDelayOrInterval()).append("\"");

									outputStructrId = true;
								}

								if (renderContext.isTemplateRoot(uuid)) {

									// render template ID into output so it can be re-used
									out.append(" data-structr-template-id=\"").append(renderContext.getTemplateId()).append("\"");

								}

								final Iterable<ParameterMapping> parameterMappings = thisElementWithSuperuserContext.getParameterMappings();

								outputStructrId |= (thisElementWithSuperuserContext.is("TemplateElement") || parameterMappings.iterator().hasNext());

								// output data-structr-id only once
								if (outputStructrId) {
									out.append(" data-structr-id=\"").append(uuid).append("\"");
								}

								break;
						}
					}

					out.append(">");
				}
			},

			GetAttributes.class,
			new GetAttributes() {

				@Override
				public Iterable<PropertyKey> getHtmlAttributes(final DOMElement element) {
					return element.getTraits().getPropertyKeysForView(PropertyView.Html);
				}

				@Override
				public List<String> getHtmlAttributeNames(final DOMElement element) {

					final List<String> names = new ArrayList<>(20);
					final NodeInterface node = element;
					final int len = PropertyView.Html.length();

					for (String key : node.getNode().getPropertyKeys()) {

						// use html properties only
						if (key.startsWith(PropertyView.Html)) {

							names.add(key.substring(len));
						}
					}

					return names;
				}
			}
		);
	}

	@Override
	public Map<Class, RelationshipTraitFactory> getRelationshipTraitFactories() {
		return Map.of();
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			DOMElement.class, (traits, node) -> new DOMElementTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {

		return Set.of(

			new InstanceMethod("DOMElement", "event") {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Map<String, Object> parameters) throws FrameworkException {

					final ActionContext actionContext = new ActionContext(securityContext);
					final EventContext  eventContext  = new EventContext();
					final String        event         = (String) parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_HTMLEVENT);
					final String        action;

					if (event == null) {
						throw new FrameworkException(422, "Cannot execute action without event name (htmlEvent property).");
					}

					ActionMapping triggeredAction;
					final NodeInterface domElementNode         = StructrApp.getInstance().getNodeById("DOMElement", entity.getUuid());
					final DOMElement domElement                = domElementNode.as(DOMElement.class);
					final List<ActionMapping> triggeredActions = Iterables.toList(domElement.getTriggeredActions());

					if (triggeredActions != null && !triggeredActions.isEmpty()) {

						triggeredAction = triggeredActions.get(0);
						action          = triggeredAction.getAction();

					} else {

						throw new FrameworkException(422, "Cannot execute action without action defined on this DOMElement: " + this);
					}

					// store event context in object
					actionContext.setConstant("eventContext", eventContext);

					switch (action) {

						case "create":
							return handleCreateAction(actionContext, domElementNode, parameters, eventContext);

						case "update":
							handleUpdateAction(actionContext, domElementNode, parameters, eventContext);
							break;

						case "delete":
							handleDeleteAction(actionContext, domElementNode, parameters, eventContext);
							break;

						case "append-child":
							handleAppendChildAction(actionContext, domElementNode, parameters, eventContext);
							break;

						case "remove-child":
							handleRemoveChildAction(actionContext, domElementNode, parameters, eventContext);
							break;

						case "insert-html":
							return handleInsertHtmlAction(actionContext, domElementNode, parameters, eventContext);

						case "replace-html":
							return handleReplaceHtmlAction(actionContext, domElementNode, parameters, eventContext);

						/*
						case "open-tree-item":
						case "close-tree-item":
						case "toggle-tree-item":
							handleTreeAction(actionContext, parameters, eventContext, event);
							break;
						*/

						case "sign-in":
							return handleSignInAction(actionContext, domElementNode, parameters, eventContext);

						case "sign-out":
							return handleSignOutAction(actionContext, domElementNode, parameters, eventContext);

						case "sign-up":
							return handleSignUpAction(actionContext, domElementNode, parameters, eventContext);

						case "reset-password":
							return handleResetPasswordAction(actionContext, domElementNode, parameters, eventContext);

						case "method":
						default:
							// execute custom method (and return the result directly)
							final String method = (String) parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRMETHOD);
							return handleCustomAction(actionContext, domElementNode, parameters, eventContext, method);
					}

					return eventContext;
				}
			}
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Iterable<NodeInterface>> reloadSourcesProperty     = new StartNodes("reloadSources", "DOMElementRELOADSDOMElement");
		final Property<Iterable<NodeInterface>> reloadTargetsProperty     = new EndNodes("reloadTargets", "DOMElementRELOADSDOMElement");
		final Property<Iterable<NodeInterface>> triggeredActionsProperty  = new EndNodes("triggeredActions", "DOMElementTRIGGERED_BYActionMapping");
		final Property<Iterable<NodeInterface>> parameterMappingsProperty = new EndNodes("parameterMappings", "DOMElementINPUT_ELEMENTParameterMapping");

		final Property<String> tagProperty              = new StringProperty("tag").indexed().category(PAGE_CATEGORY);
		final Property<String> pathProperty             = new StringProperty("path").indexed();
		final Property<String> partialUpdateKeyProperty = new StringProperty("partialUpdateKey").indexed();

		final Property<Boolean> manualReloadTargetProperty = new BooleanProperty("data-structr-manual-reload-target").category(EVENT_ACTION_MAPPING_CATEGORY).hint("Identifies this element as a manual reload target, this is necessary when using repeaters as reload targets.");
		final Property<Boolean> fromWidgetProperty         = new BooleanProperty("fromWidget");
		final Property<Boolean> dataInsertProperty         = new BooleanProperty("data-structr-insert");
		final Property<Boolean> dataFromWidgetProperty     = new BooleanProperty("data-structr-from-widget");

		final Property<String> eventMappingProperty       = new StringProperty("eventMapping").category(EVENT_ACTION_MAPPING_CATEGORY).hint("A mapping between the desired Javascript event (click, drop, dragOver, ...) and the server-side event that should be triggered: (create | update | delete | <method name>).");
		// probably useless ATM because EAM does not support trees yet
		final Property<String> dataTreeChildrenProperty   = new StringProperty("data-structr-tree-children").category(EVENT_ACTION_MAPPING_CATEGORY).hint("Toggles automatic visibility for tree child items when the 'toggle-tree-item' event is mapped. This field must contain the data key on which the tree is based, e.g. 'item'.");
		final Property<String> dataReloadTargetProperty   = new StringProperty("data-structr-reload-target").category(EVENT_ACTION_MAPPING_CATEGORY).hint("CSS selector that specifies which partials to reload.");
		final Property<String> renderingModeProperty      = new StringProperty("data-structr-rendering-mode").category(EVENT_ACTION_MAPPING_CATEGORY).hint("Rendering mode, possible values are empty (default for eager rendering), 'load' to render when the DOM document has finished loading, 'delayed' like 'load' but with a fixed delay, 'visible' to render when the element comes into view and 'periodic' to render the element with periodic updates with a given interval");
		final Property<String> delayOrIntervalProperty    = new StringProperty("data-structr-delay-or-interval").category(EVENT_ACTION_MAPPING_CATEGORY).hint("Delay or interval in milliseconds for 'delayed' or 'periodic' rendering mode");
		final Property<String> onAbortProperty            = new StringProperty("_html_onabort");
		final Property<String> onBlurProperty             = new StringProperty("_html_onblur");
		final Property<String> onCanPlayProperty          = new StringProperty("_html_oncanplay");
		final Property<String> onCanPlayThroughProperty   = new StringProperty("_html_oncanplaythrough");
		final Property<String> onChangeProperty           = new StringProperty("_html_onchange");
		final Property<String> onClickProperty            = new StringProperty("_html_onclick");
		final Property<String> onContextMenuProperty      = new StringProperty("_html_oncontextmenu");
		final Property<String> onDblClickProperty         = new StringProperty("_html_ondblclick");
		final Property<String> onDragProperty             = new StringProperty("_html_ondrag");
		final Property<String> onDragEndProperty          = new StringProperty("_html_ondragend");
		final Property<String> onDragEnterProperty        = new StringProperty("_html_ondragenter");
		final Property<String> onDragLeaveProperty        = new StringProperty("_html_ondragleave");
		final Property<String> onDragOverProperty         = new StringProperty("_html_ondragover");
		final Property<String> onDragStartProperty        = new StringProperty("_html_ondragstart");
		final Property<String> onDropProperty             = new StringProperty("_html_ondrop");
		final Property<String> onDurationChangeProperty   = new StringProperty("_html_ondurationchange");
		final Property<String> onEmptiedProperty          = new StringProperty("_html_onemptied");
		final Property<String> onEndedProperty            = new StringProperty("_html_onended");
		final Property<String> onErrorProperty            = new StringProperty("_html_onerror");
		final Property<String> onFocusProperty            = new StringProperty("_html_onfocus");
		final Property<String> onInputProperty            = new StringProperty("_html_oninput");
		final Property<String> onInvalidProperty          = new StringProperty("_html_oninvalid");
		final Property<String> onKeyDownProperty          = new StringProperty("_html_onkeydown");
		final Property<String> onKeyPressProperty         = new StringProperty("_html_onkeypress");
		final Property<String> onKeyUpProperty            = new StringProperty("_html_onkeyup");
		final Property<String> onLoadProperty             = new StringProperty("_html_onload");
		final Property<String> onLoadedDataProperty       = new StringProperty("_html_onloadeddata");
		final Property<String> onLoadedMetadataProperty   = new StringProperty("_html_onloadedmetadata");
		final Property<String> onLoadStartProperty        = new StringProperty("_html_onloadstart");
		final Property<String> onMouseDownProperty        = new StringProperty("_html_onmousedown");
		final Property<String> onMouseMoveProperty        = new StringProperty("_html_onmousemove");
		final Property<String> onMouseOutProperty         = new StringProperty("_html_onmouseout");
		final Property<String> onMouseOverProperty        = new StringProperty("_html_onmouseover");
		final Property<String> onMouseUpProperty          = new StringProperty("_html_onmouseup");
		final Property<String> onMouseWheelProperty       = new StringProperty("_html_onmousewheel");
		final Property<String> onPauseProperty            = new StringProperty("_html_onpause");
		final Property<String> onPlayProperty             = new StringProperty("_html_onplay");
		final Property<String> onPlayingProperty          = new StringProperty("_html_onplaying");
		final Property<String> onProgressProperty         = new StringProperty("_html_onprogress");
		final Property<String> onRateChangeProperty       = new StringProperty("_html_onratechange");
		final Property<String> onReadyStateChangeProperty = new StringProperty("_html_onreadystatechange");
		final Property<String> onResetProperty            = new StringProperty("_html_onreset");
		final Property<String> onScrollProperty           = new StringProperty("_html_onscroll");
		final Property<String> onSeekedProperty           = new StringProperty("_html_onseeked");
		final Property<String> onSeekingProperty          = new StringProperty("_html_onseeking");
		final Property<String> onSelectProperty           = new StringProperty("_html_onselect");
		final Property<String> onShowProperty             = new StringProperty("_html_onshow");
		final Property<String> onStalledProperty          = new StringProperty("_html_onstalled");
		final Property<String> onSubmitProperty           = new StringProperty("_html_onsubmit");
		final Property<String> onSuspendProperty          = new StringProperty("_html_onsuspend");
		final Property<String> onTimeUpdateProperty       = new StringProperty("_html_ontimeupdate");
		final Property<String> onVolumechangeProperty     = new StringProperty("_html_onvolumechange");
		final Property<String> onWaitingProperty          = new StringProperty("_html_onwaiting");
		final Property<String> htmlDataProperty           = new StringProperty("_html_data");

		// Core attributes
		final Property<String> htmlAcceskeyProperty        = new StringProperty("_html_accesskey");
		final Property<String> htmlClassProperty           = new StringProperty("_html_class");
		final Property<String> htmlContentEditableProperty = new StringProperty("_html_contenteditable");
		final Property<String> htmlContextMenuProperty     = new StringProperty("_html_contextmenu");
		final Property<String> htmlDirProperty             = new StringProperty("_html_dir");
		final Property<String> htmlDraggableProperty       = new StringProperty("_html_draggable");
		final Property<String> htmlDropzoneProperty        = new StringProperty("_html_dropzone");
		final Property<String> htmlHiddenProperty          = new StringProperty("_html_hidden");
		final Property<String> htmlIdProperty              = new StringProperty("_html_id").indexed();
		final Property<String> htmlLangProperty            = new StringProperty("_html_lang");
		final Property<String> htmlSpellcheckProperty      = new StringProperty("_html_spellcheck");
		final Property<String> htmlStyleProperty           = new StringProperty("_html_style");
		final Property<String> htmlTabindexProperty        = new StringProperty("_html_tabindex");
		final Property<String> htmlTitleProperty           = new StringProperty("_html_title");
		final Property<String> htmlTranslateProperty       = new StringProperty("_html_translate");

		// new properties for Polymer support
		final Property<String> htmlIsProperty         = new StringProperty("_html_is");
		final Property<String> htmlPropertiesProperty = new StringProperty("_html_properties");

		// The role attribute, see http://www.w3.org/TR/role-attribute/
		final Property<String> htmlRoleProperty            = new StringProperty("_html_role");

		return Set.of(
			reloadSourcesProperty,
			reloadTargetsProperty,
			triggeredActionsProperty,
			parameterMappingsProperty,
			tagProperty,
			pathProperty,
			partialUpdateKeyProperty,
			manualReloadTargetProperty,
			fromWidgetProperty,
			dataInsertProperty,
			dataFromWidgetProperty,
			eventMappingProperty,
			dataTreeChildrenProperty,
			dataReloadTargetProperty,
			renderingModeProperty,
			delayOrIntervalProperty,
			onAbortProperty,
			onBlurProperty,
			onCanPlayProperty,
			onCanPlayThroughProperty,
			onChangeProperty,
			onClickProperty,
			onContextMenuProperty,
			onDblClickProperty,
			onDragProperty,
			onDragEndProperty,
			onDragEnterProperty,
			onDragLeaveProperty,
			onDragOverProperty,
			onDragStartProperty,
			onDropProperty,
			onDurationChangeProperty,
			onEmptiedProperty,
			onEndedProperty,
			onErrorProperty,
			onFocusProperty,
			onInputProperty,
			onInvalidProperty,
			onKeyDownProperty,
			onKeyPressProperty,
			onKeyUpProperty,
			onLoadProperty,
			onLoadedDataProperty,
			onLoadedMetadataProperty,
			onLoadStartProperty,
			onMouseDownProperty,
			onMouseMoveProperty,
			onMouseOutProperty,
			onMouseOverProperty,
			onMouseUpProperty,
			onMouseWheelProperty,
			onPauseProperty,
			onPlayProperty,
			onPlayingProperty,
			onProgressProperty,
			onRateChangeProperty,
			onReadyStateChangeProperty,
			onResetProperty,
			onScrollProperty,
			onSeekedProperty,
			onSeekingProperty,
			onSelectProperty,
			onShowProperty,
			onStalledProperty,
			onSubmitProperty,
			onSuspendProperty,
			onTimeUpdateProperty,
			onVolumechangeProperty,
			onWaitingProperty,
			htmlDataProperty,
			htmlAcceskeyProperty,
			htmlClassProperty,
			htmlContentEditableProperty,
			htmlContextMenuProperty,
			htmlDirProperty,
			htmlDraggableProperty,
			htmlDropzoneProperty,
			htmlHiddenProperty,
			htmlIdProperty,
			htmlLangProperty,
			htmlSpellcheckProperty,
			htmlStyleProperty,
			htmlTabindexProperty,
			htmlTitleProperty,
			htmlTranslateProperty,
			htmlIsProperty,
			htmlPropertiesProperty,
			htmlRoleProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				"tag", "path", "partialUpdateKey", "isDOMNode", "pageId", "parent", "sharedComponentId", "syncedNodesIds",
				"name", "children", "dataKey", "cypherQuery", "restQuery", "functionQuery"
			),
			PropertyView.Ui,
			newSet(
				"tag", "path", "partialUpdateKey", "_html_class", "_html_id", "sharedComponentConfiguration",
				"isDOMNode", "pageId", "parent", "sharedComponentId", "syncedNodesIds", "data-structr-id", "children",
				"childrenIds", "showForLocales", "hideForLocales", "showConditions", "hideConditions", "dataKey", "cypherQuery",
				"restQuery", "functionQuery", "data-structr-rendering-mode", "data-structr-delay-or-interval", "data-structr-insert", "data-structr-from-widget",
				"data-structr-tree-children", "data-structr-reload-target", "eventMapping", "triggeredActions", "reloadingActions", "failureActions", "successNotificationActions",
				"failureNotificationActions", "data-structr-manual-reload-target"
			),
			PropertyView.Html,
			newSet(
				"_html_onabort", "_html_onblur", "_html_oncanplay", "_html_oncanplaythrough", "_html_onchange", "_html_onclick", "_html_oncontextmenu", "_html_ondblclick",
				"_html_ondrag", "_html_ondragend", "_html_ondragenter", "_html_ondragleave", "_html_ondragover", "_html_ondragstart", "_html_ondrop", "_html_ondurationchange",
				"_html_onemptied", "_html_onended", "_html_onerror", "_html_onfocus", "_html_oninput", "_html_oninvalid", "_html_onkeydown", "_html_onkeypress", "_html_onkeyup",
				"_html_onload", "_html_onloadeddata", "_html_onloadedmetadata", "_html_onloadstart", "_html_onmousedown", "_html_onmousemove", "_html_onmouseout",
				"_html_onmouseover", "_html_onmouseup", "_html_onmousewheel", "_html_onpause", "_html_onplay", "_html_onplaying", "_html_onprogress", "_html_onratechange",
				"_html_onreadystatechange", "_html_onreset", "_html_onscroll", "_html_onseeked", "_html_onseeking", "_html_onselect", "_html_onshow", "_html_onstalled",
				"_html_onsubmit", "_html_onsuspend", "_html_ontimeupdate", "_html_onvolumechange", "_html_onwaiting", "_html_data",

				"_html_accesskey", "_html_class", "_html_contenteditable", "_html_contextmenu", "_html_dir", "_html_draggable", "_html_dropzone",
				"_html_hidden", "_html_id", "_html_lang", "_html_spellcheck", "_html_style", "_html_tabindex", "_html_title", "_html_translate",

				"_html_role", "_html_is", "_html_properties"
			)
		);

	}

	@Override
	public Relation getRelation() {
		return null;
	}

	private Object handleSignInAction(final ActionContext actionContext, final NodeInterface entity, final Map<String, Object> parameters, final EventContext eventContext) throws FrameworkException {

		removeInternalDataBindingKeys(parameters);

		final Principal currentUser              = actionContext.getSecurityContext().getUser(false);
		final LoginResourceHandler loginResource = new LoginResourceHandler(new RESTCall("/login", PropertyView.Public, true, AbstractDataServlet.getTypeOrDefault(currentUser, "User")));
		final Map<String, Object> properties     = new LinkedHashMap<>();

		for (final Entry<String, Object> entry : parameters.entrySet()) {

			final String key   = entry.getKey();
			final String value = (String) entry.getValue();

			properties.put(key, value);
		}

		return loginResource.doPost(actionContext.getSecurityContext(), properties);
	}

	private Object handleSignOutAction(final ActionContext actionContext, final NodeInterface entity, final Map<String, Object> parameters, final EventContext eventContext) throws FrameworkException {

		removeInternalDataBindingKeys(parameters);

		final Principal currentUser                = actionContext.getSecurityContext().getUser(false);
		final LogoutResourceHandler logoutResource = new LogoutResourceHandler(new RESTCall("/logout", PropertyView.Public, true, AbstractDataServlet.getTypeOrDefault(currentUser, "User")));
		final Map<String, Object> properties       = new LinkedHashMap<>();

		for (final Entry<String, Object> entry : parameters.entrySet()) {

			final String key   = entry.getKey();
			final String value = (String) entry.getValue();

			properties.put(key, value);
		}

		return logoutResource.doPost(actionContext.getSecurityContext(), properties);
	}

	private Object handleSignUpAction(final ActionContext actionContext, final NodeInterface entity, final Map<String, Object> parameters, final EventContext eventContext) throws FrameworkException {

		final Principal currentUser          = actionContext.getSecurityContext().getUser(false);
		final Map<String, Object> properties = new LinkedHashMap<>();

		removeInternalDataBindingKeys(parameters);

		for (final Entry<String, Object> entry : parameters.entrySet()) {

			final String key   = entry.getKey();
			final String value = (String) entry.getValue();

			if (value != null) properties.put(key, value);
		}

		final RegistrationResourceHandler registrationResource = new RegistrationResourceHandler(new RESTCall("/registration", PropertyView.Public, true, AbstractDataServlet.getTypeOrDefault(currentUser, "User")));

		return registrationResource.doPost(actionContext.getSecurityContext(), properties);
	}

	private Object handleResetPasswordAction(final ActionContext actionContext, final NodeInterface entity, final Map<String, Object> parameters, final EventContext eventContext) throws FrameworkException {

		final Principal currentUser          = actionContext.getSecurityContext().getUser(false);
		final Map<String, Object> properties = new LinkedHashMap<>();

		removeInternalDataBindingKeys(parameters);

		for (final Entry<String, Object> entry : parameters.entrySet()) {

			final String key   = entry.getKey();
			final String value = (String) entry.getValue();

			if (value != null) properties.put(key, value);
		}

		final ResetPasswordResourceHandler resetPasswordResource = new ResetPasswordResourceHandler(new RESTCall("/reset-password", PropertyView.Public, true, AbstractDataServlet.getTypeOrDefault(currentUser, "User")));

		return resetPasswordResource.doPost(actionContext.getSecurityContext(), properties);
	}

//	private void handleTreeAction(final ActionContext actionContext, final java.util.Map<String, java.lang.Object> parameters, final EventContext eventContext, final String action) throws FrameworkException {
//
//		final SecurityContext securityContext = actionContext.getSecurityContext();
//
//		if (parameters.containsKey(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRTARGET)) {
//
//			final String key = getTreeItemSessionIdentifier((String)parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRTARGET));
//
//			switch (action) {
//
//				case "open-tree-item":
//					setSessionAttribute(securityContext, key, true);
//					break;
//
//				case "close-tree-item":
//					removeSessionAttribute(securityContext, key);
//					break;
//
//				case "toggle-tree-item":
//
//					if (Boolean.TRUE.equals(getSessionAttribute(securityContext, key))) {
//
//						removeSessionAttribute(securityContext, key);
//
//					} else {
//
//						setSessionAttribute(securityContext, key, true);
//					}
//					break;
//			}
//
//
//		} else {
//
//			throw new FrameworkException(422, "Cannot execute update action without target UUID (data-structr-target attribute).");
//		}
//	}

	private GraphObject handleCreateAction(final ActionContext actionContext, final NodeInterface entity, final Map<String, Object> parameters, final EventContext eventContext) throws FrameworkException {

		final SecurityContext securityContext = actionContext.getSecurityContext();

		// create new object of type?
		final String targetType = (String) parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRTARGET);
		if (targetType == null) {

			throw new FrameworkException(422, "Cannot execute create action without target type (data-structr-target attribute).");
		}

		// resolve target type
		Traits traits = Traits.of(targetType);
		if (traits == null) {

			throw new FrameworkException(422, "Cannot execute create action with target type " + targetType + ", type does not exist.");
		}

		removeInternalDataBindingKeys(parameters);

		// convert input
		final PropertyMap properties = PropertyMap.inputTypeToJavaType(securityContext, targetType, parameters);

		// create entity
		return StructrApp.getInstance(securityContext).create(targetType, properties);
	}

	private void handleUpdateAction(final ActionContext actionContext, final NodeInterface entity, final Map<String, Object> parameters, final EventContext eventContext) throws FrameworkException {

		final SecurityContext securityContext = actionContext.getSecurityContext();
		final String dataTarget               = (String) parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRTARGET);

		if (dataTarget == null) {

			throw new FrameworkException(422, "Cannot execute update action without target UUID (data-structr-target attribute).");
		}

		removeInternalDataBindingKeys(parameters);

		for (final GraphObject target : resolveDataTargets(actionContext, entity, dataTarget)) {

			// convert input
			final PropertyMap properties = PropertyMap.inputTypeToJavaType(securityContext, target.getType(), parameters);

			// update properties
			target.setProperties(securityContext, properties);

		}
	}

	private void handleDeleteAction(final ActionContext actionContext, final NodeInterface entity, final Map<String, Object> parameters, final EventContext eventContext) throws FrameworkException {

		final SecurityContext securityContext = actionContext.getSecurityContext();
		final App app                         = StructrApp.getInstance(securityContext);
		final String dataTarget               = (String) parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRTARGET);

		if (dataTarget == null) {

			throw new FrameworkException(422, "Cannot execute delete action without target UUID (data-structr-target attribute).");
		}

		for (final GraphObject target : resolveDataTargets(actionContext, entity, dataTarget)) {

			if (target.isNode()) {

				app.delete((NodeInterface)target);

			} else {

				app.delete((RelationshipInterface)target);
			}
		}
	}

	private Object handleCustomAction(final ActionContext actionContext, final NodeInterface entity, final Map<String, Object> parameters, final EventContext eventContext, final String methodName) throws FrameworkException {

		// Support old and new parameters
		final String idExpression  = (String) parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRIDEXPRESSION);
		final String structrTarget = (String) parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRTARGET);
		final String dataTarget    = structrTarget != null ? structrTarget : idExpression;

		// Empty dataTarget means no database object and no type, so it can only be a global (schema) method
		if (StringUtils.isNotBlank(methodName) && dataTarget == null) {

			removeInternalDataBindingKeys(parameters);

			return Actions.callWithSecurityContext(methodName, actionContext.getSecurityContext(), parameters);
		}

		if (Settings.isValidUuid(dataTarget)) {

			final List<GraphObject> targets = resolveDataTargets(actionContext, entity, dataTarget);
			final Logger logger             = LoggerFactory.getLogger(getClass());

			if (targets.size() > 1) {
				logger.warn("Custom action has multiple targets, this is not supported yet. Returning only the result of the first target.");
			}

			removeInternalDataBindingKeys(parameters);

			for (final GraphObject target : targets) {

				final AbstractMethod method = Methods.resolveMethod(target.getTraits(), methodName);
				if (method != null) {

					return method.execute(actionContext.getSecurityContext(), target, Arguments.fromMap(parameters), new EvaluationHints());

				} else {

					throw new FrameworkException(422, "Cannot execute method " + target.getClass().getSimpleName() + "." + methodName + ": method not found.");
				}
			}

		} else {

			// add support for static methods
			final Traits traits = Traits.of(dataTarget);
			if (traits != null) {

				final AbstractMethod method = Methods.resolveMethod(traits, methodName);
				if (method != null) {

					return method.execute(actionContext.getSecurityContext(), null, Arguments.fromMap(parameters), new EvaluationHints());

				} else {

					throw new FrameworkException(422, "Cannot execute static  method " + methodName + ": method not found.");
				}

			} else {

				throw new FrameworkException(422, "Cannot execute static method " + dataTarget + "." + methodName + ": type not found.");
			}
		}

		return null;
	}

	private Object handleAppendChildAction(final ActionContext actionContext, final NodeInterface entity, final Map<String, Object> parameters, final EventContext eventContext) throws FrameworkException {

		final SecurityContext securityContext = actionContext.getSecurityContext();
		final String dataTarget               = (String)parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRTARGET);

		if (dataTarget == null) {

			throw new FrameworkException(422, "Cannot execute append-child action without target UUID (data-structr-target attribute).");
		}

		// fetch child ID
		final String childId = (String)parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_CHILDID);
		if (childId == null) {

			throw new FrameworkException(422, "Cannot execute append-child action without child UUID (data-child-id attribute).");
		}

		// load child node
		final NodeInterface child = StructrApp.getInstance(securityContext).getNodeById("DOMNode", childId);
		if (child == null) {

			throw new FrameworkException(422, "Cannot execute append-child action without child (object with ID not found or not a DOMNode).");
		}

		removeInternalDataBindingKeys(parameters);

		for (final GraphObject target : resolveDataTargets(actionContext, entity, dataTarget)) {

			if (target.is("DOMElement")) {

				final DOMElement domTarget = target.as(DOMElement.class);

				domTarget.appendChild(child.as(DOMNode.class));

			} else {

				throw new FrameworkException(422, "Cannot execute append-child action on " + target.getClass().getSimpleName() + " (must be a DOMElement).");
			}
		}

		return null;
	}

	private Object handleRemoveChildAction(final ActionContext actionContext, final NodeInterface entity, final Map<String, Object> parameters, final EventContext eventContext) throws FrameworkException {

		final SecurityContext securityContext = actionContext.getSecurityContext();
		final String dataTarget               = (String)parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRTARGET);

		if (dataTarget == null) {

			throw new FrameworkException(422, "Cannot execute remove-child action without target UUID (data-structr-target attribute).");
		}

		// fetch child ID
		final String childId = (String)parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_CHILDID);
		if (childId == null) {

			throw new FrameworkException(422, "Cannot execute remove-child action without child UUID (data-child-id attribute).");
		}

		// load child node
		final NodeInterface child = StructrApp.getInstance(securityContext).getNodeById("DOMNode", childId);
		if (child == null) {

			throw new FrameworkException(422, "Cannot execute remove-child action without child (object with ID not found or not a DOMNode).");
		}

		removeInternalDataBindingKeys(parameters);

		for (final GraphObject target : resolveDataTargets(actionContext, entity, dataTarget)) {

			if (target.is("DOMElement")) {

				final DOMElement parent = target.as(DOMElement.class);

				RemoveDOMChildFunction.apply(actionContext.getSecurityContext(), parent, child.as(DOMNode.class));

			} else {

				throw new FrameworkException(422, "Cannot execute remove-child action on " + target.getClass().getSimpleName() + " (must be a DOMElement).");
			}
		}

		return null;
	}

	private Object handleInsertHtmlAction(final ActionContext actionContext, final NodeInterface entity, final Map<String, Object> parameters, final EventContext eventContext) throws FrameworkException {

		final SecurityContext securityContext = actionContext.getSecurityContext();
		final String dataTarget               = (String)parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRTARGET);
		if (dataTarget == null) {

			throw new FrameworkException(422, "Cannot execute insert-html action without target UUID (data-structr-target attribute).");
		}

		final String sourceObjectId = (String)parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_SOURCEOBJECT);
		if (sourceObjectId == null) {

			throw new FrameworkException(422, "Cannot execute insert-html action without html source object UUID (data-source-object).");
		}

		final NodeInterface sourceObject = StructrApp.getInstance(securityContext).getNodeById("NodeInterface", sourceObjectId);
		if (sourceObject == null) {

			throw new FrameworkException(422, "Cannot execute insert-html action without html source property name (data-source-property).");
		}

		final String sourcePropertyName = (String)parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_SOURCEPROPERTY);
		if (sourcePropertyName == null) {

			throw new FrameworkException(422, "Cannot execute insert-html action without html source property name (data-source-property).");
		}

		final PropertyKey<String> sourceProperty = sourceObject.getTraits().key(sourcePropertyName);
		if (sourceProperty == null) {

			throw new FrameworkException(422, "Cannot execute insert-html action with invalid source property (" + sourcePropertyName + " does not exist).");
		}

		final String htmlSource = sourceObject.getProperty(sourceProperty);
		if (StringUtils.isBlank(htmlSource)) {

			throw new FrameworkException(422, "Cannot execute insert-html action without empty html source.");
		}

		removeInternalDataBindingKeys(parameters);

		for (final GraphObject target : resolveDataTargets(actionContext, entity, dataTarget)) {

			if (target instanceof NodeInterface node && node.is("DOMElement")) {

				return InsertHtmlFunction.apply(securityContext, node, htmlSource);

			} else {

				throw new FrameworkException(422, "Cannot execute insert-html action on " + target.getClass().getSimpleName() + " (must be a DOMElement).");
			}
		}

		return null;
	}

	private Object handleReplaceHtmlAction(final ActionContext actionContext, final NodeInterface entity, final Map<String, Object> parameters, final EventContext eventContext) throws FrameworkException {

		final SecurityContext securityContext = actionContext.getSecurityContext();
		final String dataTarget               = (String) parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRTARGET);
		if (dataTarget == null) {

			throw new FrameworkException(422, "Cannot execute replace-html action without target UUID (data-structr-target attribute).");
		}

		// fetch child ID
		final String childId = (String) parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_CHILDID);
		if (childId == null) {

			throw new FrameworkException(422, "Cannot execute replace-html action without child UUID (data-child-id attribute).");
		}

		// load child node
		final NodeInterface child = StructrApp.getInstance(securityContext).getNodeById("DOMNode", childId);
		if (child == null) {

			throw new FrameworkException(422, "Cannot execute replace-html action without child (object with ID not found or not a DOMNode).");
		}

		final String sourceObjectId = (String) parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_SOURCEOBJECT);
		if (sourceObjectId == null) {

			throw new FrameworkException(422, "Cannot execute replace-html action without html source object UUID (data-source-object).");
		}

		final NodeInterface sourceObject = StructrApp.getInstance(securityContext).getNodeById("NodeInterface", sourceObjectId);
		if (sourceObject == null) {

			throw new FrameworkException(422, "Cannot execute replace-html action without html source property name (data-source-property).");
		}

		final String sourcePropertyName = (String)parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_SOURCEPROPERTY);
		if (sourcePropertyName == null) {

			throw new FrameworkException(422, "Cannot execute replace-html action without html source property name (data-source-property).");
		}

		final PropertyKey<String> sourceProperty = sourceObject.getTraits().key(sourcePropertyName);
		if (sourceProperty == null) {

			throw new FrameworkException(422, "Cannot execute replace-html action with invalid source property (" + sourcePropertyName + " does not exist).");
		}

		final String htmlSource = sourceObject.getProperty(sourceProperty);
		if (StringUtils.isBlank(htmlSource)) {

			throw new FrameworkException(422, "Cannot execute replace-html action with empty html source.");
		}

		removeInternalDataBindingKeys(parameters);

		for (final GraphObject target : resolveDataTargets(actionContext, entity, dataTarget)) {

			if (target instanceof NodeInterface n && n.is("DOMElement")) {

				final DOMElement parent = n.as(DOMElement.class);

				return ReplaceDOMChildFunction.apply(securityContext, parent, child.as(DOMNode.class), htmlSource);

			} else {

				throw new FrameworkException(422, "Cannot execute replace-html action on " + target.getClass().getSimpleName() + " (must be a DOMElement).");
			}
		}

		return null;
	}

	private void removeInternalDataBindingKeys(final Map<String, Object> parameters) {

		parameters.remove(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRIDEXPRESSION);
		parameters.remove(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRTARGET);
		parameters.remove(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRDATATYPE);
		parameters.remove(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRMETHOD);
		parameters.remove(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_CHILDID);
		parameters.remove(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_SOURCEOBJECT);
		parameters.remove(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_SOURCEPROPERTY);
		parameters.remove(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_HTMLEVENT);
	}

	public String getOffsetAttributeName(final NodeInterface entity, final String name, final int offset) {

		int namePosition = -1;
		int index = 0;

		List<String> keys = Iterables.toList(entity.getNode().getPropertyKeys());
		Collections.sort(keys);

		List<String> names = new ArrayList<>(10);

		for (String key : keys) {

			// use html properties only
			if (key.startsWith(PropertyView.Html)) {

				String htmlName = key.substring(DOMElement.HtmlPrefixLength);

				if (name.equals(htmlName)) {

					namePosition = index;
				}

				names.add(htmlName);

				index++;
			}
		}

		int offsetIndex = namePosition + offset;
		if (offsetIndex >= 0 && offsetIndex < names.size()) {

			return names.get(offsetIndex);
		}

		return null;
	}

	public static void renderDialogAttributes(final RenderContext renderContext, final AsyncBuffer out, final ActionMapping triggeredAction) throws FrameworkException {

		final String dialogType = triggeredAction.getDialogType();

		if (dialogType != null && !dialogType.equals("none")) {

			final Traits traits                = triggeredAction.getTraits();
			final PropertyKey<String> titleKey = traits.key("dialogTitle");
			final PropertyKey<String> textKey  = traits.key("dialogText");

			final String dialogTitle = triggeredAction.getPropertyWithVariableReplacement(renderContext, titleKey);
			final String dialogText = triggeredAction.getPropertyWithVariableReplacement(renderContext, textKey);

			out.append(" data-structr-dialog-type=\"").append(dialogType).append("\"");
			out.append(" data-structr-dialog-title=\"").append(DOMNode.escapeForHtmlAttributes(dialogTitle)).append("\"");
			out.append(" data-structr-dialog-text=\"").append(DOMNode.escapeForHtmlAttributes(dialogText)).append("\"");

		}
	}

	public void renderSuccessNotificationAttributes(final RenderContext renderContext, final AsyncBuffer out, final ActionMapping triggeredAction) {

		// Possible values for success notifications are none, system-alert, inline-text-message, custom-dialog-element, fire-event
		final String successNotifications = triggeredAction.getSuccessNotifications();
		if (StringUtils.isNotBlank(successNotifications)) {

			out.append(" data-structr-success-notifications=\"").append(successNotifications).append("\"");

			switch (successNotifications) {

				case "custom-dialog-linked":
					out.append(" data-structr-success-notifications-custom-dialog-element=\"").append(generateDataAttributesForIdList(renderContext, triggeredAction, "successNotificationElements")).append("\"");
					break;

				case "fire-event":
					out.append(" data-structr-success-notifications-event=\"").append(triggeredAction.getSuccessNotificationsEvent()).append("\"");
					break;

				case "inline-text-message":
					final Integer delay = triggeredAction.getSuccessNotificationsDelay();
					out.append(" data-structr-success-notifications-delay=\"").append(delay.toString()).append("\"");
					break;

				default:
					break;

			}
		}

		final String successNotificationsPartial = triggeredAction.getSuccessNotificationsPartial();
		if (StringUtils.isNotBlank(successNotificationsPartial)) {

			out.append(" data-structr-success-notifications-partial=\"").append(successNotificationsPartial).append("\"");
		}
	}

	public void renderFailureNotificationAttributes(final RenderContext renderContext, final AsyncBuffer out, final ActionMapping triggeredAction) {

		// Possible values for failure notifications are none, system-alert, inline-text-message, custom-dialog-element, fire-event
		final String failureNotifications = triggeredAction.getFailureNotifications();
		if (StringUtils.isNotBlank(failureNotifications)) {

			out.append(" data-structr-failure-notifications=\"").append(failureNotifications).append("\"");
		}

		if (StringUtils.isNotBlank(failureNotifications)) {

			switch (failureNotifications) {

				case "custom-dialog-linked":
					out.append(" data-structr-failure-notifications-custom-dialog-element=\"").append(generateDataAttributesForIdList(renderContext, triggeredAction, "failureNotificationElements")).append("\"");
					break;

				case "fire-event":
					out.append(" data-structr-failure-notifications-event=\"").append(triggeredAction.getFailureNotificationsEvent()).append("\"");
					break;

				case "inline-text-message":
					final Integer delay = triggeredAction.getFailureNotificationsDelay();
					out.append(" data-structr-failure-notifications-delay=\"").append(delay.toString()).append("\"");
					break;

				default:
					break;

			}
		}

		final String failureNotificationsPartial = triggeredAction.getFailureNotificationsPartial();
		if (StringUtils.isNotBlank(failureNotificationsPartial)) {

			out.append(" data-structr-failure-notifications-partial=\"").append(failureNotificationsPartial).append("\"");
		}
	}

	public void renderSuccessBehaviourAttributes(final RenderContext renderContext, final AsyncBuffer out, final ActionMapping triggeredAction) throws FrameworkException {

		final Traits traits = triggeredAction.getTraits();

		// Possible values for the success behaviour are nothing, full-page-reload, partial-refresh, navigate-to-url, fire-event
		final String successBehaviour = triggeredAction.getSuccessBehaviour();
		final String successPartial   = triggeredAction.getPropertyWithVariableReplacement(renderContext, traits.key("successPartial"));
		final String successURL       = triggeredAction.getPropertyWithVariableReplacement(renderContext, traits.key("successURL"));
		final String successEvent     = triggeredAction.getPropertyWithVariableReplacement(renderContext, traits.key("successEvent"));

		String successTargetString = null;

		if (StringUtils.isNotBlank(successBehaviour)) {

			switch (successBehaviour) {
				case "partial-refresh":
					successTargetString = successPartial;
					break;
				case "partial-refresh-linked":
					successTargetString = generateDataAttributesForIdList(renderContext, triggeredAction, "successTargets");
					break;
				case "navigate-to-url":
					successTargetString = "url:" + successURL;
					break;
				case "fire-event":
					successTargetString = "event:" + successEvent;
					break;
				case "full-page-reload":
					successTargetString = "url:";
					break;
				case "sign-out":
					successTargetString = "sign-out";
					break;
				case "none":
				default:
					successTargetString = null;
			}
		}

		final String idExpression = triggeredAction.getPropertyWithVariableReplacement(renderContext, traits.key("idExpression"));
		if (StringUtils.isNotBlank(idExpression)) {
			out.append(" data-structr-target=\"").append(idExpression).append("\"");
		}

		final String action = triggeredAction.getAction();
		if ("create".equals(action)) {

			final String dataType = triggeredAction.getPropertyWithVariableReplacement(renderContext, traits.key("dataType"));
			if (StringUtils.isNotBlank(dataType)) {
				out.append(" data-structr-target=\"").append(dataType).append("\"");
			}
		}

		if (StringUtils.isNotBlank(successTargetString)) {
			out.append(" data-structr-success-target=\"").append(successTargetString).append("\"");
		}
	}

	public void renderFailureBehaviourAttributes(final RenderContext renderContext, final AsyncBuffer out, final ActionMapping triggeredAction) throws FrameworkException {

		final Traits traits = triggeredAction.getTraits();

		// Possible values for the failure behaviour are nothing, full-page-reload, partial-refresh, navigate-to-url, fire-event
		final String failureBehaviour = triggeredAction.getFailureBehaviour();
		final String failurePartial   = triggeredAction.getPropertyWithVariableReplacement(renderContext, traits.key("failurePartial"));
		final String failureURL       = triggeredAction.getPropertyWithVariableReplacement(renderContext, traits.key("failureURL"));
		final String failureEvent     = triggeredAction.getPropertyWithVariableReplacement(renderContext, traits.key("failureEvent"));

		String failureTargetString = null;

		if (StringUtils.isNotBlank(failureBehaviour)) {

			switch (failureBehaviour) {
				case "partial-refresh":
					failureTargetString = failurePartial;
					break;
				case "partial-refresh-linked":
					failureTargetString = generateDataAttributesForIdList(renderContext, triggeredAction, "failureTargets");
					break;
				case "navigate-to-url":
					failureTargetString = "url:" + failureURL;
					break;
				case "fire-event":
					failureTargetString = "event:" + failureEvent;
					break;
				case "full-page-reload":
					failureTargetString = "url:";
					break;
				case "sign-out":
					failureTargetString = "sign-out";
					break;
				case "none":
				default:
					failureTargetString = null;
			}
		}

		if (StringUtils.isNotBlank(failureTargetString)) {
			out.append(" data-structr-failure-target=\"").append(failureTargetString).append("\"");
		}
	}

	public List<GraphObject> resolveDataTargets(final ActionContext actionContext, final NodeInterface entity, final String dataTarget) throws FrameworkException {

		final App app                   = StructrApp.getInstance(actionContext.getSecurityContext());
		final List<GraphObject> targets = new LinkedList<>();

		if (dataTarget.length() >= 32) {

			// list of UUIDs or single UUID, below code should handle both
			for (final String part : dataTarget.split(",")) {

				final String cleaned = part.trim();
				if (StringUtils.isNotBlank(cleaned) && cleaned.length() == 32) {

					final NodeInterface node = app.getNodeById(cleaned);
					if (node != null) {

						targets.add(node);

					} else {

						final RelationshipInterface rel = app.getRelationshipById(cleaned);
						if (rel != null) {

							targets.add(rel);
						}
					}
				}
			}

		} else {

			// evaluate single keyword
			final Object result = entity.evaluate(actionContext, dataTarget, null, new EvaluationHints(), 1, 1);
			if (result != null) {

				if (result instanceof Iterable) {

					for (final Object o : (Iterable)result) {

						if (o instanceof GraphObject) {
							targets.add((GraphObject)o);
						}
					}

				} else if (result instanceof GraphObject) {

					targets.add((GraphObject)result);
				}
			}

		}

		return targets;
	}

	public void updateReloadTargets(final DOMElement domElement) throws FrameworkException {

		try {

			final List<DOMElement> actualReloadSources = new LinkedList<>();
			final List<DOMElement> actualReloadTargets = new LinkedList<>();
			final org.jsoup.nodes.Element matchElement = DOMElement.getMatchElement(domElement);
			final String reloadTargets                 = domElement.getDataReloadTarget();
			final Page page                            = domElement.getOwnerDocument();

			if (page != null) {

				for (final DOMNode possibleReloadTargetNode : page.getElements()) {

					if (possibleReloadTargetNode.is("DOMElement")) {

						final DOMElement possibleTarget       = possibleReloadTargetNode.as(DOMElement.class);
						final org.jsoup.nodes.Element element = DOMElement.getMatchElement(possibleTarget);
						final String otherReloadTarget        = possibleTarget.getDataReloadTarget();

						if (reloadTargets != null && element != null) {

							for (final String part : reloadTargets.split(",")) {

								final String targetSelector = part.trim();

								try {

									if (StringUtils.isNotBlank(targetSelector) && element.select(targetSelector).first() != null) {

										actualReloadTargets.add(possibleTarget);
									}

								} catch (Throwable t) {}
							}
						}

						if (otherReloadTarget != null && matchElement != null) {

							for (final String part : otherReloadTarget.split(",")) {

								final String targetSelector = part.trim();

								try {

									if (StringUtils.isNotBlank(targetSelector) && matchElement.select(targetSelector).first() != null) {

										actualReloadSources.add(possibleTarget);
									}

								} catch (Throwable t) {}
							}
						}
					}
				}
			}

			final NodeInterface node = domElement;
			final Traits traits      = node.getTraits();

			// update reload targets with list from above
			node.setProperty(traits.key("reloadSources"), actualReloadSources);
			node.setProperty(traits.key("reloadTargets"), actualReloadTargets);

			// update shared component sync flag
			node.setProperty(traits.key("hasSharedComponent"), domElement.getSharedComponent() != null);

		} catch (Throwable t) {

			t.printStackTrace();
		}
	}

	public boolean isTargetElement(final DOMElement thisElement) {

		final boolean isManualReloadTarget                   = thisElement.isManualReloadTarget();
		final List<DOMElement> reloadSources                 = Iterables.toList(thisElement.getReloadSources());
		final List<ActionMapping> reloadingActions           = Iterables.toList(thisElement.getReloadingActions());
		final List<ActionMapping> failureActions             = Iterables.toList(thisElement.getFailureActions());
		final List<ActionMapping> successNotificationActions = Iterables.toList(thisElement.getSuccessNotificationActions());
		final List<ActionMapping> failureNotificationActions = Iterables.toList(thisElement.getFailureNotificationActions());

		return isManualReloadTarget || !reloadSources.isEmpty() || !reloadingActions.isEmpty() || !failureActions.isEmpty() || !successNotificationActions.isEmpty() || !failureNotificationActions.isEmpty();
	}

	// ----- private methods -----
	private String generateDataAttributesForIdList(final RenderContext renderContext, final ActionMapping actionMapping, final String keyName) {

		final NodeInterface actionNode                 = actionMapping;
		final List<String> selectors                   = new LinkedList<>();
		final Traits traits                            = actionNode.getTraits();
		final PropertyKey<Iterable<NodeInterface>> key = traits.key(keyName);

		for (final NodeInterface node : actionNode.getProperty(key)) {

			// Create CSS selector for data-structr-id
			String selector = "[data-structr-id='" + node.getUuid() + "']";

			final String dataKey = node.as(DOMNode.class).getDataKey();
			if (dataKey != null) {

				selector += "[data-repeater-data-object-id='" + renderContext.getDataNode(dataKey).getUuid() + "']";
			}

			selectors.add(selector);
		}

		return StringUtils.join(selectors, ",");
	}
}