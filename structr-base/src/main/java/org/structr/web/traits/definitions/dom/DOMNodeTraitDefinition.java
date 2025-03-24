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


import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.structr.api.util.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.event.RuntimeEventLog;
import org.structr.common.helper.CaseHelper;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.datasources.DataSources;
import org.structr.core.datasources.GraphDataSource;
import org.structr.core.entity.Relation;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
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
import org.structr.core.traits.operations.nodeinterface.VisitForUsage;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.property.CustomHtmlAttributeProperty;
import org.structr.web.property.DOMNodeSortedChildrenProperty;
import org.structr.web.traits.definitions.html.Option;
import org.structr.web.traits.operations.*;
import org.structr.web.traits.wrappers.dom.DOMNodeTraitWrapper;
import org.w3c.dom.DOMException;

import java.util.*;

/**
 * Combines NodeInterface and org.w3c.dom.Node.
 */
public class DOMNodeTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String PARENT_PROPERTY                         = "parent";
	public static final String CHILDREN_PROPERTY                       = "children";
	public static final String PREVIOUS_SIBLING_PROPERTY               = "previousSibling";
	public static final String NEXT_SIBLING_PROPERTY                   = "nextSibling";
	public static final String SHARED_COMPONENT_PROPERTY               = "sharedComponent";
	public static final String SYNCED_NODES_PROPERTY                   = "syncedNodes";
	public static final String OWNER_DOCUMENT_PROPERTY                 = "ownerDocument";
	public static final String RELOADING_ACTIONS_PROPERTY              = "reloadingActions";
	public static final String FAILURE_ACTIONS_PROPERTY                = "failureActions";
	public static final String SUCCESS_NOTIFICATION_ACTIONS_PROPERTY   = "successNotificationActions";
	public static final String FAILURE_NOTIFICATION_ACTIONS_PROPERTY   = "failureNotificationActions";
	public static final String SORTED_CHILDREN_PROPERTY                = "sortedChildren";
	public static final String CHILDREN_IDS_PROPERTY                   = "childrenIds";
	public static final String NEXT_SIBLING_ID_PROPERTY                = "nextSiblingId";
	public static final String PAGE_ID_PROPERTY                        = "pageId";
	public static final String PARENT_ID_PROPERTY                      = "parentId";
	public static final String SHARED_COMPONENT_ID_PROPERTY            = "sharedComponentId";
	public static final String SYNCED_NODES_IDS_PROPERTY               = "syncedNodesIds";
	public static final String DATA_KEY_PROPERTY                       = "dataKey";
	public static final String CYPHER_QUERY_PROPERTY                   = "cypherQuery";
	public static final String REST_QUERY_PROPERTY                     = "restQuery";
	public static final String FUNCTION_QUERY_PROPERTY                 = "functionQuery";
	public static final String SHOW_FOR_LOCALES_PROPERTY               = "showForLocales";
	public static final String HIDE_FOR_LOCALES_PROPERTY               = "hideForLocales";
	public static final String SHOW_CONDITIONS_PROPERTY                = "showConditions";
	public static final String HIDE_CONDITIONS_PROPERTY                = "hideConditions";
	public static final String SHARED_COMPONENT_CONFIGURATION_PROPERTY = "sharedComponentConfiguration";
	public static final String DATA_STRUCTR_ID_PROPERTY                = "data-structr-id";
	public static final String DATA_STRUCTR_HASH_PROPERTY              = "data-structr-hash";
	public static final String DONT_CACHE_PROPERTY                     = "dontCache";
	public static final String IS_DOM_NODE_PROPERTY                    = "isDOMNode";
	public static final String HAS_SHARED_COMPONENT_PROPERTY           = "hasSharedComponent";
	public static final String DOM_SORT_POSITION_PROPERTY              = "domSortPosition";
	public static final String FLOW_PROPERTY                           = "flow";
	public static final String PROCESS_SUCCESS_SHOW_ACTIONS_PROPERTY   = "processSuccessShowActions";
	public static final String PROCESS_SUCCESS_HIDE_ACTIONS_PROPERTY   = "processSuccessHideActions";
	public static final String PROCESS_FAILURE_SHOW_ACTIONS_PROPERTY   = "processFailureShowActions";
	public static final String PROCESS_FAILURE_HIDE_ACTIONS_PROPERTY   = "processFailureHideActions";

	private static final String[] rawProps = new String[] {
			DATA_KEY_PROPERTY, REST_QUERY_PROPERTY, CYPHER_QUERY_PROPERTY, FUNCTION_QUERY_PROPERTY, Option.SELECTEDVALUES_PROPERTY, FLOW_PROPERTY,
			"hideOnIndex", "hideOnDetail", // FIXME: these dont exist anymore!?
			SHOW_FOR_LOCALES_PROPERTY, HIDE_FOR_LOCALES_PROPERTY, SHOW_CONDITIONS_PROPERTY, HIDE_CONDITIONS_PROPERTY
	};

	private static final Set<String> DataAttributeOutputBlacklist = Set.of(DOMElementTraitDefinition.DATA_STRUCTR_MANUAL_RELOAD_TARGET_PROPERTY);

	public DOMNodeTraitDefinition() {
		super(StructrTraits.DOM_NODE);
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(

			OnCreation.class,
			new OnCreation() {

				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

					final DOMNode domNode = graphObject.as(DOMNode.class);

					domNode.checkName(errorBuffer);
					domNode.syncName(errorBuffer);
				}
			},

			OnModification.class,
			new OnModification() {

				@Override
				public void onModification(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

					final DOMNode domNode = graphObject.as(DOMNode.class);

					domNode.increasePageVersion();
					domNode.checkName(errorBuffer);
					domNode.syncName(errorBuffer);

					// acknowledge all events for this node when it is modified
					RuntimeEventLog.acknowledgeAllEventsForId(domNode.getUuid());
				}
			}
		);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		final Map<Class, FrameworkMethod> methods = new LinkedHashMap<>();

		methods.put(
			VisitForUsage.class,
			new VisitForUsage() {

				@Override
				public void visitForUsage(final NodeInterface obj, final Map<String, Object> data) {

					getSuper().visitForUsage(obj, data);

					final DOMNode node = obj.as(DOMNode.class);
					final Page page    = node.getOwnerDocument();

					if (page != null) {

						data.put("page", page.getName());
					}

					data.put(DOMElementTraitDefinition.PATH_PROPERTY, node.getPagePath());
				}
			}
		);

		methods.put(

			Render.class,
			new Render() {

				@Override
				public void render(final DOMNode node, final RenderContext renderContext, final int depth) throws FrameworkException {

					final SecurityContext securityContext = renderContext.getSecurityContext();
					final RenderContext.EditMode editMode = renderContext.getEditMode(securityContext.getUser(false));

					// admin-only edit modes ==> visibility check not necessary
					final boolean isAdminOnlyEditMode = (RenderContext.EditMode.RAW.equals(editMode) || RenderContext.EditMode.WIDGET.equals(editMode) || RenderContext.EditMode.DEPLOYMENT.equals(editMode));
					final boolean isPartial = renderContext.isPartialRendering(); // renderContext.getPage() == null;

					if (!isAdminOnlyEditMode && !securityContext.isVisible(node)) {
						return;
					}

					/*
					// special handling for tree items that explicitly opt-in to be controlled automatically, configured with the toggle-tree-item event.
					final String treeItemDataKey = thisNode.getProperty(StructrApp.key(DOMElement.class, "data-structr-tree-children"));
					if (treeItemDataKey != null) {

						final GraphObject treeItem = renderContext.getDataNode(treeItemDataKey);
						if (treeItem != null) {

							final String key = thisNode.getTreeItemSessionIdentifier(treeItem.getUuid());

							if (thisNode.getSessionAttribute(renderContext.getSecurityContext(), key) == null) {

								// do not render children of tree elements
								return;
							}
						}
					}
					*/

					if (isAdminOnlyEditMode) {

						node.renderContent(renderContext, depth);

					} else {

						final String subKey = node.getDataKey();

						if (StringUtils.isNotBlank(subKey)) {

							// fetch (optional) list of external data elements
							final Iterable<GraphObject> listData = DOMNodeTraitDefinition.checkListSources(node, securityContext, renderContext);

							final PropertyKey propertyKey;

							// Make sure the closest 'page' keyword is always set also for partials
							if (depth == 0 && isPartial) {

								renderContext.setPage(node.getClosestPage());

							}

							final GraphObject dataObject = renderContext.getDataNode(subKey); // renderContext.getSourceDataObject();

							// Render partial with possible top-level repeater limited to a single data object
							if (depth == 0 && isPartial && dataObject != null) {

								renderContext.putDataObject(subKey, dataObject);
								node.renderContent(renderContext, depth);

							} else {

								final GraphObject currentDataNode = renderContext.getDataNode(subKey); // renderContext.getDataObject();

								if (Iterables.isEmpty(listData) && currentDataNode != null) {

									// There are two alternative ways of retrieving sub elements:
									// First try to get generic properties,
									// if that fails, try to create a propertyKey for the subKey
									final Object elements = currentDataNode.getProperty(new GenericProperty(subKey));

									renderContext.setRelatedProperty(new GenericProperty(subKey));
									renderContext.setSourceDataObject(currentDataNode);

									if (elements != null) {

										if (elements instanceof Iterable) {

											for (Object o : (Iterable) elements) {

												if (o instanceof GraphObject) {

													GraphObject graphObject = (GraphObject) o;
													renderContext.putDataObject(subKey, graphObject);
													node.renderContent(renderContext, depth);

												}
											}

										}

									} else {

										final Traits traits = currentDataNode.getTraits();
										if (traits.hasKey(subKey)) {

											propertyKey = traits.key(subKey);
											renderContext.setRelatedProperty(propertyKey);

											final Object value = currentDataNode.getProperty(propertyKey);
											if (value != null) {

												if (value instanceof Iterable) {

													for (final Object o : ((Iterable) value)) {

														if (o instanceof GraphObject) {

															renderContext.putDataObject(subKey, (GraphObject) o);
															node.renderContent(renderContext, depth);

														}
													}
												}
											}
										}

									}

									// reset data node in render context
									renderContext.setDataObject(currentDataNode);
									renderContext.setRelatedProperty(null);

								} else {

									renderContext.setListSource(listData);
									node.renderNodeList(securityContext, renderContext, depth, subKey);

								}
							}

						} else {

							node.renderContent(renderContext, depth);
						}
					}
				}
			}
		);

		methods.put(

			DoAdopt.class,
			new DoAdopt() {

				@Override
				public DOMNode doAdopt(final DOMNode node, final Page _page) throws DOMException {

					if (_page != null) {

						try {

							node.setOwnerDocument(_page);

						} catch (FrameworkException fex) {

							throw new DOMException(DOMException.INVALID_STATE_ERR, fex.getMessage());

						}
					}

					return node;
				}
			}
		);

		methods.put(

			GetCssClass.class,
			new GetCssClass() {

				@Override
				public String getCssClass(final NodeInterface node) {
					return null;
				}
			}
		);

		methods.put(

			GetPagePath.class,
			new GetPagePath() {

				@Override
				public String getPagePath(final NodeInterface node) {

					String cachedPagePath = (String) node.getTemporaryStorage().get("cachedPagePath");
					if (cachedPagePath == null) {

						final StringBuilder buf = new StringBuilder();
						DOMNode current = node.as(DOMNode.class);

						while (current != null) {

							buf.insert(0, "/" + current.getContextName());
							current = current.getParent();
						}

						cachedPagePath = buf.toString();

						node.getTemporaryStorage().put("cachedPagePath", cachedPagePath);
					}

					return cachedPagePath;
				}
			}
		);

		methods.put(

			GetNodeValue.class,
			new GetNodeValue() {

				@Override
				public String getNodeValue(final NodeInterface node) {
					return null;
				}
			}
		);

		methods.put(

			RenderCustomAttributes.class,
			new RenderCustomAttributes() {

				@Override
				public void renderCustomAttributes(final DOMNode node, final AsyncBuffer out, final SecurityContext securityContext, final RenderContext renderContext) throws FrameworkException {

					final RenderContext.EditMode editMode = renderContext.getEditMode(securityContext.getUser(false));
					final Traits traits = node.getTraits();
					final NodeInterface wrappedNode = node;

					Set<PropertyKey> dataAttributes = node.getDataPropertyKeys();

					if (RenderContext.EditMode.DEPLOYMENT.equals(editMode)) {
						List sortedAttributes = new LinkedList(dataAttributes);
						Collections.sort(sortedAttributes);
						dataAttributes = new LinkedHashSet<>(sortedAttributes);
					}

					for (final PropertyKey key : dataAttributes) {

						// do not render attributes that are on the blacklist
						if (DataAttributeOutputBlacklist.contains(key.jsonName()) && !RenderContext.EditMode.DEPLOYMENT.equals(editMode)) {
							continue;
						}

						String value = "";

						if (RenderContext.EditMode.DEPLOYMENT.equals(editMode)) {

							final Object obj = wrappedNode.getProperty(key);
							if (obj != null) {

								value = obj.toString();
							}

						} else {

							value = wrappedNode.getPropertyWithVariableReplacement(renderContext, key);
							if (value != null) {

								value = value.trim();
							}
						}

						if (!(RenderContext.EditMode.RAW.equals(editMode) || RenderContext.EditMode.WIDGET.equals(editMode))) {

							value = DOMNode.escapeForHtmlAttributes(value);
						}

						if (StringUtils.isNotBlank(value)) {

							if (key instanceof CustomHtmlAttributeProperty) {
								out.append(" ").append(((CustomHtmlAttributeProperty) key).cleanName()).append("=\"").append(value).append("\"");
							} else {
								out.append(" ").append(key.dbName()).append("=\"").append(value).append("\"");
							}
						}
					}

					if (RenderContext.EditMode.DEPLOYMENT.equals(editMode) || RenderContext.EditMode.RAW.equals(editMode) || RenderContext.EditMode.WIDGET.equals(editMode)) {

						if (RenderContext.EditMode.DEPLOYMENT.equals(editMode)) {

							// export name property if set
							final String name = wrappedNode.getProperty(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY));
							if (name != null) {

								out.append(" data-structr-meta-name=\"").append(DOMNode.escapeForHtmlAttributes(name)).append("\"");
							}

							out.append(" data-structr-meta-id=\"").append(node.getUuid()).append("\"");
						}

						for (final String p : rawProps) {

							if (traits.hasKey(p)) {

								final PropertyKey key = traits.key(p);
								final Object value    = wrappedNode.getProperty(key);

								if (value != null) {

									final String htmlName    = "data-structr-meta-" + CaseHelper.toUnderscore(p, false).replaceAll("_", "-");
									final boolean isBoolean  = key instanceof BooleanProperty;
									final String stringValue = value.toString();

									if ((isBoolean && "true".equals(stringValue)) || (!isBoolean && StringUtils.isNotBlank(stringValue))) {
										out.append(" ").append(htmlName).append("=\"").append(DOMNode.escapeForHtmlAttributes(stringValue)).append("\"");
									}
								}
							}
						}
					}
				}
			}
		);

		methods.put(

			HandleNewChild.class,
			new HandleNewChild() {

				@Override
				public void handleNewChild(DOMNode node, DOMNode newChild) throws FrameworkException {

					final Page page = node.getOwnerDocument();

					newChild.setOwnerDocument(page);

					for (final NodeInterface child : newChild.getAllChildNodes()) {

						final DOMNode d = child.as(DOMNode.class);

						d.setOwnerDocument(page);
					}
				}
			}
		);

		methods.put(

			CheckHierarchy.class,
			new CheckHierarchy() {

				@Override
				public void checkHierarchy(final DOMNode thisNode, final DOMNode otherNode) throws FrameworkException {

					// verify that the other node is not this node
					if (thisNode.isSameNode(otherNode)) {
						throw new FrameworkException(422, DOMNode.HIERARCHY_REQUEST_ERR_MESSAGE_SAME_NODE);
					}

					// verify that otherNode is not one of the
					// the ancestors of this node
					// (prevent circular relationships)
					DOMNode _parent = thisNode.getParent();
					while (_parent != null) {

						if (_parent.isSameNode(otherNode)) {
							throw new FrameworkException(422, DOMNode.HIERARCHY_REQUEST_ERR_MESSAGE_ANCESTOR);
						}

						_parent = _parent.getParent();
					}

					// TODO: check hierarchy constraints imposed by the schema
					// validation successful
					return;
				}
			}
		);

		methods.put(

			RenderManagedAttributes.class,
			new RenderManagedAttributes() {

				@Override
				public void renderManagedAttributes(final NodeInterface node, final AsyncBuffer out, final SecurityContext securityContext, final RenderContext renderContext) throws FrameworkException {
				}
			}
		);

		methods.put(

			ContentEquals.class,
			new ContentEquals() {

				@Override
				public boolean contentEquals(final DOMNode elem, final DOMNode node) {
					return false;
				}
			}
		);

		return methods;
	}

	@Override
	public Map<Class, RelationshipTraitFactory> getRelationshipTraitFactories() {
		return Map.of();
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			DOMNode.class, (traits, node) -> new DOMNodeTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {
		return Set.of();
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<NodeInterface> parentProperty                               = new StartNode(PARENT_PROPERTY, StructrTraits.DOM_NODE_CONTAINS_DOM_NODE).category(DOMNode.PAGE_CATEGORY);
		final Property<Iterable<NodeInterface>> childrenProperty                   = new EndNodes(CHILDREN_PROPERTY, StructrTraits.DOM_NODE_CONTAINS_DOM_NODE).category(DOMNode.PAGE_CATEGORY);
		final Property<NodeInterface> previousSiblingProperty                      = new StartNode(PREVIOUS_SIBLING_PROPERTY, StructrTraits.DOM_NODE_CONTAINS_NEXT_SIBLING_DOM_NODE).category(DOMNode.PAGE_CATEGORY);
		final Property<NodeInterface> nextSiblingProperty                          = new EndNode(NEXT_SIBLING_PROPERTY, StructrTraits.DOM_NODE_CONTAINS_NEXT_SIBLING_DOM_NODE).category(DOMNode.PAGE_CATEGORY);
		final Property<NodeInterface> sharedComponentProperty                      = new StartNode(SHARED_COMPONENT_PROPERTY, StructrTraits.DOM_NODE_SYNC_DOM_NODE).category(DOMNode.PAGE_CATEGORY);
		final Property<Iterable<NodeInterface>> syncedNodesProperty                = new EndNodes(SYNCED_NODES_PROPERTY, StructrTraits.DOM_NODE_SYNC_DOM_NODE).category(DOMNode.PAGE_CATEGORY);
		final Property<NodeInterface> ownerDocumentProperty                        = new EndNode(OWNER_DOCUMENT_PROPERTY, StructrTraits.DOM_NODE_PAGE_PAGE).category(DOMNode.PAGE_CATEGORY);
		final Property<Iterable<NodeInterface>> reloadingActionsProperty           = new EndNodes(RELOADING_ACTIONS_PROPERTY, StructrTraits.DOM_NODE_SUCCESS_TARGET_ACTION_MAPPING).category(DOMNode.EVENT_ACTION_MAPPING_CATEGORY);
		final Property<Iterable<NodeInterface>> failureActionsProperty             = new EndNodes(FAILURE_ACTIONS_PROPERTY, StructrTraits.DOM_NODE_FAILURE_TARGET_ACTION_MAPPING).category(DOMNode.EVENT_ACTION_MAPPING_CATEGORY);
		final Property<Iterable<NodeInterface>> successNotificationActionsProperty = new EndNodes(SUCCESS_NOTIFICATION_ACTIONS_PROPERTY, StructrTraits.DOM_NODE_SUCCESS_NOTIFICATION_ELEMENT_ACTION_MAPPING).category(DOMNode.EVENT_ACTION_MAPPING_CATEGORY);
		final Property<Iterable<NodeInterface>> failureNotificationActionsProperty = new EndNodes(FAILURE_NOTIFICATION_ACTIONS_PROPERTY, StructrTraits.DOM_NODE_FAILURE_NOTIFICATION_ELEMENT_ACTION_MAPPING).category(DOMNode.EVENT_ACTION_MAPPING_CATEGORY);
		final Property<Iterable<NodeInterface>> processSuccessShowActions          = new EndNodes(PROCESS_SUCCESS_SHOW_ACTIONS_PROPERTY, StructrTraits.DOM_NODE_PROCESS_SHOW_ELEMENT_ON_SUCCESS_ACTION_MAPPING).category(DOMNode.EVENT_ACTION_MAPPING_CATEGORY);
		final Property<Iterable<NodeInterface>> processSuccessHideActions          = new EndNodes(PROCESS_SUCCESS_HIDE_ACTIONS_PROPERTY, StructrTraits.DOM_NODE_PROCESS_HIDE_ELEMENT_ON_SUCCESS_ACTION_MAPPING).category(DOMNode.EVENT_ACTION_MAPPING_CATEGORY);
		final Property<Iterable<NodeInterface>> processFailureShowActions          = new EndNodes(PROCESS_FAILURE_SHOW_ACTIONS_PROPERTY, StructrTraits.DOM_NODE_PROCESS_SHOW_ELEMENT_ON_FAILURE_ACTION_MAPPING).category(DOMNode.EVENT_ACTION_MAPPING_CATEGORY);
		final Property<Iterable<NodeInterface>> processFailureHideActions          = new EndNodes(PROCESS_FAILURE_HIDE_ACTIONS_PROPERTY, StructrTraits.DOM_NODE_PROCESS_HIDE_ELEMENT_ON_FAILURE_ACTION_MAPPING).category(DOMNode.EVENT_ACTION_MAPPING_CATEGORY);
		final Property<Iterable<DOMNode>> sortedChildrenProperty                   = new DOMNodeSortedChildrenProperty(SORTED_CHILDREN_PROPERTY).typeHint("DOMNode[]");
		final Property<String> childrenIdsProperty                                 = new CollectionIdProperty(CHILDREN_IDS_PROPERTY, StructrTraits.DOM_NODE, DOMNodeTraitDefinition.CHILDREN_PROPERTY, StructrTraits.DOM_NODE).category("Page Structure");
		final Property<String> nextSiblingIdProperty                               = new EntityIdProperty(NEXT_SIBLING_ID_PROPERTY, StructrTraits.DOM_NODE, NEXT_SIBLING_PROPERTY, StructrTraits.DOM_NODE).category("Page Structure");
		final Property<String> pageIdProperty                                      = new EntityIdProperty(PAGE_ID_PROPERTY, StructrTraits.DOM_NODE, OWNER_DOCUMENT_PROPERTY, StructrTraits.PAGE).category("Page Structure");
		final Property<String> parentIdProperty                                    = new EntityIdProperty(PARENT_ID_PROPERTY, StructrTraits.DOM_NODE, PARENT_PROPERTY, StructrTraits.DOM_NODE).category("Page Structure");
		final Property<String> sharedComponentIdProperty                           = new EntityIdProperty(SHARED_COMPONENT_ID_PROPERTY, StructrTraits.DOM_NODE, SHARED_COMPONENT_PROPERTY, StructrTraits.DOM_NODE).format("sharedComponent, {},");
		final Property<String> syncedNodesIdsProperty                              = new CollectionIdProperty(SYNCED_NODES_IDS_PROPERTY, StructrTraits.DOM_NODE, SYNCED_NODES_PROPERTY, StructrTraits.DOM_NODE);
		final Property<String> dataKeyProperty                                     = new StringProperty(DATA_KEY_PROPERTY).indexed().category(DOMNode.QUERY_CATEGORY);
		final Property<String> cypherQueryProperty                                 = new StringProperty(CYPHER_QUERY_PROPERTY).category(DOMNode.QUERY_CATEGORY);
		final Property<String> restQueryProperty                                   = new StringProperty(REST_QUERY_PROPERTY).category(DOMNode.QUERY_CATEGORY);
		final Property<String> functionQueryProperty                               = new StringProperty(FUNCTION_QUERY_PROPERTY).category(DOMNode.QUERY_CATEGORY);
		final Property<String> showForLocalesProperty                              = new StringProperty(SHOW_FOR_LOCALES_PROPERTY).indexed().category(GraphObject.VISIBILITY_CATEGORY);
		final Property<String> hideForLocalesProperty                              = new StringProperty(HIDE_FOR_LOCALES_PROPERTY).indexed().category(GraphObject.VISIBILITY_CATEGORY);
		final Property<String> showConditionsProperty                              = new StringProperty(SHOW_CONDITIONS_PROPERTY).indexed().category(GraphObject.VISIBILITY_CATEGORY).hint("Conditions which have to be met in order for the element to be shown.<br><br>This is an 'auto-script' environment, meaning that the text is automatically surrounded with ${}");
		final Property<String> hideConditionsProperty                              = new StringProperty(HIDE_CONDITIONS_PROPERTY).indexed().category(GraphObject.VISIBILITY_CATEGORY).hint("Conditions which have to be met in order for the element to be hidden.<br><br>This is an 'auto-script' environment, meaning that the text is automatically surrounded with ${}");
		final Property<String> sharedComponentConfigurationProperty                = new StringProperty(SHARED_COMPONENT_CONFIGURATION_PROPERTY).format("multi-line").category(DOMNode.PAGE_CATEGORY).hint("The contents of this field will be evaluated before rendering this component. This is usually used to customize shared components to make them more flexible.<br><br>This is an 'auto-script' environment, meaning that the text is automatically surrounded with ${}");
		final Property<String> dataStructrIdProperty                               = new StringProperty(DATA_STRUCTR_ID_PROPERTY).hint("Set to ${current.id} most of the time").category(DOMNode.PAGE_CATEGORY);
		final Property<String> dataStructrHashProperty                             = new StringProperty(DATA_STRUCTR_HASH_PROPERTY).category(DOMNode.PAGE_CATEGORY);
		final Property<Boolean> dontCacheProperty                                  = new BooleanProperty(DONT_CACHE_PROPERTY).defaultValue(false);
		final Property<Boolean> isDOMNodeProperty                                  = new ConstantBooleanProperty(IS_DOM_NODE_PROPERTY, true).category(DOMNode.PAGE_CATEGORY);
		final Property<Boolean> hasSharedComponent                                 = new BooleanProperty(HAS_SHARED_COMPONENT_PROPERTY).indexed();
		final Property<Integer> domSortPositionProperty                            = new IntProperty(DOM_SORT_POSITION_PROPERTY).category(DOMNode.PAGE_CATEGORY);

		return newSet(
			parentProperty,
			childrenProperty,
			previousSiblingProperty,
			nextSiblingProperty,
			sharedComponentProperty,
			syncedNodesProperty,
			ownerDocumentProperty,
			reloadingActionsProperty,
			failureActionsProperty,
			successNotificationActionsProperty,
			failureNotificationActionsProperty,
			sortedChildrenProperty,
			childrenIdsProperty,
			nextSiblingIdProperty,
			pageIdProperty,
			parentIdProperty,
			sharedComponentIdProperty,
			syncedNodesIdsProperty,
			dataKeyProperty,
			cypherQueryProperty,
			restQueryProperty,
			functionQueryProperty,
			showForLocalesProperty,
			hideForLocalesProperty,
			showConditionsProperty,
			hideConditionsProperty,
			sharedComponentConfigurationProperty,
			dataStructrIdProperty,
			dataStructrHashProperty,
			dontCacheProperty,
			isDOMNodeProperty,
			hasSharedComponent,
			domSortPositionProperty,
			processSuccessShowActions,
			processSuccessHideActions,
			processFailureShowActions,
			processFailureHideActions
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Ui,
			newSet(
					DOMNodeTraitDefinition.RELOADING_ACTIONS_PROPERTY, DOMNodeTraitDefinition.FAILURE_ACTIONS_PROPERTY,
					DOMNodeTraitDefinition.SUCCESS_NOTIFICATION_ACTIONS_PROPERTY, DOMNodeTraitDefinition.FAILURE_NOTIFICATION_ACTIONS_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	// ----- private static methods -----
	private static Iterable<GraphObject> checkListSources(final NodeInterface thisNode, final SecurityContext securityContext, final RenderContext renderContext) {

		// try registered data sources first
		for (final GraphDataSource<Iterable<GraphObject>> source : DataSources.getDataSources()) {

			try {

				final Iterable<GraphObject> graphData = source.getData(renderContext, thisNode);
				if (graphData != null && !Iterables.isEmpty(graphData)) {

					return graphData;
				}

			} catch (FrameworkException fex) {

				LoggerFactory.getLogger(DOMNode.class).warn("Could not retrieve data from graph data source {} in {} {}: {}", source.getClass().getSimpleName(), thisNode.getType(), thisNode.getUuid(), fex.getMessage());
			}
		}

		return Collections.EMPTY_LIST;
	}
}