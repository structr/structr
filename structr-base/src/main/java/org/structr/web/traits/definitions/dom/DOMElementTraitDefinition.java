/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
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
import org.structr.core.api.InstanceMethod;
import org.structr.core.api.Methods;
import org.structr.core.api.NamedArguments;
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
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
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
import org.structr.web.traits.definitions.ActionMappingTraitDefinition;
import org.structr.web.traits.definitions.ParameterMappingTraitDefinition;
import org.structr.web.traits.operations.*;
import org.structr.web.traits.wrappers.dom.DOMElementTraitWrapper;

import java.util.*;
import java.util.Map.Entry;

import static org.structr.web.entity.dom.DOMElement.lowercaseBodyName;
import static org.structr.web.entity.dom.DOMNode.EVENT_ACTION_MAPPING_CATEGORY;
import static org.structr.web.entity.dom.DOMNode.PAGE_CATEGORY;

public class DOMElementTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String RELOAD_SOURCES_PROPERTY                    = "reloadSources";
	public static final String RELOAD_TARGETS_PROPERTY                    = "reloadTargets";
	public static final String TRIGGERED_ACTIONS_PROPERTY                 = "triggeredActions";
	public static final String PARAMETER_MAPPINGS_PROPERTY                = "parameterMappings";
	public static final String TAG_PROPERTY                               = "tag";
	public static final String PATH_PROPERTY                              = "path";
	public static final String PARTIAL_UPDATE_KEY_PROPERTY                = "partialUpdateKey";
	public static final String DATA_STRUCTR_MANUAL_RELOAD_TARGET_PROPERTY = "data-structr-manual-reload-target";
	public static final String FROM_WIDGET_PROPERTY                       = "fromWidget";
	public static final String DATA_STRUCTR_INSERT_PROPERTY               = "data-structr-insert";
	public static final String DATA_STRUCTR_FROM_WIDGET_PROPERTY          = "data-structr-from-widget";
	public static final String EVENT_MAPPING_PROPERTY                     = "eventMapping";
	public static final String DATA_STRUCTR_TREE_CHILDREN_PROPERTY        = "data-structr-tree-children";
	public static final String DATA_STRUCTR_RELOAD_TARGET_PROPERTY        = "data-structr-reload-target";
	public static final String DATA_STRUCTR_RENDERING_MODE_PROPERTY       = "data-structr-rendering-mode";
	public static final String DATA_STRUCTR_DELAY_OR_INTERVAL_PROPERTY    = "data-structr-delay-or-interval";
	public static final String _HTML_ONABORT_PROPERTY                     = "_html_onabort";
	public static final String _HTML_ONBLUR_PROPERTY                      = "_html_onblur";
	public static final String _HTML_ONCANPLAY_PROPERTY                   = "_html_oncanplay";
	public static final String _HTML_ONCANPLAYTHROUGH_PROPERTY            = "_html_oncanplaythrough";
	public static final String _HTML_ONCHANGE_PROPERTY                    = "_html_onchange";
	public static final String _HTML_ONCLICK_PROPERTY                     = "_html_onclick";
	public static final String _HTML_ONCONTEXTMENU_PROPERTY               = "_html_oncontextmenu";
	public static final String _HTML_ONDBLCLICK_PROPERTY                  = "_html_ondblclick";
	public static final String _HTML_ONDRAG_PROPERTY                      = "_html_ondrag";
	public static final String _HTML_ONDRAGEND_PROPERTY                   = "_html_ondragend";
	public static final String _HTML_ONDRAGENTER_PROPERTY                 = "_html_ondragenter";
	public static final String _HTML_ONDRAGLEAVE_PROPERTY                 = "_html_ondragleave";
	public static final String _HTML_ONDRAGOVER_PROPERTY                  = "_html_ondragover";
	public static final String _HTML_ONDRAGSTART_PROPERTY                 = "_html_ondragstart";
	public static final String _HTML_ONDROP_PROPERTY                      = "_html_ondrop";
	public static final String _HTML_ONDURATIONCHANGE_PROPERTY            = "_html_ondurationchange";
	public static final String _HTML_ONEMPTIED_PROPERTY                   = "_html_onemptied";
	public static final String _HTML_ONENDED_PROPERTY                     = "_html_onended";
	public static final String _HTML_ONERROR_PROPERTY                     = "_html_onerror";
	public static final String _HTML_ONFOCUS_PROPERTY                     = "_html_onfocus";
	public static final String _HTML_ONINPUT_PROPERTY                     = "_html_oninput";
	public static final String _HTML_ONINVALID_PROPERTY                   = "_html_oninvalid";
	public static final String _HTML_ONKEYDOWN_PROPERTY                   = "_html_onkeydown";
	public static final String _HTML_ONKEYPRESS_PROPERTY                  = "_html_onkeypress";
	public static final String _HTML_ONKEYUP_PROPERTY                     = "_html_onkeyup";
	public static final String _HTML_ONLOAD_PROPERTY                      = "_html_onload";
	public static final String _HTML_ONLOADEDDATA_PROPERTY                = "_html_onloadeddata";
	public static final String _HTML_ONLOADEDMETADATA_PROPERTY            = "_html_onloadedmetadata";
	public static final String _HTML_ONLOADSTART_PROPERTY                 = "_html_onloadstart";
	public static final String _HTML_ONMOUSEDOWN_PROPERTY                 = "_html_onmousedown";
	public static final String _HTML_ONMOUSEMOVE_PROPERTY                 = "_html_onmousemove";
	public static final String _HTML_ONMOUSEOUT_PROPERTY                  = "_html_onmouseout";
	public static final String _HTML_ONMOUSEOVER_PROPERTY                 = "_html_onmouseover";
	public static final String _HTML_ONMOUSEUP_PROPERTY                   = "_html_onmouseup";
	public static final String _HTML_ONMOUSEWHEEL_PROPERTY                = "_html_onmousewheel";
	public static final String _HTML_ONPAUSE_PROPERTY                     = "_html_onpause";
	public static final String _HTML_ONPLAY_PROPERTY                      = "_html_onplay";
	public static final String _HTML_ONPLAYING_PROPERTY                   = "_html_onplaying";
	public static final String _HTML_ONPROGRESS_PROPERTY                  = "_html_onprogress";
	public static final String _HTML_ONRATECHANGE_PROPERTY                = "_html_onratechange";
	public static final String _HTML_ONREADYSTATECHANGE_PROPERTY          = "_html_onreadystatechange";
	public static final String _HTML_ONRESET_PROPERTY                     = "_html_onreset";
	public static final String _HTML_ONSCROLL_PROPERTY                    = "_html_onscroll";
	public static final String _HTML_ONSEEKED_PROPERTY                    = "_html_onseeked";
	public static final String _HTML_ONSEEKING_PROPERTY                   = "_html_onseeking";
	public static final String _HTML_ONSELECT_PROPERTY                    = "_html_onselect";
	public static final String _HTML_ONSHOW_PROPERTY                      = "_html_onshow";
	public static final String _HTML_ONSTALLED_PROPERTY                   = "_html_onstalled";
	public static final String _HTML_ONSUBMIT_PROPERTY                    = "_html_onsubmit";
	public static final String _HTML_ONSUSPEND_PROPERTY                   = "_html_onsuspend";
	public static final String _HTML_ONTIMEUPDATE_PROPERTY                = "_html_ontimeupdate";
	public static final String _HTML_ONVOLUMECHANGE_PROPERTY              = "_html_onvolumechange";
	public static final String _HTML_ONWAITING_PROPERTY                   = "_html_onwaiting";
	public static final String _HTML_DATA_PROPERTY                        = "_html_data";
	public static final String _HTML_ACCESSKEY_PROPERTY                   = "_html_accesskey";
	public static final String _HTML_CLASS_PROPERTY                       = "_html_class";
	public static final String _HTML_CONTENTEDITABLE_PROPERTY             = "_html_contenteditable";
	public static final String _HTML_CONTEXTMENU_PROPERTY                 = "_html_contextmenu";
	public static final String _HTML_DIR_PROPERTY                         = "_html_dir";
	public static final String _HTML_DRAGGABLE_PROPERTY                   = "_html_draggable";
	public static final String _HTML_DROPZONE_PROPERTY                    = "_html_dropzone";
	public static final String _HTML_HIDDEN_PROPERTY                      = "_html_hidden";
	public static final String _HTML_ID_PROPERTY                          = "_html_id";
	public static final String _HTML_LANG_PROPERTY                        = "_html_lang";
	public static final String _HTML_SPELLCHECK_PROPERTY                  = "_html_spellcheck";
	public static final String _HTML_STYLE_PROPERTY                       = "_html_style";
	public static final String _HTML_TABINDEX_PROPERTY                    = "_html_tabindex";
	public static final String _HTML_TITLE_PROPERTY                       = "_html_title";
	public static final String _HTML_TRANSLATE_PROPERTY                   = "_html_translate";
	public static final String _HTML_IS_PROPERTY                          = "_html_is";
	public static final String _HTML_PROPERTIES_PROPERTY                  = "_html_properties";
	public static final String _HTML_ROLE_PROPERTY                        = "_html_role";

	private static final Set<String> RequestParameterBlacklist = Set.of(HtmlServlet.ENCODED_RENDER_STATE_PARAMETER_NAME);

	public DOMElementTraitDefinition() {
		super(StructrTraits.DOM_ELEMENT);
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
					final String _name = node.getProperty(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY));

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

					return node.getProperty(traits.key(_HTML_CLASS_PROPERTY));
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
					properties.put(traits.key(TAG_PROPERTY), newNode.getTag());

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

									if (subNode.is(StructrTraits.DOM_ELEMENT)) {
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

						final String _name = _sharedComponentElement.getProperty(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY));
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
							if (key.startsWith(PropertyView.Html) && traits.hasKey(key)) {
								htmlAttributes.add(traits.key(key));
							}
						});

						if (EditMode.DEPLOYMENT.equals(editMode)) {
							Collections.sort(htmlAttributes);
						}

						for (final PropertyKey attribute : htmlAttributes) {

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

								out.append(" ").append(DOMNodeTraitDefinition.DATA_STRUCTR_ID_PROPERTY).append("=\"").append(uuid).append("\"");
								break;

							case RAW:

								out.append(" ").append(DOMNodeTraitDefinition.DATA_STRUCTR_HASH_PROPERTY).append("=\"").append(node.getIdHash()).append("\"");
								break;

							case WIDGET:
							case DEPLOYMENT:

								final String eventMapping = node.getEventMapping();
								if (eventMapping != null) {

									out.append(" ").append("data-structr-meta-event-mapping").append("=\"").append(StringEscapeUtils.escapeHtml4(eventMapping)).append("\"");
								}
								break;

							case NONE:

								// Get actions in superuser context
								final DOMElement thisElementWithSuperuserContext = StructrApp.getInstance().getNodeById(StructrTraits.DOM_ELEMENT, uuid).as(DOMElement.class);
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
									for (final String key : Set.of(ActionMappingTraitDefinition.EVENT_PROPERTY, ActionMappingTraitDefinition.ACTION_PROPERTY, ActionMappingTraitDefinition.METHOD_PROPERTY, ActionMappingTraitDefinition.FLOW_PROPERTY, ActionMappingTraitDefinition.DATA_TYPE_PROPERTY, ActionMappingTraitDefinition.ID_EXPRESSION_PROPERTY)) {

										final String value = actionNode.getPropertyWithVariableReplacement(renderContext, eamTraits.key(key));
										if (StringUtils.isNotBlank(value)) {

											final String keyHyphenated = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, key);
											out.append(" data-structr-" + keyHyphenated + "=\"").append(value).append("\"");
										}

										if (key.equals(ActionMappingTraitDefinition.EVENT_PROPERTY)) {

											eventsString = value;
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

									final Traits parameterMappingTraits = Traits.of(StructrTraits.PARAMETER_MAPPING);
									final PropertyKey<String> scriptExpressionKey = parameterMappingTraits.key(ParameterMappingTraitDefinition.SCRIPT_EXPRESSION_PROPERTY);
									final PropertyKey<String> parameterTypeKey    = parameterMappingTraits.key(ParameterMappingTraitDefinition.PARAMETER_TYPE_PROPERTY);
									final PropertyKey<String> parameterNameKey    = parameterMappingTraits.key(ParameterMappingTraitDefinition.PARAMETER_NAME_PROPERTY);
									final PropertyKey<String> htmlIdKey           = traits.key(DOMElementTraitDefinition._HTML_ID_PROPERTY);


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

								if (thisElementWithSuperuserContext.isTargetElement()) {

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

								outputStructrId |= (thisElementWithSuperuserContext.is(StructrTraits.TEMPLATE_ELEMENT) || parameterMappings.iterator().hasNext());

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

			new InstanceMethod(StructrTraits.DOM_ELEMENT, "event") {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Map<String, Object> parameters) throws FrameworkException {

					final RenderContext renderContext = new RenderContext(securityContext);
					final EventContext  eventContext  = new EventContext();
					final String        action;

					final NodeInterface domElementNode         = StructrApp.getInstance().getNodeById(StructrTraits.DOM_ELEMENT, entity.getUuid());
					final DOMElement domElement                = domElementNode.as(DOMElement.class);

					action = getActionMapping(entity.as(DOMElement.class)).getAction();

					// store event context in object
					renderContext.setConstant("eventContext", eventContext);

					switch (action) {

						// Note: if you add new actions here, please also add them to MigrationService.EventActionMappingActions so
						// they are not migrated accidentially..

						case "create":
							return handleCreateAction(renderContext, domElementNode, parameters, eventContext);

						case "update":
							handleUpdateAction(renderContext, domElementNode, parameters, eventContext);
							break;

						case "delete":
							handleDeleteAction(renderContext, domElementNode, parameters, eventContext);
							break;

						case "append-child":
							handleAppendChildAction(renderContext, domElementNode, parameters, eventContext);
							break;

						case "remove-child":
							handleRemoveChildAction(renderContext, domElementNode, parameters, eventContext);
							break;

						case "insert-html":
							return handleInsertHtmlAction(renderContext, domElementNode, parameters, eventContext);

						case "replace-html":
							return handleReplaceHtmlAction(renderContext, domElementNode, parameters, eventContext);

						/*
						case "open-tree-item":
						case "close-tree-item":
						case "toggle-tree-item":
							handleTreeAction(actionContext, parameters, eventContext, event);
							break;
						*/

						case "sign-in":
							return handleSignInAction(renderContext, domElementNode, parameters, eventContext);

						case "sign-out":
							return handleSignOutAction(renderContext, domElementNode, parameters, eventContext);

						case "sign-up":
							return handleSignUpAction(renderContext, domElementNode, parameters, eventContext);

						case "reset-password":
							return handleResetPasswordAction(renderContext, domElementNode, parameters, eventContext);

						case "flow":
							//final String flow = (String) parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRFLOW);
							final String flow = getActionMapping(entity.as(DOMElement.class)).getFlow();
							return handleFlowAction(renderContext, domElementNode, parameters, eventContext, flow);

						case "method":
						default:
							// execute custom method (and return the result directly)
							final String method = (String) parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRMETHOD);
							return handleCustomAction(renderContext, domElementNode, parameters, eventContext, method);
					}

					return eventContext;
				}
			}
		);
	}

	/**
	 * Get the action mapping object connected to this element.
	 *
	 * @return The action mapping object
	 * @throws FrameworkException
	 */
	private ActionMapping getActionMapping(final DOMElement domElementNode) throws FrameworkException {

		ActionMapping triggeredAction;
		final List<ActionMapping> triggeredActions = Iterables.toList(domElementNode.getTriggeredActions());

		if (triggeredActions != null && !triggeredActions.isEmpty()) {

			triggeredAction = triggeredActions.get(0);

			return triggeredAction;

		} else {

			throw new FrameworkException(422, "Cannot execute action without action defined on this DOMElement: " + this);
		}
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Iterable<NodeInterface>> reloadSourcesProperty     = new StartNodes(RELOAD_SOURCES_PROPERTY, StructrTraits.DOM_ELEMENT_RELOADS_DOM_ELEMENT);
		final Property<Iterable<NodeInterface>> reloadTargetsProperty     = new EndNodes(RELOAD_TARGETS_PROPERTY, StructrTraits.DOM_ELEMENT_RELOADS_DOM_ELEMENT);
		final Property<Iterable<NodeInterface>> triggeredActionsProperty  = new EndNodes(TRIGGERED_ACTIONS_PROPERTY, StructrTraits.DOM_ELEMENT_TRIGGERED_BY_ACTION_MAPPING);

		// FIXME ? why does DOMElement have parameter mappings? they are/should be attached to ActionMapping nodes (it is also not defined on ParameterMapping...)
		final Property<Iterable<NodeInterface>> parameterMappingsProperty = new EndNodes(PARAMETER_MAPPINGS_PROPERTY, StructrTraits.DOM_ELEMENT_INPUT_ELEMENT_PARAMETER_MAPPING);

		final Property<String> tagProperty                  = new StringProperty(TAG_PROPERTY).indexed().category(PAGE_CATEGORY);
		final Property<String> pathProperty                 = new StringProperty(PATH_PROPERTY).indexed();
		final Property<String> partialUpdateKeyProperty     = new StringProperty(PARTIAL_UPDATE_KEY_PROPERTY).indexed();

		final Property<Boolean> manualReloadTargetProperty  = new BooleanProperty(DATA_STRUCTR_MANUAL_RELOAD_TARGET_PROPERTY).category(EVENT_ACTION_MAPPING_CATEGORY).hint("Identifies this element as a manual reload target, this is necessary when using repeaters as reload targets.");
		final Property<Boolean> fromWidgetProperty          = new BooleanProperty(FROM_WIDGET_PROPERTY);
		final Property<Boolean> dataInsertProperty          = new BooleanProperty(DATA_STRUCTR_INSERT_PROPERTY);
		final Property<Boolean> dataFromWidgetProperty      = new BooleanProperty(DATA_STRUCTR_FROM_WIDGET_PROPERTY);

		final Property<String> eventMappingProperty        = new StringProperty(EVENT_MAPPING_PROPERTY).category(EVENT_ACTION_MAPPING_CATEGORY).hint("A mapping between the desired Javascript event (click, drop, dragOver, ...) and the server-side event that should be triggered: (create | update | delete | <method name>).");
		// probably useless ATM because EAM does not support trees yet
		final Property<String> dataTreeChildrenProperty    = new StringProperty(DATA_STRUCTR_TREE_CHILDREN_PROPERTY).category(EVENT_ACTION_MAPPING_CATEGORY).hint("Toggles automatic visibility for tree child items when the 'toggle-tree-item' event is mapped. This field must contain the data key on which the tree is based, e.g. 'item'.");
		final Property<String> dataReloadTargetProperty    = new StringProperty(DATA_STRUCTR_RELOAD_TARGET_PROPERTY).category(EVENT_ACTION_MAPPING_CATEGORY).hint("CSS selector that specifies which partials to reload.");
		final Property<String> renderingModeProperty       = new StringProperty(DATA_STRUCTR_RENDERING_MODE_PROPERTY).category(EVENT_ACTION_MAPPING_CATEGORY).hint("Rendering mode, possible values are empty (default for eager rendering), 'load' to render when the DOM document has finished loading, 'delayed' like 'load' but with a fixed delay, 'visible' to render when the element comes into view and 'periodic' to render the element with periodic updates with a given interval");
		final Property<String> delayOrIntervalProperty     = new StringProperty(DATA_STRUCTR_DELAY_OR_INTERVAL_PROPERTY).category(EVENT_ACTION_MAPPING_CATEGORY).hint("Delay or interval in milliseconds for 'delayed' or 'periodic' rendering mode");
		final Property<String> onAbortProperty             = new StringProperty(_HTML_ONABORT_PROPERTY);
		final Property<String> onBlurProperty              = new StringProperty(_HTML_ONBLUR_PROPERTY);
		final Property<String> onCanPlayProperty           = new StringProperty(_HTML_ONCANPLAY_PROPERTY);
		final Property<String> onCanPlayThroughProperty    = new StringProperty(_HTML_ONCANPLAYTHROUGH_PROPERTY);
		final Property<String> onChangeProperty            = new StringProperty(_HTML_ONCHANGE_PROPERTY);
		final Property<String> onClickProperty             = new StringProperty(_HTML_ONCLICK_PROPERTY);
		final Property<String> onContextMenuProperty       = new StringProperty(_HTML_ONCONTEXTMENU_PROPERTY);
		final Property<String> onDblClickProperty          = new StringProperty(_HTML_ONDBLCLICK_PROPERTY);
		final Property<String> onDragProperty              = new StringProperty(_HTML_ONDRAG_PROPERTY);
		final Property<String> onDragEndProperty           = new StringProperty(_HTML_ONDRAGEND_PROPERTY);
		final Property<String> onDragEnterProperty         = new StringProperty(_HTML_ONDRAGENTER_PROPERTY);
		final Property<String> onDragLeaveProperty         = new StringProperty(_HTML_ONDRAGLEAVE_PROPERTY);
		final Property<String> onDragOverProperty          = new StringProperty(_HTML_ONDRAGOVER_PROPERTY);
		final Property<String> onDragStartProperty         = new StringProperty(_HTML_ONDRAGSTART_PROPERTY);
		final Property<String> onDropProperty              = new StringProperty(_HTML_ONDROP_PROPERTY);
		final Property<String> onDurationChangeProperty    = new StringProperty(_HTML_ONDURATIONCHANGE_PROPERTY);
		final Property<String> onEmptiedProperty           = new StringProperty(_HTML_ONEMPTIED_PROPERTY);
		final Property<String> onEndedProperty             = new StringProperty(_HTML_ONENDED_PROPERTY);
		final Property<String> onErrorProperty             = new StringProperty(_HTML_ONERROR_PROPERTY);
		final Property<String> onFocusProperty             = new StringProperty(_HTML_ONFOCUS_PROPERTY);
		final Property<String> onInputProperty             = new StringProperty(_HTML_ONINPUT_PROPERTY);
		final Property<String> onInvalidProperty           = new StringProperty(_HTML_ONINVALID_PROPERTY);
		final Property<String> onKeyDownProperty           = new StringProperty(_HTML_ONKEYDOWN_PROPERTY);
		final Property<String> onKeyPressProperty          = new StringProperty(_HTML_ONKEYPRESS_PROPERTY);
		final Property<String> onKeyUpProperty             = new StringProperty(_HTML_ONKEYUP_PROPERTY);
		final Property<String> onLoadProperty              = new StringProperty(_HTML_ONLOAD_PROPERTY);
		final Property<String> onLoadedDataProperty        = new StringProperty(_HTML_ONLOADEDDATA_PROPERTY);
		final Property<String> onLoadedMetadataProperty    = new StringProperty(_HTML_ONLOADEDMETADATA_PROPERTY);
		final Property<String> onLoadStartProperty         = new StringProperty(_HTML_ONLOADSTART_PROPERTY);
		final Property<String> onMouseDownProperty         = new StringProperty(_HTML_ONMOUSEDOWN_PROPERTY);
		final Property<String> onMouseMoveProperty         = new StringProperty(_HTML_ONMOUSEMOVE_PROPERTY);
		final Property<String> onMouseOutProperty          = new StringProperty(_HTML_ONMOUSEOUT_PROPERTY);
		final Property<String> onMouseOverProperty         = new StringProperty(_HTML_ONMOUSEOVER_PROPERTY);
		final Property<String> onMouseUpProperty           = new StringProperty(_HTML_ONMOUSEUP_PROPERTY);
		final Property<String> onMouseWheelProperty        = new StringProperty(_HTML_ONMOUSEWHEEL_PROPERTY);
		final Property<String> onPauseProperty             = new StringProperty(_HTML_ONPAUSE_PROPERTY);
		final Property<String> onPlayProperty              = new StringProperty(_HTML_ONPLAY_PROPERTY);
		final Property<String> onPlayingProperty           = new StringProperty(_HTML_ONPLAYING_PROPERTY);
		final Property<String> onProgressProperty          = new StringProperty(_HTML_ONPROGRESS_PROPERTY);
		final Property<String> onRateChangeProperty        = new StringProperty(_HTML_ONRATECHANGE_PROPERTY);
		final Property<String> onReadyStateChangeProperty  = new StringProperty(_HTML_ONREADYSTATECHANGE_PROPERTY);
		final Property<String> onResetProperty             = new StringProperty(_HTML_ONRESET_PROPERTY);
		final Property<String> onScrollProperty            = new StringProperty(_HTML_ONSCROLL_PROPERTY);
		final Property<String> onSeekedProperty            = new StringProperty(_HTML_ONSEEKED_PROPERTY);
		final Property<String> onSeekingProperty           = new StringProperty(_HTML_ONSEEKING_PROPERTY);
		final Property<String> onSelectProperty            = new StringProperty(_HTML_ONSELECT_PROPERTY);
		final Property<String> onShowProperty              = new StringProperty(_HTML_ONSHOW_PROPERTY);
		final Property<String> onStalledProperty           = new StringProperty(_HTML_ONSTALLED_PROPERTY);
		final Property<String> onSubmitProperty            = new StringProperty(_HTML_ONSUBMIT_PROPERTY);
		final Property<String> onSuspendProperty           = new StringProperty(_HTML_ONSUSPEND_PROPERTY);
		final Property<String> onTimeUpdateProperty        = new StringProperty(_HTML_ONTIMEUPDATE_PROPERTY);
		final Property<String> onVolumechangeProperty      = new StringProperty(_HTML_ONVOLUMECHANGE_PROPERTY);
		final Property<String> onWaitingProperty           = new StringProperty(_HTML_ONWAITING_PROPERTY);
		final Property<String> htmlDataProperty            = new StringProperty(_HTML_DATA_PROPERTY);

		// Core attributes
		final Property<String> htmlAcceskeyProperty        = new StringProperty(_HTML_ACCESSKEY_PROPERTY);
		final Property<String> htmlClassProperty           = new StringProperty(_HTML_CLASS_PROPERTY);
		final Property<String> htmlContentEditableProperty = new StringProperty(_HTML_CONTENTEDITABLE_PROPERTY);
		final Property<String> htmlContextMenuProperty     = new StringProperty(_HTML_CONTEXTMENU_PROPERTY);
		final Property<String> htmlDirProperty             = new StringProperty(_HTML_DIR_PROPERTY);
		final Property<String> htmlDraggableProperty       = new StringProperty(_HTML_DRAGGABLE_PROPERTY);
		final Property<String> htmlDropzoneProperty        = new StringProperty(_HTML_DROPZONE_PROPERTY);
		final Property<String> htmlHiddenProperty          = new StringProperty(_HTML_HIDDEN_PROPERTY);
		final Property<String> htmlIdProperty              = new StringProperty(_HTML_ID_PROPERTY).indexed();
		final Property<String> htmlLangProperty            = new StringProperty(_HTML_LANG_PROPERTY);
		final Property<String> htmlSpellcheckProperty      = new StringProperty(_HTML_SPELLCHECK_PROPERTY);
		final Property<String> htmlStyleProperty           = new StringProperty(_HTML_STYLE_PROPERTY);
		final Property<String> htmlTabindexProperty        = new StringProperty(_HTML_TABINDEX_PROPERTY);
		final Property<String> htmlTitleProperty           = new StringProperty(_HTML_TITLE_PROPERTY);
		final Property<String> htmlTranslateProperty       = new StringProperty(_HTML_TRANSLATE_PROPERTY);

		// new properties for Polymer support
		final Property<String> htmlIsProperty              = new StringProperty(_HTML_IS_PROPERTY);
		final Property<String> htmlPropertiesProperty      = new StringProperty(_HTML_PROPERTIES_PROPERTY);

		// The role attribute, see http://www.w3.org/TR/role-attribute/
		final Property<String> htmlRoleProperty            = new StringProperty(_HTML_ROLE_PROPERTY);

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
					TAG_PROPERTY, PATH_PROPERTY, PARTIAL_UPDATE_KEY_PROPERTY, DOMNodeTraitDefinition.IS_DOM_NODE_PROPERTY,
					DOMNodeTraitDefinition.PAGE_ID_PROPERTY, DOMNodeTraitDefinition.PARENT_PROPERTY, DOMNodeTraitDefinition.SHARED_COMPONENT_ID_PROPERTY,
					DOMNodeTraitDefinition.SYNCED_NODES_IDS_PROPERTY, NodeInterfaceTraitDefinition.NAME_PROPERTY, DOMNodeTraitDefinition.CHILDREN_PROPERTY,
					DOMNodeTraitDefinition.DATA_KEY_PROPERTY, DOMNodeTraitDefinition.CYPHER_QUERY_PROPERTY, DOMNodeTraitDefinition.REST_QUERY_PROPERTY,
					DOMNodeTraitDefinition.FUNCTION_QUERY_PROPERTY
			),
			PropertyView.Ui,
			newSet(
					TAG_PROPERTY, PATH_PROPERTY, PARTIAL_UPDATE_KEY_PROPERTY, _HTML_CLASS_PROPERTY, _HTML_ID_PROPERTY, DOMNodeTraitDefinition.SHARED_COMPONENT_CONFIGURATION_PROPERTY,
					DOMNodeTraitDefinition.IS_DOM_NODE_PROPERTY, DOMNodeTraitDefinition.PAGE_ID_PROPERTY, DOMNodeTraitDefinition.PARENT_PROPERTY,
					DOMNodeTraitDefinition.SHARED_COMPONENT_ID_PROPERTY, DOMNodeTraitDefinition.SYNCED_NODES_IDS_PROPERTY,
					DOMNodeTraitDefinition.DATA_STRUCTR_ID_PROPERTY, DOMNodeTraitDefinition.CHILDREN_PROPERTY, DOMNodeTraitDefinition.CHILDREN_IDS_PROPERTY,
					DOMNodeTraitDefinition.SHOW_FOR_LOCALES_PROPERTY, DOMNodeTraitDefinition.HIDE_FOR_LOCALES_PROPERTY, DOMNodeTraitDefinition.SHOW_CONDITIONS_PROPERTY,
					DOMNodeTraitDefinition.HIDE_CONDITIONS_PROPERTY, DOMNodeTraitDefinition.DATA_KEY_PROPERTY, DOMNodeTraitDefinition.CYPHER_QUERY_PROPERTY,
					DOMNodeTraitDefinition.REST_QUERY_PROPERTY, DOMNodeTraitDefinition.FUNCTION_QUERY_PROPERTY,
					DOMElementTraitDefinition.DATA_STRUCTR_RENDERING_MODE_PROPERTY, DOMElementTraitDefinition.DATA_STRUCTR_DELAY_OR_INTERVAL_PROPERTY,
					DOMElementTraitDefinition.DATA_STRUCTR_INSERT_PROPERTY, DOMElementTraitDefinition.DATA_STRUCTR_FROM_WIDGET_PROPERTY,
					DOMElementTraitDefinition.DATA_STRUCTR_TREE_CHILDREN_PROPERTY, DOMElementTraitDefinition.DATA_STRUCTR_RELOAD_TARGET_PROPERTY,
					DOMElementTraitDefinition.EVENT_MAPPING_PROPERTY, TRIGGERED_ACTIONS_PROPERTY, DOMNodeTraitDefinition.RELOADING_ACTIONS_PROPERTY,
					DOMNodeTraitDefinition.FAILURE_ACTIONS_PROPERTY, DOMNodeTraitDefinition.SUCCESS_NOTIFICATION_ACTIONS_PROPERTY,
					DOMNodeTraitDefinition.FAILURE_NOTIFICATION_ACTIONS_PROPERTY, DOMElementTraitDefinition.DATA_STRUCTR_MANUAL_RELOAD_TARGET_PROPERTY
			),
			PropertyView.Html,
			newSet(
				_HTML_ONABORT_PROPERTY, _HTML_ONBLUR_PROPERTY, _HTML_ONCANPLAY_PROPERTY, _HTML_ONCANPLAYTHROUGH_PROPERTY, _HTML_ONCHANGE_PROPERTY, _HTML_ONCLICK_PROPERTY, _HTML_ONCONTEXTMENU_PROPERTY, _HTML_ONDBLCLICK_PROPERTY,
				_HTML_ONDRAG_PROPERTY, _HTML_ONDRAGEND_PROPERTY, _HTML_ONDRAGENTER_PROPERTY, _HTML_ONDRAGLEAVE_PROPERTY, _HTML_ONDRAGOVER_PROPERTY, _HTML_ONDRAGSTART_PROPERTY, _HTML_ONDROP_PROPERTY, _HTML_ONDURATIONCHANGE_PROPERTY,
				_HTML_ONEMPTIED_PROPERTY, _HTML_ONENDED_PROPERTY, _HTML_ONERROR_PROPERTY, _HTML_ONFOCUS_PROPERTY, _HTML_ONINPUT_PROPERTY, _HTML_ONINVALID_PROPERTY, _HTML_ONKEYDOWN_PROPERTY, _HTML_ONKEYPRESS_PROPERTY, _HTML_ONKEYUP_PROPERTY,
				_HTML_ONLOAD_PROPERTY, _HTML_ONLOADEDDATA_PROPERTY, _HTML_ONLOADEDMETADATA_PROPERTY, _HTML_ONLOADSTART_PROPERTY, _HTML_ONMOUSEDOWN_PROPERTY, _HTML_ONMOUSEMOVE_PROPERTY, _HTML_ONMOUSEOUT_PROPERTY,
				_HTML_ONMOUSEOVER_PROPERTY, _HTML_ONMOUSEUP_PROPERTY, _HTML_ONMOUSEWHEEL_PROPERTY, _HTML_ONPAUSE_PROPERTY, _HTML_ONPLAY_PROPERTY, _HTML_ONPLAYING_PROPERTY, _HTML_ONPROGRESS_PROPERTY, _HTML_ONRATECHANGE_PROPERTY,
				_HTML_ONREADYSTATECHANGE_PROPERTY, _HTML_ONRESET_PROPERTY, _HTML_ONSCROLL_PROPERTY, _HTML_ONSEEKED_PROPERTY, _HTML_ONSEEKING_PROPERTY, _HTML_ONSELECT_PROPERTY, _HTML_ONSHOW_PROPERTY, _HTML_ONSTALLED_PROPERTY,
				_HTML_ONSUBMIT_PROPERTY, _HTML_ONSUSPEND_PROPERTY, _HTML_ONTIMEUPDATE_PROPERTY, _HTML_ONVOLUMECHANGE_PROPERTY, _HTML_ONWAITING_PROPERTY, _HTML_DATA_PROPERTY,

				_HTML_ACCESSKEY_PROPERTY, _HTML_CLASS_PROPERTY, _HTML_CONTENTEDITABLE_PROPERTY, _HTML_CONTEXTMENU_PROPERTY, _HTML_DIR_PROPERTY, _HTML_DRAGGABLE_PROPERTY, _HTML_DROPZONE_PROPERTY,
				_HTML_HIDDEN_PROPERTY, _HTML_ID_PROPERTY, _HTML_LANG_PROPERTY, _HTML_SPELLCHECK_PROPERTY, _HTML_STYLE_PROPERTY, _HTML_TABINDEX_PROPERTY, _HTML_TITLE_PROPERTY, _HTML_TRANSLATE_PROPERTY,

				_HTML_ROLE_PROPERTY, _HTML_IS_PROPERTY, _HTML_PROPERTIES_PROPERTY
			)
		);

	}

	@Override
	public Relation getRelation() {
		return null;
	}

	private GraphObject handleCreateAction(final RenderContext renderContext, final NodeInterface entity, final Map<String, Object> parameters, final EventContext eventContext) throws FrameworkException {

		final SecurityContext securityContext = renderContext.getSecurityContext();
		final String dataTarget               = getDataTargetFromParameters(parameters, "create", true);

		// resolve target type
		Traits traits = Traits.of(dataTarget);
		if (traits == null) {

			throw new FrameworkException(422, "Cannot execute create action with target type " + dataTarget + ", type does not exist.");
		}

		removeInternalDataBindingKeys(parameters);

		// convert input
		final PropertyMap properties = PropertyMap.inputTypeToJavaType(securityContext, dataTarget, parameters);

		// create entity
		return StructrApp.getInstance(securityContext).create(dataTarget, properties);
	}

	private void handleUpdateAction(final RenderContext renderContext, final NodeInterface entity, final Map<String, Object> parameters, final EventContext eventContext) throws FrameworkException {

		final SecurityContext securityContext = renderContext.getSecurityContext();
		final String dataTarget               = getDataTargetFromParameters(parameters, "update", true);

		removeInternalDataBindingKeys(parameters);

		for (final GraphObject target : resolveDataTargets(renderContext, entity, dataTarget)) {

			// convert input
			final PropertyMap properties = PropertyMap.inputTypeToJavaType(securityContext, target.getType(), parameters);

			// update properties
			target.setProperties(securityContext, properties);

		}
	}

	private void handleDeleteAction(final RenderContext renderContext, final NodeInterface entity, final Map<String, Object> parameters, final EventContext eventContext) throws FrameworkException {

		final SecurityContext securityContext = renderContext.getSecurityContext();
		final App app                         = StructrApp.getInstance(securityContext);
		final String dataTarget               = getDataTargetFromParameters(parameters, "delete", true);

		for (final GraphObject target : resolveDataTargets(renderContext, entity, dataTarget)) {

			if (target.isNode()) {

				app.delete((NodeInterface)target);

			} else {

				app.delete((RelationshipInterface)target);
			}
		}
	}

	private Object handleAppendChildAction(final ActionContext actionContext, final NodeInterface entity, final Map<String, Object> parameters, final EventContext eventContext) throws FrameworkException {

		final SecurityContext securityContext = actionContext.getSecurityContext();
		final String dataTarget = getDataTargetFromParameters(parameters, "append-child", true);

		// fetch child ID
		final String childId = (String) parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_CHILDID);
		if (childId == null) {

			throw new FrameworkException(422, "Cannot execute append-child action without child UUID (data-child-id attribute).");
		}

		// load child node
		final NodeInterface child = StructrApp.getInstance(securityContext).getNodeById(StructrTraits.DOM_NODE, childId);
		if (child == null) {

			throw new FrameworkException(422, "Cannot execute append-child action without child (object with ID not found or not a DOMNode).");
		}

		removeInternalDataBindingKeys(parameters);

		for (final GraphObject target : resolveDataTargets(actionContext, entity, dataTarget)) {

			if (target.is(StructrTraits.DOM_ELEMENT)) {

				final DOMElement domTarget = target.as(DOMElement.class);

				domTarget.appendChild(child.as(DOMNode.class));

			} else {

				throw new FrameworkException(422, "Cannot execute append-child action on " + target.getClass().getSimpleName() + " (must be a DOMElement).");
			}
		}

		return null;
	}

	private Object handleRemoveChildAction(final RenderContext renderContext, final NodeInterface entity, final Map<String, Object> parameters, final EventContext eventContext) throws FrameworkException {

		final SecurityContext securityContext = renderContext.getSecurityContext();
		final String dataTarget               = getDataTargetFromParameters(parameters, "remove-child", true);

		// fetch child ID
		final String childId = (String) parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_CHILDID);
		if (childId == null) {

			throw new FrameworkException(422, "Cannot execute remove-child action without child UUID (data-child-id attribute).");
		}

		// load child node
		final NodeInterface child = StructrApp.getInstance(securityContext).getNodeById(StructrTraits.DOM_NODE, childId);
		if (child == null) {

			throw new FrameworkException(422, "Cannot execute remove-child action without child (object with ID not found or not a DOMNode).");
		}

		removeInternalDataBindingKeys(parameters);

		for (final GraphObject target : resolveDataTargets(renderContext, entity, dataTarget)) {

			if (target.is(StructrTraits.DOM_ELEMENT)) {

				final DOMElement parent = target.as(DOMElement.class);

				RemoveDOMChildFunction.apply(renderContext.getSecurityContext(), parent, child.as(DOMNode.class));

			} else {

				throw new FrameworkException(422, "Cannot execute remove-child action on " + target.getClass().getSimpleName() + " (must be a DOMElement).");
			}
		}

		return null;
	}

	private Object handleInsertHtmlAction(final RenderContext renderContext, final NodeInterface entity, final Map<String, Object> parameters, final EventContext eventContext) throws FrameworkException {

		final SecurityContext securityContext = renderContext.getSecurityContext();
		final String dataTarget               = getDataTargetFromParameters(parameters, "insert-html", true);

		final String sourceObjectId = (String)parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_SOURCEOBJECT);
		if (sourceObjectId == null) {

			throw new FrameworkException(422, "Cannot execute insert-html action without html source object UUID (data-source-object).");
		}

		final NodeInterface sourceObject = StructrApp.getInstance(securityContext).getNodeById(StructrTraits.NODE_INTERFACE, sourceObjectId);
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

		for (final GraphObject target : resolveDataTargets(renderContext, entity, dataTarget)) {

			if (target instanceof NodeInterface node && node.is(StructrTraits.DOM_ELEMENT)) {

				return InsertHtmlFunction.apply(securityContext, node, htmlSource);

			} else {

				throw new FrameworkException(422, "Cannot execute insert-html action on " + target.getClass().getSimpleName() + " (must be a DOMElement).");
			}
		}

		return null;
	}

	private Object handleReplaceHtmlAction(final RenderContext renderContext, final NodeInterface entity, final Map<String, Object> parameters, final EventContext eventContext) throws FrameworkException {

		final SecurityContext securityContext = renderContext.getSecurityContext();
		final String dataTarget               = getDataTargetFromParameters(parameters, "replace-html", true);

		// fetch child ID
		final String childId = (String) parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_CHILDID);
		if (childId == null) {

			throw new FrameworkException(422, "Cannot execute replace-html action without child UUID (data-child-id attribute).");
		}

		// load child node
		final NodeInterface child = StructrApp.getInstance(securityContext).getNodeById(StructrTraits.DOM_NODE, childId);
		if (child == null) {

			throw new FrameworkException(422, "Cannot execute replace-html action without child (object with ID not found or not a DOMNode).");
		}

		final String sourceObjectId = (String) parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_SOURCEOBJECT);
		if (sourceObjectId == null) {

			throw new FrameworkException(422, "Cannot execute replace-html action without html source object UUID (data-source-object).");
		}

		final NodeInterface sourceObject = StructrApp.getInstance(securityContext).getNodeById(StructrTraits.NODE_INTERFACE, sourceObjectId);
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

		for (final GraphObject target : resolveDataTargets(renderContext, entity, dataTarget)) {

			if (target instanceof NodeInterface n && n.is(StructrTraits.DOM_ELEMENT)) {

				final DOMElement parent = n.as(DOMElement.class);

				return ReplaceDOMChildFunction.apply(securityContext, parent, child.as(DOMNode.class), htmlSource);

			} else {

				throw new FrameworkException(422, "Cannot execute replace-html action on " + target.getClass().getSimpleName() + " (must be a DOMElement).");
			}
		}

		return null;
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

	private Object handleSignInAction(final RenderContext renderContext, final NodeInterface entity, final Map<String, Object> parameters, final EventContext eventContext) throws FrameworkException {

		removeInternalDataBindingKeys(parameters);

		final Principal currentUser              = renderContext.getSecurityContext().getUser(false);
		final LoginResourceHandler loginResource = new LoginResourceHandler(new RESTCall("/login", PropertyView.Public, true, AbstractDataServlet.getTypeOrDefault(currentUser, StructrTraits.USER)));
		final Map<String, Object> properties     = new LinkedHashMap<>();

		for (final Entry<String, Object> entry : parameters.entrySet()) {

			final String key   = entry.getKey();
			final String value = (String) entry.getValue();

			properties.put(key, value);
		}

		return loginResource.doPost(renderContext.getSecurityContext(), properties);
	}

	private Object handleSignOutAction(final RenderContext renderContext, final NodeInterface entity, final Map<String, Object> parameters, final EventContext eventContext) throws FrameworkException {

		removeInternalDataBindingKeys(parameters);

		final Principal currentUser                = renderContext.getSecurityContext().getUser(false);
		final LogoutResourceHandler logoutResource = new LogoutResourceHandler(new RESTCall("/logout", PropertyView.Public, true, AbstractDataServlet.getTypeOrDefault(currentUser, StructrTraits.USER)));
		final Map<String, Object> properties       = new LinkedHashMap<>();

		for (final Entry<String, Object> entry : parameters.entrySet()) {

			final String key   = entry.getKey();
			final String value = (String) entry.getValue();

			properties.put(key, value);
		}

		return logoutResource.doPost(renderContext.getSecurityContext(), properties);
	}

	private Object handleSignUpAction(final RenderContext renderContext, final NodeInterface entity, final Map<String, Object> parameters, final EventContext eventContext) throws FrameworkException {

		final Principal currentUser          = renderContext.getSecurityContext().getUser(false);
		final Map<String, Object> properties = new LinkedHashMap<>();

		removeInternalDataBindingKeys(parameters);

		for (final Entry<String, Object> entry : parameters.entrySet()) {

			final String key   = entry.getKey();
			final String value = (String) entry.getValue();

			if (value != null) properties.put(key, value);
		}

		final RegistrationResourceHandler registrationResource = new RegistrationResourceHandler(new RESTCall("/registration", PropertyView.Public, true, AbstractDataServlet.getTypeOrDefault(currentUser, StructrTraits.USER)));

		return registrationResource.doPost(renderContext.getSecurityContext(), properties);
	}

	private Object handleResetPasswordAction(final RenderContext renderContext, final NodeInterface entity, final Map<String, Object> parameters, final EventContext eventContext) throws FrameworkException {

		final Principal currentUser          = renderContext.getSecurityContext().getUser(false);
		final Map<String, Object> properties = new LinkedHashMap<>();

		removeInternalDataBindingKeys(parameters);

		for (final Entry<String, Object> entry : parameters.entrySet()) {

			final String key   = entry.getKey();
			final String value = (String) entry.getValue();

			if (value != null) properties.put(key, value);
		}

		final ResetPasswordResourceHandler resetPasswordResource = new ResetPasswordResourceHandler(new RESTCall("/reset-password", PropertyView.Public, true, AbstractDataServlet.getTypeOrDefault(currentUser, StructrTraits.USER)));

		return resetPasswordResource.doPost(renderContext.getSecurityContext(), properties);
	}

	private Object handleFlowAction(final RenderContext renderContext, final NodeInterface entity, final java.util.Map<String, java.lang.Object> parameters, final EventContext eventContext, final String flowName) throws FrameworkException {

		if (flowName != null) {

			return Scripting.evaluate(renderContext,  entity, "${flow('" + flowName.trim() + "')}", "flow query");

		} else {

			throw new FrameworkException(422, "Cannot execute Flow because no or empty name was provided.");
		}

	}

	private Object handleCustomAction(final RenderContext renderContext, final NodeInterface entity, final Map<String, Object> parameters, final EventContext eventContext, final String methodName) throws FrameworkException {

		final String dataTarget = getDataTargetFromParameters(parameters, "custom", false);

		// Empty dataTarget means no database object and no type, so it can only be a global (schema) method
		if (StringUtils.isNotBlank(methodName) && StringUtils.isBlank(dataTarget)) {

			removeInternalDataBindingKeys(parameters);

			return Actions.callWithSecurityContext(methodName, renderContext.getSecurityContext(), parameters);
		}

		if (Settings.isValidUuid(dataTarget)) {

			final List<GraphObject> targets = resolveDataTargets(renderContext, entity, dataTarget);
			final Logger logger             = LoggerFactory.getLogger(getClass());

			if (targets.size() > 1) {
				logger.warn("Custom action has multiple targets, this is not supported yet. Returning only the result of the first target.");
			}

			removeInternalDataBindingKeys(parameters);

			for (final GraphObject target : targets) {

				final AbstractMethod method = Methods.resolveMethod(target.getTraits(), methodName);
				if (method != null) {

					if (method.shouldReturnRawResult()) {
						renderContext.getSecurityContext().enableReturnRawResult();
					}

					return method.execute(renderContext.getSecurityContext(), target, NamedArguments.fromMap(parameters), new EvaluationHints());

				} else {

					throw new FrameworkException(422, "Cannot execute method " + target.getClass().getSimpleName() + "." + methodName + ": method not found.");
				}
			}

		} else {

			if (dataTarget != null) {

				// add support for static methods
				if (Traits.exists(dataTarget)) {

					final Traits traits         = Traits.of(dataTarget);
					final AbstractMethod method = Methods.resolveMethod(traits, methodName);

					if (method != null) {

						if (method.shouldReturnRawResult()) {
							renderContext.getSecurityContext().enableReturnRawResult();
						}

						return method.execute(renderContext.getSecurityContext(), null, NamedArguments.fromMap(parameters), new EvaluationHints());

					} else {

						throw new FrameworkException(422, "Cannot execute static  method " + methodName + ": method not found.");
					}

				} else {

					throw new FrameworkException(422, "Cannot execute static method " + dataTarget + "." + methodName + ": type not found.");
				}

			} else {

				throw new FrameworkException(422, "Custom action has empty dataTarget.");
			}
		}

		return null;
	}

	private void removeInternalDataBindingKeys(final Map<String, Object> parameters) {

		parameters.remove(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRIDEXPRESSION);
		parameters.remove(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRTARGET);
		parameters.remove(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRDATATYPE);
		parameters.remove(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRMETHOD);
		parameters.remove(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRFLOW);
		parameters.remove(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_CHILDID);
		parameters.remove(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_SOURCEOBJECT);
		parameters.remove(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_SOURCEPROPERTY);
		parameters.remove(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_HTMLEVENT);
	}

	private String getDataTargetFromParameters(final Map<String, Object> parameters, final String action, final boolean throwExceptionIfEmpty) throws FrameworkException {

		// Support old and new parameters
		final String idExpression  = (String) parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRIDEXPRESSION);
		final String structrTarget = (String) parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRTARGET);
		final String dataTarget    = idExpression != null ? idExpression : structrTarget;

		if (StringUtils.isBlank(dataTarget) && throwExceptionIfEmpty) {

			throw new FrameworkException(422, "Cannot execute " + action + " action without target UUID (data-structr-target attribute).");
		}

		return dataTarget;
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
			final PropertyKey<String> titleKey = traits.key(ActionMappingTraitDefinition.DIALOG_TITLE_PROPERTY);
			final PropertyKey<String> textKey  = traits.key(ActionMappingTraitDefinition.DIALOG_TEXT_PROPERTY);

			final String dialogTitle = triggeredAction.getPropertyWithVariableReplacement(renderContext, titleKey);
			final String dialogText  = triggeredAction.getPropertyWithVariableReplacement(renderContext, textKey);

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
					out.append(" data-structr-success-notifications-custom-dialog-element=\"").append(generateDataAttributesForIdList(renderContext, triggeredAction, ActionMappingTraitDefinition.SUCCESS_NOTIFICATION_ELEMENTS_PROPERTY)).append("\"");
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
					out.append(" data-structr-failure-notifications-custom-dialog-element=\"").append(generateDataAttributesForIdList(renderContext, triggeredAction, ActionMappingTraitDefinition.FAILURE_NOTIFICATION_ELEMENTS_PROPERTY)).append("\"");
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
		final String successPartial   = triggeredAction.getSuccessPartial();
		final String successURL       = triggeredAction.getPropertyWithVariableReplacement(renderContext, traits.key(ActionMappingTraitDefinition.SUCCESS_URL_PROPERTY));
		final String successEvent     = triggeredAction.getSuccessEvent();

		String successTargetString = null;

		if (StringUtils.isNotBlank(successBehaviour)) {

			switch (successBehaviour) {
				case "partial-refresh":
					successTargetString = successPartial;
					break;
				case "partial-refresh-linked":
					successTargetString = generateDataAttributesForIdList(renderContext, triggeredAction, ActionMappingTraitDefinition.SUCCESS_TARGETS_PROPERTY);
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

		final String idExpression = triggeredAction.getIdExpression();
		if (StringUtils.isNotBlank(idExpression)) {
			out.append(" data-structr-target=\"").append(idExpression).append("\"");
		}

		final String action = triggeredAction.getAction();
		if ("create".equals(action)) {

			final String dataType = triggeredAction.getDataType();
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
		final String failurePartial   = triggeredAction.getFailurePartial();
		final String failureURL       = triggeredAction.getPropertyWithVariableReplacement(renderContext, traits.key(ActionMappingTraitDefinition.FAILURE_URL_PROPERTY));
		final String failureEvent     = triggeredAction.getFailureEvent();

		String failureTargetString = null;

		if (StringUtils.isNotBlank(failureBehaviour)) {

			switch (failureBehaviour) {
				case "partial-refresh":
					failureTargetString = failurePartial;
					break;
				case "partial-refresh-linked":
					failureTargetString = generateDataAttributesForIdList(renderContext, triggeredAction, ActionMappingTraitDefinition.FAILURE_TARGETS_PROPERTY);
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

					if (possibleReloadTargetNode.is(StructrTraits.DOM_ELEMENT)) {

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
			node.setProperty(traits.key(RELOAD_SOURCES_PROPERTY), actualReloadSources);
			node.setProperty(traits.key(RELOAD_TARGETS_PROPERTY), actualReloadTargets);

			domElement.updateHasSharedComponentFlag();

		} catch (Throwable t) {

			t.printStackTrace();
		}
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
			if (dataKey != null && renderContext.getDataNode(dataKey) != null) {

				selector += "[data-repeater-data-object-id='" + renderContext.getDataNode(dataKey).getUuid() + "']";
			}

			selectors.add(selector);
		}

		return StringUtils.join(selectors, ",");
	}
}
