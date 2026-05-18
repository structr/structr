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
package org.structr.web.traits.definitions.dom;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.structr.api.util.Iterables;
import org.structr.common.ChannelInput;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.event.RuntimeEventLog;
import org.structr.common.helper.CaseHelper;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.InstanceMethod;
import org.structr.core.api.JavaMethod;
import org.structr.core.app.StructrApp;
import org.structr.core.datasources.Channel;
import org.structr.core.datasources.ChannelResult;
import org.structr.core.datasources.DataSources;
import org.structr.core.datasources.GraphDataSource;
import org.structr.core.entity.*;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.*;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.definitions.DataAdapterFieldTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.Evaluate;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.core.traits.operations.nodeinterface.VisitForUsage;
import org.structr.schema.action.ActionContext;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.ComponentConfiguration;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.property.CustomHtmlAttributeProperty;
import org.structr.web.property.DOMNodeSortedChildrenProperty;
import org.structr.web.traits.definitions.html.Option;
import org.structr.web.traits.operations.*;
import org.structr.web.traits.wrappers.dom.DOMNodeTraitWrapper;
import org.w3c.dom.DOMException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Combines NodeInterface and org.w3c.dom.Node.
 */
public class DOMNodeTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String PARENT_PROPERTY                         = "parent";
	public static final String CHILDREN_PROPERTY                       = "children";
	public static final String SHARED_COMPONENT_PROPERTY               = "sharedComponent";
	public static final String SYNCED_NODES_PROPERTY                   = "syncedNodes";
	public static final String OWNER_DOCUMENT_PROPERTY                 = "ownerDocument";
	public static final String RELOADING_ACTIONS_PROPERTY              = "reloadingActions";
	public static final String FAILURE_ACTIONS_PROPERTY                = "failureActions";
	public static final String SUCCESS_NOTIFICATION_ACTIONS_PROPERTY   = "successNotificationActions";
	public static final String FAILURE_NOTIFICATION_ACTIONS_PROPERTY   = "failureNotificationActions";
	public static final String SORTED_CHILDREN_PROPERTY                = "sortedChildren";
	public static final String CHILDREN_IDS_PROPERTY                   = "childrenIds";
	public static final String PAGE_ID_PROPERTY                        = "pageId";
	public static final String PARENT_ID_PROPERTY                      = "parentId";
	public static final String SHARED_COMPONENT_ID_PROPERTY            = "sharedComponentId";
	public static final String SYNCED_NODES_IDS_PROPERTY               = "syncedNodesIds";
	public static final String DATA_KEY_PROPERTY                       = "dataKey";
	public static final String CYPHER_QUERY_PROPERTY                   = "cypherQuery";
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
	public static final String IS_COMPONENT_ROOT_PROPERTY              = "root";
	public static final String COMPONENT_TYPE_PROPERTY                 = "componentType";
	public static final String DIMENSIONS_PROPERTY                     = "dimensions";
	public static final String ITEM_TYPE_PROPERTY                      = "itemType";
	public static final String REPEATER_TYPE_PROPERTY                  = "repeaterType";
	public static final String COMPONENT_CONFIGURATION_PROPERTY        = "componentConfiguration";

	private static final String[] rawProps = new String[] {
		DATA_KEY_PROPERTY, CYPHER_QUERY_PROPERTY, FUNCTION_QUERY_PROPERTY, Option.SELECTEDVALUES_PROPERTY, FLOW_PROPERTY,
		SHOW_FOR_LOCALES_PROPERTY, HIDE_FOR_LOCALES_PROPERTY, SHOW_CONDITIONS_PROPERTY, HIDE_CONDITIONS_PROPERTY
	};

	private static final Set<String> DataAttributeOutputBlacklist = Set.of(DOMElementTraitDefinition.DATA_STRUCTR_MANUAL_RELOAD_TARGET_PROPERTY);

	public DOMNodeTraitDefinition() {
		super(StructrTraits.DOM_NODE);
	}

	@Override
	public Map<Class, LifecycleMethod> createLifecycleMethods(TraitsInstance traitsInstance) {

		return Map.of(

			OnCreation.class,
			new OnCreation() {

				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

					final DOMNode domNode = graphObject.as(DOMNode.class);

					domNode.checkName(errorBuffer);
					domNode.syncName(errorBuffer);
					domNode.updateHasSharedComponentFlag();
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
					domNode.updateHasSharedComponentFlag();

					// acknowledge all events for this node when it is modified
					RuntimeEventLog.acknowledgeAllEventsForId(domNode.getUuid());

					final ComponentConfiguration componentConfiguration = domNode.getComponentConfiguration();
					if (componentConfiguration != null) {

						componentConfiguration.updateFieldSetForChildren();
					}
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

					if (!node.shouldBeRendered(renderContext)) {
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

												if (o instanceof GraphObject graphObject) {

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
					final Traits traits                   = node.getTraits();
					final NodeInterface wrappedNode       = node;

					Set<PropertyKey> dataAttributes = node.getDataPropertyKeys();

					if (RenderContext.EditMode.DEPLOYMENT.equals(editMode)) {

						List sortedAttributes = new LinkedList(dataAttributes);
						Collections.sort(sortedAttributes);
						dataAttributes = new LinkedHashSet<>(sortedAttributes);
					}

					for (final PropertyKey key : dataAttributes) {

						// do not render attributes that are on the blacklist
						if (!RenderContext.EditMode.DEPLOYMENT.equals(editMode) && DataAttributeOutputBlacklist.contains(key.jsonName())) {
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

						if (value != null) {

							if (key instanceof CustomHtmlAttributeProperty chap) {

								out.append(" ").append(chap.cleanName()).append("=\"").append(value).append("\"");

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

					// verify that otherNode is not one of
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

		methods.put(

			AvoidWhitespace.class,
			new AvoidWhitespace() {

				@Override
				public boolean avoidWhitespace() {
					return false;
				}
			}
		);

		methods.put(

			IsVoidElement.class,
			new IsVoidElement() {

				@Override
				public boolean isVoidElement() {
					return false;
				}
			}
		);

		methods.put(

			Evaluate.class,
			new Evaluate() {

				@Override
				public Object evaluate(final AbstractNode node, final ActionContext actionContext, final String key, final String defaultValue, final GraphObject contextObject, final int row, final int column) throws FrameworkException {

					final ComponentConfiguration config = node.as(DOMNode.class).getComponentConfiguration();
					if (config != null) {

						final Channel channel = config.getDataSource();
						if (channel != null) {

							if (actionContext instanceof RenderContext renderContext) {

								final DataAdapter adapter                 = config.getDataAdapter();
								final ChannelResult<GraphObject> iterable = channel.getResult(renderContext, config.getChannelInput(renderContext, adapter));
								final String paginationKey                = channel.getPaginationKey();
								final int resultCount                     = iterable.getResultCount();
								final int pageCount                       = (int) Math.ceil((double)resultCount / (double)config.getPageSize());

								if (paginationKey != null) {

									final int currentPage = DOMElement.intOrOne(actionContext.getSecurityContext().getRequestParameter(paginationKey));

									switch (key) {

										case "pageCount":
											return pageCount;

										case "resultCount":
											return resultCount;

										case "currentPage":
											return currentPage;

										case "nextPage":
											return currentPage + 1;

										case "prevPage":
											return Math.max(1, currentPage - 1);
									}
								}
							}
						}
					}

					return getSuper().evaluate(node, actionContext, key, defaultValue, contextObject, row, column);
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

		return Set.of(

			new InstanceMethod(StructrTraits.DOM_NODE, "cloneNode") {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Map<String, Object> parameters) throws FrameworkException {

					final DOMNode node = entity.as(DOMNode.class);
					final boolean deep = parameters.get("deep") != null && Boolean.parseBoolean(parameters.get("deep").toString());

					final DOMNode clonedNode = node.cloneNode(deep);

					return clonedNode;
				}
			},

			new InstanceMethod(StructrTraits.DOM_NODE, "appendChild") {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Map<String, Object> parameters) throws FrameworkException {

					final NodeInterface newChildNode = (NodeInterface) parameters.get("newChild");
					if (newChildNode != null) {

						final DOMNode newChild = newChildNode.as(DOMNode.class);
						final DOMNode node = entity.as(DOMNode.class);

						node.appendChild(newChild);

						return newChild;

					} else {

						throw new FrameworkException(422, "DOMNode.appendChild(): missing required argument `newChild` of type DOMNode.");
					}
				}
			},

			new InstanceMethod(StructrTraits.DOM_NODE, "setOwnerDocument") {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Map<String, Object> parameters) throws FrameworkException {

					final NodeInterface newChildNode = (NodeInterface) parameters.get("newChild");
					if (newChildNode != null) {

						final DOMNode newChild = newChildNode.as(DOMNode.class);
						final DOMNode node = entity.as(DOMNode.class);

						node.appendChild(newChild);

						return newChild;

					} else {

						throw new FrameworkException(422, "DOMNode.setOwnerDocument(): missing required argument `newChild` of type DOMNode.");
					}
				}
			},

			new InstanceMethod(StructrTraits.DOM_NODE, "getOwnerDocument") {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Map<String, Object> parameters) throws FrameworkException {

					final DOMNode node = entity.as(DOMNode.class);
					return node.getOwnerDocument();
				}
			},

			new InstanceMethod(StructrTraits.DOM_NODE, "isEditable") {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Map<String, Object> parameters) throws FrameworkException {

					return entity.as(DOMNode.class).isEditable();
				}
			},

			new JavaMethod("getDataSourceFields", false, false) {
				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {

					final DOMNode domNode               = entity.as(DOMNode.class);
					final ComponentConfiguration config = domNode.getComponentConfiguration();
					final DataAdapter adapter           = config.getDataAdapter();
					final Channel dataSource            = config.getDataSource();

					if (actionContext instanceof RenderContext renderContext) {

						return adapter.augmentFields(renderContext, dataSource, false);
					}

					// this is called in case
					return adapter.augmentFields(new RenderContext(actionContext.getSecurityContext()), dataSource, false);
				}
			},

			new JavaMethod("updateDataSourceField", false, false) {
				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {

					final SecurityContext securityContext = actionContext.getSecurityContext();
					final DOMNode domNode                 = entity.as(DOMNode.class);
					final ComponentConfiguration config   = domNode.getComponentConfiguration();
					final DataAdapter adapter             = config.getDataAdapter();

					final String fieldName    = (String) arguments.get("_fieldName");
					final String templateName = (String) arguments.get("_templateName");
					final String displayMode  = (String) arguments.get("_displayMode");
					final String destination  = (String) arguments.get("_destination");
					final Boolean reset       = (Boolean) arguments.get("_reset");
					final Boolean activate    = (Boolean) arguments.get("_activate");
					final Boolean delete      = (Boolean) arguments.get("_delete");
					final Boolean update      = (Boolean) arguments.get("_update");

					final Map<String, DataAdapterField> fields = adapter.getFields();
					final String type                          = StructrTraits.DATA_ADAPTER_FIELD;
					final Traits fieldTraits                   = Traits.of(type);

					DataAdapterField field = fields.get(fieldName);
					if (field == null) {

						field = StructrApp.getInstance(securityContext).create(type,
							new NodeAttribute<>(fieldTraits.key(DataAdapterFieldTraitDefinition.DATA_ADAPTER_PROPERTY), adapter),
							new NodeAttribute<>(fieldTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), fieldName)
						).as(DataAdapterField.class);
					}

					if (templateName != null) {

						if ("input".equals(displayMode)) {

							field.setEditTemplate(templateName);

						} else {

							field.setRenderTemplate(templateName);
						}
					}

					if (reset != null && reset.booleanValue()) {

						if ("input".equals(displayMode)) {

							field.setEditTemplate(null);

						} else {

							field.setRenderTemplate(null);
						}
					}

					if (activate != null && activate.booleanValue()) {
						config.setFieldSet(config.getFieldSet() + "," + fieldName);
					}

					if (delete != null && delete.booleanValue()) {
						config.setFieldSet(config.getFieldSet().replace(fieldName, ""));
						StructrApp.getInstance(securityContext).delete(field);
					}

					if (update != null && update.booleanValue()) {

						final Map<String, Object> args = arguments.toMap();
						args.remove("_destination");
						args.remove("_fieldName");
						args.remove("_update");

						if ("field".equals(destination)) {

							final PropertyMap input = PropertyMap.inputTypeToJavaType(securityContext, StructrTraits.DATA_ADAPTER_FIELD, args);
							field.setProperties(securityContext, input);

						} else {

							// set all values on the config object
							Map<String, Object> detailConfig = field.getConfig();
							if (detailConfig == null) {

								detailConfig = new LinkedHashMap<>();
							}

							detailConfig.putAll(args);
							field.setConfig(detailConfig);
						}
					}

					return null;
				}
			},

			// private method, to be called from within a page rendering context only!
			new JavaMethod("pagination", true, false) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {

					final Map<String, Object> attributes  = new LinkedHashMap<>();
					final SecurityContext securityContext = actionContext.getSecurityContext();
					final RenderContext renderContext     = (RenderContext) actionContext;
					final DOMNode component               = entity.as(DOMNode.class);
					final ComponentConfiguration config   = component.getComponentConfiguration();
					final Channel channel                 = config.getDataSource();

					if (channel == null) {
						return " hidden ";
					}

					final DataAdapter adapter             = config.getDataAdapter();
					final ChannelInput input              = config.getChannelInput(renderContext, adapter);
					final String paginationKey            = channel.getPaginationKey();
					final ChannelResult result            = channel.getResult(renderContext, input);
					final int resultCount                 = result.getResultCount();
					final int windowSize                  = config.getPaginationWindowSize();
					final int pageSize                    = config.getPageSize();
					final int pageCount                   = (int) Math.ceil((double)resultCount / (double)pageSize);

					// adjust current page to max. number of pages
					int currentPage = DOMElement.intOrOne(securityContext.getRequestParameter(paginationKey));
					if (currentPage > pageCount && pageCount > 0) {

						currentPage = pageCount;
					}

					final int value                       = DOMNodeTraitDefinition.getPaginationValue(currentPage, pageCount, windowSize, arguments);
					final boolean hidden                  = DOMNodeTraitDefinition.isPaginationControlHidden(currentPage, pageCount, resultCount, arguments);

					if (hidden) {

						attributes.put("hidden", true);

					} else {

						if (value > 0 && (resultCount == -1 || value <= pageCount)) {

							attributes.put("data-structr-success-target", "[data-channel~='" + channel.getChannelName() + "']");
							attributes.put("data-structr-events", "click");
							attributes.put("data-structr-target", paginationKey);
							attributes.put("data-" + paginationKey, value);

							if (value == currentPage) {
								attributes.put("data-current-page", currentPage);
							}

						} else {

							attributes.put("disabled", "true");
						}
					}

					// join into a single string and return it
					return attributes.entrySet().stream().map(e -> e.getKey() + "=\"" + e.getValue() + "\"").collect(Collectors.joining(" "));
				}
			},

			// private method, to be called from within a page rendering context only!
			new JavaMethod("page", true, false) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {

					final DOMNode component               = entity.as(DOMNode.class);
					final SecurityContext securityContext = actionContext.getSecurityContext();
					final RenderContext renderContext     = (RenderContext) actionContext;
					final ComponentConfiguration config   = component.getComponentConfiguration();
					final Channel channel                 = config.getDataSource();

					if (channel == null) {
						return "";
					}

					final DataAdapter adapter             = config.getDataAdapter();
					final ChannelInput input              = config.getChannelInput(renderContext, adapter);
					final String paginationKey            = channel.getPaginationKey();
					final ChannelResult result            = channel.getResult(renderContext, input);
					final int resultCount                 = result.getResultCount();
					final int windowSize                  = config.getPaginationWindowSize();
					final int pageSize                    = config.getPageSize();
					final int pageCount                   = (int) Math.ceil((double)resultCount / (double)pageSize);

					// adjust current page to max. number of pages
					int currentPage = DOMElement.intOrOne(securityContext.getRequestParameter(paginationKey));
					if (currentPage > pageCount && pageCount > 0) {

						currentPage = pageCount;
					}

					return DOMNodeTraitDefinition.getPaginationValue(currentPage, pageCount, windowSize, arguments);
				}
			},

			// private method, to be called from within a page rendering context only!
			new JavaMethod("filterControls", true, false) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {

					final Map<String, Object> attributes  = new LinkedHashMap<>();
					final RenderContext renderContext     = (RenderContext) actionContext;
					final DOMNode component               = entity.as(DOMNode.class);
					final ComponentConfiguration config   = component.getComponentConfiguration();
					final Channel channel                 = config.getDataSource();

					if (channel == null) {
						return " hidden ";
					}

					final String paginationKey            = channel.getPaginationKey();
					final String filterKey                = channel.getFilterKey();
					final String filterString             = renderContext.getRequestParameter(filterKey);

					attributes.put("data-structr-success-target", "[data-channel~='" + channel.getChannelName() + "']");
					attributes.put("data-structr-events", "keyup");
					attributes.put("data-structr-target", filterKey);
					attributes.put("data-structr-options", "{ &quot;delay&quot;: 500, &quot;resetWithEsc&quot;: true, &quot;resetPagination&quot;: &quot;" + paginationKey + "&quot; }");
					attributes.put("data-" + paginationKey, 1);
					attributes.put("name", filterKey);

					if (StringUtils.isNotBlank(filterString)) {
						attributes.put("value", filterString);
					}

					// join into a single string and return it
					return attributes.entrySet().stream().map(e -> e.getKey() + "=\"" + e.getValue() + "\"").collect(Collectors.joining(" "));
				}
			}
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<NodeInterface> parentProperty                               = new StartNode(traitsInstance, PARENT_PROPERTY, StructrTraits.DOM_NODE_CONTAINS_DOM_NODE).category(DOMNode.PAGE_CATEGORY);
		final Property<Iterable<NodeInterface>> childrenProperty                   = new EndNodes(traitsInstance, CHILDREN_PROPERTY, StructrTraits.DOM_NODE_CONTAINS_DOM_NODE).category(DOMNode.PAGE_CATEGORY);
		final Property<NodeInterface> sharedComponentProperty                      = new StartNode(traitsInstance, SHARED_COMPONENT_PROPERTY, StructrTraits.DOM_NODE_SYNC_DOM_NODE).category(DOMNode.PAGE_CATEGORY);
		final Property<Iterable<NodeInterface>> syncedNodesProperty                = new EndNodes(traitsInstance, SYNCED_NODES_PROPERTY, StructrTraits.DOM_NODE_SYNC_DOM_NODE).category(DOMNode.PAGE_CATEGORY);
		final Property<NodeInterface> ownerDocumentProperty                        = new EndNode(traitsInstance, OWNER_DOCUMENT_PROPERTY, StructrTraits.DOM_NODE_PAGE_PAGE).category(DOMNode.PAGE_CATEGORY);
		final Property<Iterable<NodeInterface>> reloadingActionsProperty           = new EndNodes(traitsInstance, RELOADING_ACTIONS_PROPERTY, StructrTraits.DOM_NODE_SUCCESS_TARGET_ACTION_MAPPING);
		final Property<Iterable<NodeInterface>> failureActionsProperty             = new EndNodes(traitsInstance, FAILURE_ACTIONS_PROPERTY, StructrTraits.DOM_NODE_FAILURE_TARGET_ACTION_MAPPING);
		final Property<Iterable<NodeInterface>> successNotificationActionsProperty = new EndNodes(traitsInstance, SUCCESS_NOTIFICATION_ACTIONS_PROPERTY, StructrTraits.DOM_NODE_SUCCESS_NOTIFICATION_ELEMENT_ACTION_MAPPING);
		final Property<Iterable<NodeInterface>> failureNotificationActionsProperty = new EndNodes(traitsInstance, FAILURE_NOTIFICATION_ACTIONS_PROPERTY, StructrTraits.DOM_NODE_FAILURE_NOTIFICATION_ELEMENT_ACTION_MAPPING);
		final Property<NodeInterface> componentConfigurationProperty               = new EndNode(traitsInstance, COMPONENT_CONFIGURATION_PROPERTY, StructrTraits.DOM_NODE_HAS_COMPONENT_CONFIGURATION).category(DOMNode.WIDGETS_CATEGORY);
		final Property<Iterable<DOMNode>> sortedChildrenProperty                   = new DOMNodeSortedChildrenProperty(SORTED_CHILDREN_PROPERTY).typeHint("DOMNode[]");
		final Property<String> childrenIdsProperty                                 = new CollectionIdProperty(CHILDREN_IDS_PROPERTY, StructrTraits.DOM_NODE, DOMNodeTraitDefinition.CHILDREN_PROPERTY, StructrTraits.DOM_NODE).category("Page Structure");
		final Property<String> pageIdProperty                                      = new EntityIdProperty(PAGE_ID_PROPERTY, StructrTraits.DOM_NODE, OWNER_DOCUMENT_PROPERTY, StructrTraits.PAGE).category("Page Structure");
		final Property<String> parentIdProperty                                    = new EntityIdProperty(PARENT_ID_PROPERTY, StructrTraits.DOM_NODE, PARENT_PROPERTY, StructrTraits.DOM_NODE).category("Page Structure");
		final Property<String> sharedComponentIdProperty                           = new EntityIdProperty(SHARED_COMPONENT_ID_PROPERTY, StructrTraits.DOM_NODE, SHARED_COMPONENT_PROPERTY, StructrTraits.DOM_NODE).format("sharedComponent, {},");
		final Property<String> syncedNodesIdsProperty                              = new CollectionIdProperty(SYNCED_NODES_IDS_PROPERTY, StructrTraits.DOM_NODE, SYNCED_NODES_PROPERTY, StructrTraits.DOM_NODE);
		final Property<String> dataKeyProperty                                     = new StringProperty(DATA_KEY_PROPERTY).indexed().category(DOMNode.QUERY_CATEGORY);
		final Property<String> cypherQueryProperty                                 = new StringProperty(CYPHER_QUERY_PROPERTY).category(DOMNode.QUERY_CATEGORY);
		final Property<String> functionQueryProperty                               = new StringProperty(FUNCTION_QUERY_PROPERTY).category(DOMNode.QUERY_CATEGORY);
		final Property<String> showForLocalesProperty                              = new StringProperty(SHOW_FOR_LOCALES_PROPERTY).indexed().category(GraphObject.VISIBILITY_CATEGORY);
		final Property<String> hideForLocalesProperty                              = new StringProperty(HIDE_FOR_LOCALES_PROPERTY).indexed().category(GraphObject.VISIBILITY_CATEGORY);
		final Property<String> showConditionsProperty                              = new StringProperty(SHOW_CONDITIONS_PROPERTY).indexed().category(GraphObject.VISIBILITY_CATEGORY).description("Conditions which have to be met in order for the element to be shown. This is an 'auto-script' environment, meaning that the text is automatically surrounded with ${}.");
		final Property<String> hideConditionsProperty                              = new StringProperty(HIDE_CONDITIONS_PROPERTY).indexed().category(GraphObject.VISIBILITY_CATEGORY).description("Conditions which have to be met in order for the element to be hidden. This is an 'auto-script' environment, meaning that the text is automatically surrounded with ${}.");
		final Property<String> sharedComponentConfigurationProperty                = new StringProperty(SHARED_COMPONENT_CONFIGURATION_PROPERTY).format("multi-line").category(DOMNode.PAGE_CATEGORY).description("The contents of this field will be evaluated before rendering this component. This is usually used to customize shared components to make them more flexible. This is an 'auto-script' environment, meaning that the text is automatically surrounded with ${}.");
		final Property<String> dataStructrIdProperty                               = new StringProperty(DATA_STRUCTR_ID_PROPERTY).category(DOMNode.PAGE_CATEGORY).description("Set to ${current.id} most of the time.");
		final Property<String> dataStructrHashProperty                             = new StringProperty(DATA_STRUCTR_HASH_PROPERTY).category(DOMNode.PAGE_CATEGORY);
		final Property<Boolean> dontCacheProperty                                  = new BooleanProperty(DONT_CACHE_PROPERTY).defaultValue(false);
		final Property<Boolean> isDOMNodeProperty                                  = new ConstantBooleanProperty(IS_DOM_NODE_PROPERTY, true).category(DOMNode.PAGE_CATEGORY);
		final Property<Boolean> hasSharedComponent                                 = new BooleanProperty(HAS_SHARED_COMPONENT_PROPERTY).indexed();
		final Property<Integer> domSortPositionProperty                            = new IntProperty(DOM_SORT_POSITION_PROPERTY).category(DOMNode.PAGE_CATEGORY);
		final Property<Boolean> isComponentRootProperty                            = new BooleanProperty(IS_COMPONENT_ROOT_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<String> componentTypeProperty                               = new StringProperty(COMPONENT_TYPE_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<Integer> dimensionsProperty                                 = new IntProperty(DIMENSIONS_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<String> itemTypeProperty                                    = new StringProperty(ITEM_TYPE_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<String> repeaterTypeProperty                                = new StringProperty(REPEATER_TYPE_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);


		return newSet(
			parentProperty,
			childrenProperty,
			sharedComponentProperty,
			syncedNodesProperty,
			ownerDocumentProperty,
			reloadingActionsProperty,
			failureActionsProperty,
			successNotificationActionsProperty,
			failureNotificationActionsProperty,
			sortedChildrenProperty,
			childrenIdsProperty,
			pageIdProperty,
			parentIdProperty,
			sharedComponentIdProperty,
			syncedNodesIdsProperty,
			dataKeyProperty,
			cypherQueryProperty,
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
			isComponentRootProperty,
			componentTypeProperty,
			dimensionsProperty,
			itemTypeProperty,
			repeaterTypeProperty,
			componentConfigurationProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Ui,
			newSet(
				RELOADING_ACTIONS_PROPERTY, FAILURE_ACTIONS_PROPERTY, SUCCESS_NOTIFICATION_ACTIONS_PROPERTY,
				FAILURE_NOTIFICATION_ACTIONS_PROPERTY, COMPONENT_CONFIGURATION_PROPERTY, IS_COMPONENT_ROOT_PROPERTY,
				COMPONENT_TYPE_PROPERTY, DIMENSIONS_PROPERTY, ITEM_TYPE_PROPERTY, REPEATER_TYPE_PROPERTY
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

	private static int getPaginationValue(final int currentPage, final int pageCount, final int windowSize, final Arguments arguments) {

		if (arguments.get("prev") != null) {

			return currentPage - 1;
		}

		if (arguments.get("next") != null) {

			return currentPage + 1;
		}

		if (arguments.get("first") != null) {
			return 1;
		}

		if (arguments.get("last") != null) {
			return pageCount;
		}

		final Object windowInput = arguments.get("window");
		if (windowInput != null) {

			int window = DOMElement.intOrZero(windowInput);

			// We return all the values here, buttons with invalid
			// values are hidden by the method below.
			return currentPage + window;
		}

		return 0;
	}

	private static boolean isPaginationControlHidden(final int currentPage, final int pageCount, final int resultCount, final Arguments arguments) {

		// hide "prev" button if there is no previous page
		if (arguments.get("first") != null && (currentPage < 4 || resultCount == -1)) {
			return true;
		}

		// hide "low" ellipsis button
		if ("low".equals(arguments.get("ellipsis")) && (currentPage < 5 || resultCount == -1)) {
			return true;
		}

		// hide "high" ellipsis button
		if ("high".equals(arguments.get("ellipsis")) && (currentPage >= (pageCount - 3) || resultCount == -1)) {
			return true;
		}

		// hide "last" button
		if (arguments.get("last") != null && (currentPage >= (pageCount - 2) || resultCount == -1)) {

			return true;
		}

		// fewer pages than window size? Show only buttons in the middle
		final Object windowInput = arguments.get("window");
		if (windowInput != null) {

			// do not show "window" buttons if result count is above soft limit
			if (resultCount == -1) {
				return true;
			}

			final int window  = DOMElement.intOrZero(windowInput);
			final int newPage = currentPage + window;

			// we're returning "hidden", not "visible" here
			return newPage < 1 || newPage > pageCount;
		}

		return false;
	}
}