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
package org.structr.web.traits.definitions;


import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.event.RuntimeEventLog;
import org.structr.common.helper.CaseHelper;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.datasources.DataSources;
import org.structr.core.datasources.GraphDataSource;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Relation;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.AbstractTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.core.traits.operations.nodeinterface.VisitForUsage;
import org.structr.core.traits.operations.propertycontainer.GetPropertyKeys;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.property.CustomHtmlAttributeProperty;
import org.structr.web.property.MethodProperty;
import org.structr.web.traits.operations.*;
import org.structr.web.traits.wrappers.DOMNodeTraitWrapper;
import org.w3c.dom.DOMException;

import java.util.*;

/**
 * Combines AbstractNode and org.w3c.dom.Node.
 */
public class DOMNodeTraitDefinition extends AbstractTraitDefinition {

	private static final String[] rawProps = new String[] {
		"dataKey", "restQuery", "cypherQuery", "xpathQuery", "functionQuery", "selectedValues", "flow", "hideOnIndex", "hideOnDetail", "showForLocales", "hideForLocales", "showConditions", "hideConditions"
	};

	private static final Set<String> DataAttributeOutputBlacklist = Set.of("data-structr-manual-reload-target");

	private static final Property<NodeInterface> parentProperty                               = new StartNode("parent", "DOMNodeCONTAINSDOMNode").category(DOMNode.PAGE_CATEGORY).partOfBuiltInSchema();
	private static final Property<Iterable<NodeInterface>> childrenProperty                   = new EndNodes("children", "DOMNodeCONTAINSDOMNode").category(DOMNode.PAGE_CATEGORY).partOfBuiltInSchema();
	private static final Property<NodeInterface> previousSiblingProperty                      = new StartNode("previousSibling", "DOMNodeCONTAINS_NEXT_SIBLINGDOMNode").category(DOMNode.PAGE_CATEGORY).partOfBuiltInSchema();
	private static final Property<NodeInterface> nextSiblingProperty                          = new EndNode("nextSibling", "DOMNodeCONTAINS_NEXT_SIBLINGDOMNode").category(DOMNode.PAGE_CATEGORY).partOfBuiltInSchema();
	private static final Property<NodeInterface> sharedComponentProperty                      = new StartNode("sharedComponent", "DOMNodeSYNCDOMNode").category(DOMNode.PAGE_CATEGORY).partOfBuiltInSchema();
	private static final Property<Iterable<NodeInterface>> syncedNodesProperty                = new EndNodes("syncedNodes", "DOMNodeSYNCDOMNode").category(DOMNode.PAGE_CATEGORY).partOfBuiltInSchema();
	private static final Property<NodeInterface> ownerDocumentProperty                        = new EndNode("ownerDocument", "DOMNodePAGEPage").category(DOMNode.PAGE_CATEGORY).partOfBuiltInSchema();
	private static final Property<Iterable<NodeInterface>> reloadingActionsProperty           = new EndNodes("reloadingActions", "DOMNodeSUCCESS_TARGETActionMapping").partOfBuiltInSchema();
	private static final Property<Iterable<NodeInterface>> failureActionsProperty             = new EndNodes("failureActions", "DOMNodeFAILURE_TARGETActionMapping").partOfBuiltInSchema();
	private static final Property<Iterable<NodeInterface>> successNotificationActionsProperty = new EndNodes("successNotificationActions", "DOMNodeSUCCESS_NOTIFICATION_ELEMENTActionMapping").partOfBuiltInSchema();
	private static final Property<Iterable<NodeInterface>> failureNotificationActionsProperty = new EndNodes("failureNotificationActions", "DOMNodeFAILURE_NOTIFICATION_ELEMENTActionMapping").partOfBuiltInSchema();
	private static final Property<Object> sortedChildrenProperty                              = new MethodProperty("sortedChildren").format("org.structr.web.entity.dom.DOMNode, getChildNodes").typeHint("DOMNode[]").partOfBuiltInSchema();
	private static final Property<String> childrenIdsProperty                                 = new CollectionIdProperty("childrenIds", childrenProperty).format("children, {},").partOfBuiltInSchema().category("Page Structure").partOfBuiltInSchema();
	private static final Property<String> nextSiblingIdProperty                               = new EntityIdProperty("nextSiblingId", nextSiblingProperty).format("nextSibling, {},").partOfBuiltInSchema().category("Page Structure").partOfBuiltInSchema();
	private static final Property<String> pageIdProperty                                      = new EntityIdProperty("pageId", ownerDocumentProperty).format("ownerDocument, {},").partOfBuiltInSchema().category("Page Structure").partOfBuiltInSchema();
	private static final Property<String> parentIdProperty                                    = new EntityIdProperty("parentId", parentProperty).format("parent, {},").partOfBuiltInSchema().category("Page Structure").partOfBuiltInSchema();
	private static final Property<String> sharedComponentIdProperty                           = new EntityIdProperty("sharedComponentId", sharedComponentProperty).format("sharedComponent, {},").partOfBuiltInSchema();
	private static final Property<String> syncedNodesIdsProperty                              = new CollectionIdProperty("syncedNodesIds", syncedNodesProperty).format("syncedNodes, {},").partOfBuiltInSchema();
	private static final Property<String> dataKeyProperty                                     = new StringProperty("dataKey").indexed().category(DOMNode.QUERY_CATEGORY).partOfBuiltInSchema();
	private static final Property<String> cypherQueryProperty                                 = new StringProperty("cypherQuery").category(DOMNode.QUERY_CATEGORY).partOfBuiltInSchema();
	private static final Property<String> restQueryProperty                                   = new StringProperty("restQuery").category(DOMNode.QUERY_CATEGORY).partOfBuiltInSchema();
	private static final Property<String> functionQueryProperty                               = new StringProperty("functionQuery").category(DOMNode.QUERY_CATEGORY).partOfBuiltInSchema();
	private static final Property<String> showForLocalesProperty                              = new StringProperty("showForLocales").indexed().category(GraphObject.VISIBILITY_CATEGORY).partOfBuiltInSchema();
	private static final Property<String> hideForLocalesProperty                              = new StringProperty("hideForLocales").indexed().category(GraphObject.VISIBILITY_CATEGORY).partOfBuiltInSchema();
	private static final Property<String> showConditionsProperty                              = new StringProperty("showConditions").indexed().category(GraphObject.VISIBILITY_CATEGORY).hint("Conditions which have to be met in order for the element to be shown.<br><br>This is an 'auto-script' environment, meaning that the text is automatically surrounded with ${}").partOfBuiltInSchema();
	private static final Property<String> hideConditionsProperty                              = new StringProperty("hideConditions").indexed().category(GraphObject.VISIBILITY_CATEGORY).hint("Conditions which have to be met in order for the element to be hidden.<br><br>This is an 'auto-script' environment, meaning that the text is automatically surrounded with ${}").partOfBuiltInSchema();
	private static final Property<String> sharedComponentConfigurationProperty                = new StringProperty("sharedComponentConfiguration").format("multi-line").category(DOMNode.PAGE_CATEGORY).hint("The contents of this field will be evaluated before rendering this component. This is usually used to customize shared components to make them more flexible.<br><br>This is an 'auto-script' environment, meaning that the text is automatically surrounded with ${}").partOfBuiltInSchema();
	private static final Property<String> dataStructrIdProperty                               = new StringProperty("data-structr-id").hint("Set to ${current.id} most of the time").category(DOMNode.PAGE_CATEGORY).partOfBuiltInSchema();
	private static final Property<String> dataStructrHashProperty                             = new StringProperty("data-structr-hash").category(DOMNode.PAGE_CATEGORY).partOfBuiltInSchema();
	private static final Property<Boolean> dontCacheProperty                                  = new BooleanProperty("dontCache").defaultValue(false).partOfBuiltInSchema();
	private static final Property<Boolean> isDOMNodeProperty                                  = new ConstantBooleanProperty("isDOMNode", true).category(DOMNode.PAGE_CATEGORY).partOfBuiltInSchema();
	private static final Property<Integer> domSortPositionProperty                            = new IntProperty("domSortPosition").category(DOMNode.PAGE_CATEGORY).partOfBuiltInSchema();
	private static Property<NodeInterface> flow                                               = new EndNode("flow", "DOMNodeFLOWFlowContainer");

	/*
	public static final View uiView = new View(DOMNode.class, PropertyView.Ui,
		reloadingActionsProperty, failureActionsProperty, successNotificationActionsProperty, failureNotificationActionsProperty
	);
	*/

	public DOMNodeTraitDefinition() {
		super("DOMNode");
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(

			OnCreation.class,
			new OnCreation() {

				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

					final DOMNode domNode = ((NodeInterface) graphObject).as(DOMNode.class);

					domNode.checkName(errorBuffer);
					domNode.syncName(errorBuffer);
				}
			},

			OnModification.class,
			new OnModification() {

				@Override
				public void onModification(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

					final DOMNode domNode = ((NodeInterface) graphObject).as(DOMNode.class);

					domNode.increasePageVersion();
					domNode.checkName(errorBuffer);
					domNode.syncName(errorBuffer);

					final String uuid = domNode.getUuid();
					if (uuid != null) {

						// acknowledge all events for this node when it is modified
						RuntimeEventLog.getEvents(e -> uuid.equals(e.getData().get("id"))).stream().forEach(e -> e.acknowledge());
					}
				}
			}
		);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			VisitForUsage.class,
			new VisitForUsage() {

				@Override
				public void visitForUsage(final NodeInterface obj, final Map<String, Object> data) {

					getSuper().visitForUsage(obj, data);

					final DOMNode node = obj.as(DOMNode.class);
					final Page page = (Page) node.getOwnerDocument();

					if (page != null) {

						data.put("page", page.getName());
					}

					data.put("path", node.getPagePath());
				}
			},

			Render.class,
			new Render() {

				@Override
				public void render(final NodeInterface node, final RenderContext renderContext, final int depth) throws FrameworkException {

					final DOMNode domNode = node.as(DOMNode.class);
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

					final GraphObject details = renderContext.getDetailsDataObject();
					final boolean detailMode = details != null;

					if (detailMode && domNode.hideOnDetail()) {
						return;
					}

					if (!detailMode && domNode.hideOnIndex()) {
						return;
					}

					if (isAdminOnlyEditMode) {

						domNode.renderContent(renderContext, depth);

					} else {

						final String subKey = domNode.getDataKey();

						if (StringUtils.isNotBlank(subKey)) {

							// fetch (optional) list of external data elements
							final Iterable<GraphObject> listData = DOMNodeTraitDefinition.checkListSources(node, securityContext, renderContext);

							final PropertyKey propertyKey;

							// Make sure the closest 'page' keyword is always set also for partials
							if (depth == 0 && isPartial) {

								renderContext.setPage(domNode.getClosestPage());

							}

							final GraphObject dataObject = renderContext.getDataNode(subKey); // renderContext.getSourceDataObject();

							// Render partial with possible top-level repeater limited to a single data object
							if (depth == 0 && isPartial && dataObject != null) {

								renderContext.putDataObject(subKey, dataObject);
								domNode.renderContent(renderContext, depth);

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
													domNode.renderContent(renderContext, depth);

												}
											}

										}

									} else {

										final Traits traits = currentDataNode.getTraits();
										propertyKey = traits.key(subKey);
										renderContext.setRelatedProperty(propertyKey);

										if (propertyKey != null) {

											final Object value = currentDataNode.getProperty(propertyKey);
											if (value != null) {

												if (value instanceof Iterable) {

													for (final Object o : ((Iterable) value)) {

														if (o instanceof GraphObject) {

															renderContext.putDataObject(subKey, (GraphObject) o);
															domNode.renderContent(renderContext, depth);

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
									domNode.renderNodeList(securityContext, renderContext, depth, subKey);

								}
							}

						} else {

							domNode.renderContent(renderContext, depth);
						}
					}
				}
			},

			DoAdopt.class,
			new DoAdopt() {
				@Override
				public void doAdopt(final NodeInterface node, final Page _page) throws DOMException {

					if (_page != null) {

						try {

							final DOMNode domNode = node.as(DOMNode.class);

							domNode.setOwnerDocument(_page);

						} catch (FrameworkException fex) {

							throw new DOMException(DOMException.INVALID_STATE_ERR, fex.getMessage());

						}
					}
				}
			},

			GetCssClass.class,
			new GetCssClass() {

				@Override
				public String getCssClass(final NodeInterface node) {
					return null;
				}
			},

			GetPagePath.class,
			new GetPagePath() {

				@Override
				public String getPagePath(NodeInterface node) {

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
			},

			RenderCustomAttributes.class,
			new RenderCustomAttributes() {

				@Override
				public void renderCustomAttributes(final NodeInterface node, final AsyncBuffer out, final SecurityContext securityContext, final RenderContext renderContext) throws FrameworkException {

					final RenderContext.EditMode editMode = renderContext.getEditMode(securityContext.getUser(false));
					final DOMNode thisNode                = node.as(DOMNode.class);
					final Traits traits                   = node.getTraits();

					Set<PropertyKey> dataAttributes = thisNode.getDataPropertyKeys();

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

							final Object obj = node.getProperty(key);
							if (obj != null) {

								value = obj.toString();
							}

						} else {

							value = node.getPropertyWithVariableReplacement(renderContext, key);
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
							final String name = node.getProperty(traits.key("name"));
							if (name != null) {

								out.append(" data-structr-meta-name=\"").append(DOMNode.escapeForHtmlAttributes(name)).append("\"");
							}

							out.append(" data-structr-meta-id=\"").append(thisNode.getUuid()).append("\"");
						}

						for (final String p : rawProps) {

							final String htmlName = "data-structr-meta-" + CaseHelper.toUnderscore(p, false).replaceAll("_", "-");
							final PropertyKey key = traits.key(p);

							if (key != null) {

								final Object value = node.getProperty(key);
								if (value != null) {

									final boolean isBoolean = key instanceof BooleanProperty;
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
	public Set<PropertyKey> getPropertyKeys() {

		return Set.of(
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
			domSortPositionProperty,
			flow
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