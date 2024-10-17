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


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.service.LicenseManager;
import org.structr.api.util.Iterables;
import org.structr.common.*;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.SemanticErrorToken;
import org.structr.common.error.UnlicensedScriptException;
import org.structr.common.event.RuntimeEventLog;
import org.structr.common.helper.CaseHelper;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.datasources.DataSources;
import org.structr.core.datasources.GraphDataSource;
import org.structr.core.entity.*;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.*;
import org.structr.core.script.Scripting;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.RenderContext;
import org.structr.web.common.RenderContext.EditMode;
import org.structr.web.common.StringRenderBuffer;
import org.structr.web.entity.LinkSource;
import org.structr.web.entity.Linkable;
import org.structr.web.entity.Renderable;
import org.structr.web.entity.dom.relationship.*;
import org.structr.web.entity.event.ActionMapping;
import org.structr.web.property.CustomHtmlAttributeProperty;
import org.structr.web.property.MethodProperty;
import org.structr.websocket.command.CreateComponentCommand;
import org.w3c.dom.*;

import java.util.*;

/**
 * Combines AbstractNode and org.w3c.dom.Node.
 */
public abstract class DOMNode extends AbstractNode implements LinkedTreeNode<DOMNode, DOMNode>, Node, Renderable, DOMAdoptable, DOMImportable, ContextAwareEntity {

	private static final Set<String> DataAttributeOutputBlacklist = Set.of("data-structr-manual-reload-target");

	public static final String PAGE_CATEGORY                 = "Page Structure";
	public static final String EDIT_MODE_BINDING_CATEGORY    = "Edit Mode Binding";
	public static final String EVENT_ACTION_MAPPING_CATEGORY = "Event Action Mapping";
	public static final String QUERY_CATEGORY                = "Query and Data Binding";

	// ----- error messages for DOMExceptions -----
	public static final String NO_MODIFICATION_ALLOWED_MESSAGE         = "Permission denied.";
	public static final String INVALID_ACCESS_ERR_MESSAGE              = "Permission denied.";
	public static final String INDEX_SIZE_ERR_MESSAGE                  = "Index out of range.";
	public static final String CANNOT_SPLIT_TEXT_WITHOUT_PARENT        = "Cannot split text element without parent and/or owner document.";
	public static final String WRONG_DOCUMENT_ERR_MESSAGE              = "Node does not belong to this document.";
	public static final String HIERARCHY_REQUEST_ERR_MESSAGE_SAME_NODE = "A node cannot accept itself as a child.";
	public static final String HIERARCHY_REQUEST_ERR_MESSAGE_ANCESTOR  = "A node cannot accept its own ancestor as child.";
	public static final String HIERARCHY_REQUEST_ERR_MESSAGE_DOCUMENT  = "A document may only have one html element.";
	public static final String HIERARCHY_REQUEST_ERR_MESSAGE_ELEMENT   = "A document may only accept an html element as its document element.";
	public static final String NOT_SUPPORTED_ERR_MESSAGE               = "Node type not supported.";
	public static final String NOT_FOUND_ERR_MESSAGE                   = "Node is not a child.";
	public static final String NOT_SUPPORTED_ERR_MESSAGE_IMPORT_DOC    = "Document nodes cannot be imported into another document.";
	public static final String NOT_SUPPORTED_ERR_MESSAGE_ADOPT_DOC     = "Document nodes cannot be adopted by another document.";
	public static final String NOT_SUPPORTED_ERR_MESSAGE_RENAME        = "Renaming of nodes is not supported by this implementation.";

	public static final Set<String> cloneBlacklist = new LinkedHashSet<>(Arrays.asList(new String[] {
		"id", "type", "ownerDocument", "pageId", "parent", "parentId", "syncedNodes", "syncedNodesIds", "children", "childrenIds", "linkable", "linkableId", "path", "relationshipId", "triggeredActions", "reloadingActions", "failureActions", "successNotificationActions", "failureNotificationActions"
	}));

	public static final String[] rawProps = new String[] {
		"dataKey", "restQuery", "cypherQuery", "functionQuery", "selectedValues", "flow", "showForLocales", "hideForLocales", "showConditions", "hideConditions"
	};

	public static final Property<DOMNode> parentProperty             = new StartNode<>("parent", DOMNodeCONTAINSDOMNode.class).category(PAGE_CATEGORY).partOfBuiltInSchema();
	public static final Property<Iterable<DOMNode>> childrenProperty = new EndNodes<>("children", DOMNodeCONTAINSDOMNode.class).category(PAGE_CATEGORY).partOfBuiltInSchema();

	public static final Property<DOMNode> previousSiblingProperty = new StartNode<>("previousSibling", DOMNodeCONTAINS_NEXT_SIBLINGDOMNode.class).category(PAGE_CATEGORY).partOfBuiltInSchema();
	public static final Property<DOMNode> nextSiblingProperty     = new EndNode<>("nextSibling", DOMNodeCONTAINS_NEXT_SIBLINGDOMNode.class).category(PAGE_CATEGORY).partOfBuiltInSchema();

	public static final Property<DOMNode> sharedComponentProperty       = new StartNode<>("sharedComponent", DOMNodeSYNCDOMNode.class).category(PAGE_CATEGORY).partOfBuiltInSchema();
	public static final Property<Iterable<DOMNode>> syncedNodesProperty = new EndNodes<>("syncedNodes", DOMNodeSYNCDOMNode.class).category(PAGE_CATEGORY).partOfBuiltInSchema();

	public static final Property<Page> ownerDocumentProperty = new EndNode<>("ownerDocument", DOMNodePAGEPage.class).category(PAGE_CATEGORY).partOfBuiltInSchema();

	public static final Property<Iterable<ActionMapping>> reloadingActionsProperty           = new EndNodes<>("reloadingActions", DOMNodeSUCCESS_TARGETActionMapping.class).partOfBuiltInSchema();
	public static final Property<Iterable<ActionMapping>> failureActionsProperty             = new EndNodes<>("failureActions", DOMNodeFAILURE_TARGETActionMapping.class).partOfBuiltInSchema();
	public static final Property<Iterable<ActionMapping>> successNotificationActionsProperty = new EndNodes<>("successNotificationActions", DOMNodeSUCCESS_NOTIFICATION_ELEMENTActionMapping.class).partOfBuiltInSchema();
	public static final Property<Iterable<ActionMapping>> failureNotificationActionsProperty = new EndNodes<>("failureNotificationActions", DOMNodeFAILURE_NOTIFICATION_ELEMENTActionMapping.class).partOfBuiltInSchema();

	public static final Property<java.lang.Object> sortedChildrenProperty = new MethodProperty("sortedChildren").format("org.structr.web.entity.dom.DOMNode, getChildNodes").typeHint("DOMNode[]").partOfBuiltInSchema();
	public static final Property<String> childrenIdsProperty              = new CollectionIdProperty("childrenIds", childrenProperty).format("children, {},").partOfBuiltInSchema().category("Page Structure").partOfBuiltInSchema();
	public static final Property<String> nextSiblingIdProperty            = new EntityIdProperty("nextSiblingId", nextSiblingProperty).format("nextSibling, {},").partOfBuiltInSchema().category("Page Structure").partOfBuiltInSchema();
	public static final Property<String> pageIdProperty                   = new EntityIdProperty("pageId", ownerDocumentProperty).format("ownerDocument, {},").partOfBuiltInSchema().category("Page Structure").partOfBuiltInSchema();
	public static final Property<String> parentIdProperty                 = new EntityIdProperty("parentId", parentProperty).format("parent, {},").partOfBuiltInSchema().category("Page Structure").partOfBuiltInSchema();
	public static final Property<String> sharedComponentIdProperty        = new EntityIdProperty("sharedComponentId", sharedComponentProperty).format("sharedComponent, {},").partOfBuiltInSchema();
	public static final Property<String> syncedNodesIdsProperty           = new CollectionIdProperty("syncedNodesIds", syncedNodesProperty).format("syncedNodes, {},").partOfBuiltInSchema();

	public static final Property<String> dataKeyProperty       = new StringProperty("dataKey").indexed().category(QUERY_CATEGORY).partOfBuiltInSchema();
	public static final Property<String> cypherQueryProperty   = new StringProperty("cypherQuery").category(QUERY_CATEGORY).partOfBuiltInSchema();
	public static final Property<String> restQueryProperty     = new StringProperty("restQuery").category(QUERY_CATEGORY).partOfBuiltInSchema();
	public static final Property<String> functionQueryProperty = new StringProperty("functionQuery").category(QUERY_CATEGORY).partOfBuiltInSchema();

	public static final Property<String> showForLocalesProperty = new StringProperty("showForLocales").indexed().category(VISIBILITY_CATEGORY).partOfBuiltInSchema();
	public static final Property<String> hideForLocalesProperty = new StringProperty("hideForLocales").indexed().category(VISIBILITY_CATEGORY).partOfBuiltInSchema();
	public static final Property<String> showConditionsProperty = new StringProperty("showConditions").indexed().category(VISIBILITY_CATEGORY).hint("Conditions which have to be met in order for the element to be shown.<br><br>This is an 'auto-script' environment, meaning that the text is automatically surrounded with ${}").partOfBuiltInSchema();
	public static final Property<String> hideConditionsProperty = new StringProperty("hideConditions").indexed().category(VISIBILITY_CATEGORY).hint("Conditions which have to be met in order for the element to be hidden.<br><br>This is an 'auto-script' environment, meaning that the text is automatically surrounded with ${}").partOfBuiltInSchema();

	public static final Property<String> sharedComponentConfigurationProperty  = new StringProperty("sharedComponentConfiguration").format("multi-line").category(PAGE_CATEGORY).hint("The contents of this field will be evaluated before rendering this component. This is usually used to customize shared components to make them more flexible.<br><br>This is an 'auto-script' environment, meaning that the text is automatically surrounded with ${}").partOfBuiltInSchema();

	public static final Property<String> dataStructrIdProperty = new StringProperty("data-structr-id").hint("Set to ${current.id} most of the time").category(PAGE_CATEGORY).partOfBuiltInSchema();
	public static final Property<String> dataStructrHashProperty = new StringProperty("data-structr-hash").category(PAGE_CATEGORY).partOfBuiltInSchema();

	public static final Property<Boolean> dontCacheProperty = new BooleanProperty("dontCache").defaultValue(false).partOfBuiltInSchema();
	public static final Property<Boolean> isDOMNodeProperty = new ConstantBooleanProperty("isDOMNode", true).category(PAGE_CATEGORY).partOfBuiltInSchema();

	public static final Property<Integer> domSortPositionProperty = new IntProperty("domSortPosition").category(PAGE_CATEGORY).partOfBuiltInSchema();

	public static final View uiView = new View(DOMNode.class, PropertyView.Ui,
		reloadingActionsProperty, failureActionsProperty, successNotificationActionsProperty, failureNotificationActionsProperty
	);

	public static Property<NodeInterface> flow;

	static {

		final LicenseManager licenseManager = Services.getInstance().getLicenseManager();
		if (licenseManager == null || licenseManager.isModuleLicensed("api-builder")) {

			try {

				final String name = "org.structr.flow.impl.rels.DOMNodeFLOWFlowContainer";
				Class.forName(name);

				// FIXME: we need a property here that can only be initialized at runtime because
				// otherwise it would introduce a cyclic depdendency to the flow module..
				flow = new EndNode("flow", Class.forName(name));

			} catch (Throwable ignore) {}
		}
	}

	// ----- abstract methods -----
	public abstract String getContextName();
	public abstract boolean contentEquals(final Node node);
	public abstract void updateFromNode(final DOMNode otherNode) throws FrameworkException;

	// ----- property getters -----
	public boolean dontCache() {
		return getProperty(dontCacheProperty);
	}

	public String getRestQuery() {
		return getProperty(restQueryProperty);
	}

	public String getFunctionQuery() {
		return getProperty(functionQueryProperty);
	}

	public String getDataKey() {
		return getProperty(dataKeyProperty);
	}

	public String getShowConditions() {
		return getProperty(showConditionsProperty);
	}

	public String getHideConditions() {
		return getProperty(hideConditionsProperty);
	}

	public String getSharedComponentConfiguration() {
		return getProperty(sharedComponentConfigurationProperty);
	}

	public String getDataHash() {
		return getProperty(dataStructrHashProperty);
	}

	public String getIdHash() {
		return getUuid();
	}

	public DOMNode getParent() {
		return getProperty(parentProperty);
	}

	public Iterable<DOMNode> getChildren() {
		return getProperty(childrenProperty);
	}

	public DOMNode getNextSibling() {
		return getProperty(nextSiblingProperty);
	}

	public DOMNode getPreviousSibling() {
		return getProperty(previousSiblingProperty);
	}

	public Iterable<DOMNode> getSyncedNodes() {
		return getProperty(syncedNodesProperty);
	}

	public Page getOwnerDocument() {
		return getProperty(ownerDocumentProperty);
	}

	public DOMNode getSharedComponent() {
		return getProperty(sharedComponentProperty);
	}

	public void setOwnerDocument(final Page ownerDocument) throws FrameworkException {
		setProperty(ownerDocumentProperty, ownerDocument);
	}

	public void setSharedComponent(final DOMNode sharedComponent) throws FrameworkException {
		setProperty(sharedComponentProperty, sharedComponent);
	}

	// ----- abstract method implementations -----
	@Override
	public <R extends Relation<DOMNode, DOMNode, OneStartpoint<DOMNode>, ManyEndpoint<DOMNode>>> Class<R> getChildLinkType() {
		return (Class<R>)DOMNodeCONTAINSDOMNode.class;
	}

	@Override
	public <R extends Relation<DOMNode, DOMNode, OneStartpoint<DOMNode>, OneEndpoint<DOMNode>>> Class<R> getSiblingLinkType() {
		return (Class<R>)DOMNodeCONTAINS_NEXT_SIBLINGDOMNode.class;
	}

	@Override
	public Property<Integer> getPositionProperty() {
		return DOMNodeCONTAINSDOMNode.position;
	}

	// ----- public methods -----
	public void renderManagedAttributes(final AsyncBuffer out, final SecurityContext securityContext, final RenderContext renderContext) throws FrameworkException {
	}

	public String getCssClass() {
		return null;
	}

	@Override
	public void visitForUsage(final Map<String, Object> data) {

		super.visitForUsage(data);

		final Page page = getOwnerDocument();
		if (page != null) {

			data.put("page", page.getName());
		}

		data.put("path", getPagePath());
	}

	@Override
	public boolean isFrontendNode() {
		return true;
	}

	@Override
	public void onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		super.onCreation(securityContext, errorBuffer);

		this.checkName(errorBuffer);
		this.syncName(errorBuffer);
	}

	@Override
	public void onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		super.onModification(securityContext, errorBuffer, modificationQueue) ;

		this.increasePageVersion();
		this.checkName(errorBuffer);
		this.syncName(errorBuffer);

		final String uuid = this.getUuid();
		if (uuid != null) {

			// acknowledge all events for this node when it is modified
			RuntimeEventLog.getEvents(e -> uuid.equals(e.getData().get("id"))).stream().forEach(e -> e.acknowledge());
		}
	}

	public int getChildPosition(final DOMNode child) {
		return treeGetChildPosition(child);
	}

	public boolean avoidWhitespace() {
		return false;
	}

	public boolean isVoidElement() {
		return false;
	}

	public void setVisibility(final boolean publicUsers, final boolean authenticatedUsers) throws FrameworkException {

		setProperty(visibleToPublicUsers, publicUsers);
		setProperty(visibleToAuthenticatedUsers, authenticatedUsers);
	}

	public boolean inTrash() {
		return getParent() == null && getOwnerDocumentAsSuperUser() == null;
	}

	public boolean isSynced() {
		return getSharedComponent() != null || getSyncedNodes().iterator().hasNext();
	}

	// ----- static methods -----
	public static String escapeForHtml(final String raw) {
		return StringUtils.replaceEach(raw, new String[]{"&", "<", ">"}, new String[]{"&amp;", "&lt;", "&gt;"});
	}

	public static String unescapeForHtml(final String raw) {
		return StringUtils.replaceEach(raw, new String[]{"&amp;", "&lt;", "&gt;"}, new String[]{"&", "<", ">"});
	}

	public static String escapeForHtmlAttributes(final String raw) {
		return StringUtils.replaceEach(raw, new String[]{"&", "<", ">", "\""}, new String[]{"&amp;", "&lt;", "&gt;", "&quot;"});
	}

	public static String unescapeForHtmlAttributes(final String raw) {
		return StringUtils.replaceEach(raw, new String[]{"&amp;", "&lt;", "&gt;", "&quot;"}, new String[]{"&", "<", ">", "\""});
	}

	public static String objectToString(final Object source) {

		if (source != null) {
			return source.toString();
		}

		return null;
	}

	public static DOMNode cloneAndAppendChildren(final SecurityContext securityContext, final DOMNode nodeToClone) {

		final DOMNode newNode               = (DOMNode)nodeToClone.cloneNode(false);
		final List<DOMNode> childrenToClone = (List<DOMNode>)nodeToClone.getChildNodes();

		for (final DOMNode childNodeToClone : childrenToClone) {

			newNode.appendChild((DOMNode)cloneAndAppendChildren(securityContext, childNodeToClone));
		}

		return newNode;
	}

	public static Set<DOMNode> getAllChildNodes(final DOMNode node) {

		final Set<DOMNode> allChildNodes = new LinkedHashSet<>();

		getAllChildNodes(node, allChildNodes);

		return allChildNodes;
	}

	public static void getAllChildNodes(final DOMNode node, final Set<DOMNode> allChildNodes) {

		Node n = node.getFirstChild();

		while (n != null) {

			if (n instanceof DOMNode) {

				DOMNode domNode = (DOMNode)n;

				if (!allChildNodes.contains(domNode)) {

					allChildNodes.add(domNode);
					allChildNodes.addAll(getAllChildNodes(domNode));

				} else {

					// break loop!
					break;
				}
			}

			n = n.getNextSibling();
		}
	}

	public void renderNodeList(final SecurityContext securityContext, final RenderContext renderContext, final int depth, final String dataKey) throws FrameworkException {

		final Iterable<GraphObject> listSource = renderContext.getListSource();
		if (listSource != null) {

			final GraphObject previousDataObject = renderContext.getDataNode(dataKey);

			try {
				for (final Object dataObject : listSource) {

					// make current data object available in renderContext
					if (dataObject instanceof GraphObject) {

						renderContext.putDataObject(dataKey, (GraphObject)dataObject);

					} else if (dataObject instanceof Iterable) {

						renderContext.putDataObject(dataKey, Function.recursivelyWrapIterableInMap((Iterable)dataObject, 0));
					}

					this.renderContent(renderContext, depth + 1);
				}

			} finally {

				if (listSource instanceof AutoCloseable) {
					try { ((AutoCloseable)listSource).close(); } catch (Exception ex) {}
				}
			}

			// restore previous data object
			renderContext.putDataObject(dataKey, previousDataObject);
			renderContext.setDataObject(previousDataObject);
		}
	}

	public Template getClosestTemplate(final Page page) {

		DOMNode node = this;

		while (node != null) {

			if (node instanceof Template) {

				final Template template = (Template)node;

				Document doc = template.getOwnerDocument();

				if (doc == null) {

					doc = node.getClosestPage();
				}

				if (doc != null && (page == null || doc.equals(page))) {

					return template;

				}

				for (final DOMNode syncedNode : template.getSyncedNodes()) {

					doc = syncedNode.getOwnerDocument();

					if (doc != null && (page == null || doc.equals(page))) {

						return (Template)syncedNode;

					}
				}
			}

			node = (DOMNode)node.getParentNode();
		}

		return null;
	}

	public Page getClosestPage() {

		DOMNode node = this;

		while (node != null) {

			if (node instanceof Page) {

				return (Page)node;
			}

			node = (DOMNode)node.getParentNode();

		}

		return null;
	}

	public String getPositionPath() {

		String path = "";

		DOMNode currentNode = this;
		while (currentNode.getParentNode() != null) {

			DOMNode parentNode = (DOMNode)currentNode.getParentNode();

			path = "/" + parentNode.getChildPosition(currentNode) + path;

			currentNode = parentNode;

		}

		return path;

	}

	public String getContent(final RenderContext.EditMode editMode) throws FrameworkException {

		final RenderContext ctx         = new RenderContext(this.getSecurityContext(), null, null, editMode);
		final StringRenderBuffer buffer = new StringRenderBuffer();

		ctx.setBuffer(buffer);
		this.render(ctx, 0);

		// extract source
		return buffer.getBuffer().toString();
	}

	public String getIdHashOrProperty() {

		String idHash = this.getDataHash();
		if (idHash == null) {

			idHash = this.getIdHash();
		}

		return idHash;
	}

	public Page getOwnerDocumentAsSuperUser() {

		final RelationshipInterface ownership = this.getOutgoingRelationshipAsSuperUser(StructrApp.getConfiguration().getRelationshipEntityClass("DOMNodePAGEPage"));
		if (ownership != null) {

			return (Page)ownership.getTargetNode();
		}

		return null;
	}

	public boolean isSharedComponent() {

		final Document _ownerDocument = this.getOwnerDocumentAsSuperUser();
		if (_ownerDocument != null) {

			try {

				return _ownerDocument.equals(CreateComponentCommand.getOrCreateHiddenDocument());

			} catch (FrameworkException fex) {

				final Logger logger = LoggerFactory.getLogger(DOMNode.class);
				logger.warn("Unable fetch ShadowDocument node: {}", fex.getMessage());
			}
		}

		return false;
	}

	public static void copyAllAttributes(final DOMNode sourceNode, final DOMNode targetNode) {

		final SecurityContext securityContext = sourceNode.getSecurityContext();
		final PropertyMap properties          = new PropertyMap();

		for (final PropertyKey key : sourceNode.getPropertyKeys(PropertyView.Ui)) {

			// skip blacklisted properties
			if (cloneBlacklist.contains(key.jsonName())) {
				continue;
			}

			// skip tagName, otherwise the target node will have mismatching type and tag
			if ("tag".equals(key.jsonName())) {
				continue;
			}

			if (!key.isUnvalidated()) {
				properties.put(key, sourceNode.getProperty(key));
			}
		}

		// htmlView is necessary for the cloning of DOM nodes - otherwise some properties won't be cloned
		for (final PropertyKey key : sourceNode.getPropertyKeys(PropertyView.Html)) {

			// skip blacklisted properties
			if (cloneBlacklist.contains(key.jsonName())) {
				continue;
			}

			// skip tagName, otherwise the target node will have mismatching type and tag
			if ("tag".equals(key.jsonName())) {
				continue;
			}

			if (!key.isUnvalidated()) {
				properties.put(key, sourceNode.getProperty(key));
			}
		}

		// also clone data-* attributes
		for (final PropertyKey key : sourceNode.getDataPropertyKeys()) {

			// skip blacklisted properties
			if (cloneBlacklist.contains(key.jsonName())) {
				continue;
			}

			if (!key.isUnvalidated()) {
				properties.put(key, sourceNode.getProperty(key));
			}
		}

		if (sourceNode instanceof LinkSource) {

			final LinkSource linkSourceElement = (LinkSource)sourceNode;

			properties.put(StructrApp.key(LinkSource.class, "linkable"), linkSourceElement.getLinkable());
		}

		final App app = StructrApp.getInstance(securityContext);

		try {

			// set the properties we collected above
			targetNode.setProperties(securityContext, properties);

			// for clone, always copy permissions
			sourceNode.copyPermissionsTo(securityContext, targetNode, true);

		} catch (FrameworkException ex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, ex.toString());
		}
	}

	public String getTextContent() throws DOMException {

		final DOMNodeList results         = new DOMNodeList();
		final TextCollector textCollector = new TextCollector();

		DOMNode.collectNodesByPredicate(this.getSecurityContext(), this, results, textCollector, 0, false);

		return textCollector.getText();
	}

	static void collectNodesByPredicate(final SecurityContext securityContext, Node startNode, DOMNodeList results, Predicate<Node> predicate, int depth, boolean stopOnFirstHit) {

		if (predicate instanceof Filter) {

			((Filter)predicate).setSecurityContext(securityContext);
		}

		if (predicate.accept(startNode)) {

			results.add(startNode);

			if (stopOnFirstHit) {

				return;
			}
		}

		NodeList _children = startNode.getChildNodes();
		if (_children != null) {

			int len = _children.getLength();
			for (int i = 0; i < len; i++) {

				Node child = _children.item(i);

				collectNodesByPredicate(securityContext, child, results, predicate, depth + 1, stopOnFirstHit);
			}
		}
	}

	public void normalize() {

		final Document document = this.getOwnerDocument();
		if (document != null) {

			// merge adjacent text nodes until there is only one left
			Node child = this.getFirstChild();
			while (child != null) {

				if (child instanceof Text) {

					Node next = child.getNextSibling();
					if (next != null && next instanceof Text) {

						String text1 = child.getNodeValue();
						String text2 = next.getNodeValue();

						// create new text node
						final Text newText = document.createTextNode(text1.concat(text2));

						this.removeChild(child);
						this.insertBefore(newText, next);
						this.removeChild(next);

						child = newText;

					} else {

						// advance to next node
						child = next;
					}

				} else {

					// advance to next node
					child = child.getNextSibling();

				}
			}

			// recursively normalize child nodes
			if (this.hasChildNodes()) {

				Node currentChild = this.getFirstChild();
				while (currentChild != null) {

					currentChild.normalize();
					currentChild = currentChild.getNextSibling();
				}
			}
		}
	}

	public Node appendChild(final Node newChild) throws DOMException {

		this.checkWriteAccess();
		this.checkSameDocument(newChild);
		this.checkHierarchy(newChild);

		try {

			if (newChild instanceof DocumentFragment) {

				// When inserting document fragments, we must take
				// care of the special case that the nodes already
				// have a NEXT_LIST_ENTRY relationship coming from
				// the document fragment, so we must first remove
				// the node from the document fragment and then
				// add it to the new parent.
				// replace indirectly using insertBefore and remove
				final DocumentFragment fragment = (DocumentFragment)newChild;
				Node currentChild = fragment.getFirstChild();

				while (currentChild != null) {

					// save next child in fragment list for later use
					final Node savedNextChild = currentChild.getNextSibling();

					// remove child from document fragment
					fragment.removeChild(currentChild);

					// append child to new parent
					this.appendChild(currentChild);

					// next
					currentChild = savedNextChild;
				}

			} else {

				final Node _parent = newChild.getParentNode();

				if (_parent != null && _parent instanceof DOMNode) {
					_parent.removeChild(newChild);
				}

				this.doAppendChild((DOMNode)newChild);

				// allow parent to set properties in new child
				this.handleNewChild(newChild);
			}

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());
		}

		return newChild;
	}

	public Node removeChild(final Node node) throws DOMException {

		this.checkWriteAccess();
		this.checkSameDocument(node);
		this.checkIsChild(node);

		try {

			this.doRemoveChild((DOMNode)node);

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());
		}

		return node;
	}

	public void checkIsChild(final Node otherNode) throws DOMException {

		if (otherNode instanceof DOMNode) {

			Node _parent = otherNode.getParentNode();

			if (!this.isSameNode(_parent)) {

				throw new DOMException(DOMException.NOT_FOUND_ERR, NOT_FOUND_ERR_MESSAGE);
			}

			// validation successful
			return;
		}

		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, NOT_SUPPORTED_ERR_MESSAGE);
	}

	public void checkHierarchy(final Node otherNode) throws DOMException {

		// we can only check DOMNodes
		if (otherNode instanceof DOMNode) {

			// verify that the other node is not this node
			if (this.isSameNode(otherNode)) {
				throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, HIERARCHY_REQUEST_ERR_MESSAGE_SAME_NODE);
			}

			// verify that otherNode is not one of the
			// the ancestors of this node
			// (prevent circular relationships)
			Node _parent = this.getParentNode();
			while (_parent != null) {

				if (_parent.isSameNode(otherNode)) {
					throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, HIERARCHY_REQUEST_ERR_MESSAGE_ANCESTOR);
				}

				_parent = _parent.getParentNode();
			}

			// TODO: check hierarchy constraints imposed by the schema
			// validation successful
			return;
		}

		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, NOT_SUPPORTED_ERR_MESSAGE);
	}

	public void checkSameDocument(final Node otherNode) throws DOMException {

		Document doc = this.getOwnerDocument();

		if (doc != null) {

			Document otherDoc = otherNode.getOwnerDocument();

			// Shadow doc is neutral
			if (otherDoc != null && !doc.equals(otherDoc) && !(doc instanceof ShadowDocument)) {

				final Logger logger = LoggerFactory.getLogger(DOMNode.class);
				logger.warn("{} node with UUID {} has owner document {} with UUID {} whereas this node has owner document {} with UUID {}",
					otherNode.getClass().getSimpleName(),
					((NodeInterface)otherNode).getUuid(),
					otherDoc.getClass().getSimpleName(),
					((NodeInterface)otherDoc).getUuid(),
					doc.getClass().getSimpleName(),
					((NodeInterface)doc).getUuid()
				);

				throw new DOMException(DOMException.WRONG_DOCUMENT_ERR, WRONG_DOCUMENT_ERR_MESSAGE);
			}

			if (otherDoc == null) {

				((DOMNode)otherNode).doAdopt((Page)doc);

			}
		}
	}

	public void checkWriteAccess() throws DOMException {

		if (!this.isGranted(Permission.write, this.getSecurityContext())) {

			throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, NO_MODIFICATION_ALLOWED_MESSAGE);
		}
	}

	public void checkReadAccess() throws DOMException {

		final SecurityContext securityContext = this.getSecurityContext();

		// superuser can do everything
		if (securityContext != null && securityContext.isSuperUser()) {
			return;
		}

		if (securityContext.isVisible(this) || this.isGranted(Permission.read, securityContext)) {
			return;
		}

		throw new DOMException(DOMException.INVALID_ACCESS_ERR, INVALID_ACCESS_ERR_MESSAGE);
	}

	@Override
	public boolean hasChildNodes() {
		return getProperty(childrenProperty).iterator().hasNext();
	}

	public static String indent(final int depth, final RenderContext renderContext) {

		if (!renderContext.shouldIndentHtml()) {
			return "";
		}

		StringBuilder indent = new StringBuilder("\n");

		for (int d = 0; d < depth; d++) {

			indent.append("	");

		}

		return indent.toString();
	}

	public boolean displayForLocale(final RenderContext renderContext) {

		String localeString = renderContext.getLocale().toString();

		String show = this.getProperty(showForLocalesProperty);
		String hide = this.getProperty(hideForLocalesProperty);

		// If both fields are empty, render node
		if (StringUtils.isBlank(hide) && StringUtils.isBlank(show)) {
			return true;
		}

		// If locale string is found in hide, don't render
		if (StringUtils.contains(hide, localeString)) {
			return false;
		}

		// If locale string is found in hide, don't render
		if (StringUtils.isNotBlank(show) && !StringUtils.contains(show, localeString)) {
			return false;
		}

		return true;

	}

	public boolean displayForConditions(final RenderContext renderContext)  {

		String _showConditions = this.getProperty(showConditionsProperty);
		String _hideConditions = this.getProperty(hideConditionsProperty);

		// If both fields are empty, render node
		if (StringUtils.isBlank(_hideConditions) && StringUtils.isBlank(_showConditions)) {
			return true;
		}

		try {
			// If hide conditions evaluates to "true", don't render
			if (StringUtils.isNotBlank(_hideConditions) && Boolean.TRUE.equals(Scripting.evaluate(renderContext, this, "${".concat(_hideConditions.trim()).concat("}"), "hideConditions", this.getUuid()))) {
				return false;
			}

		} catch (UnlicensedScriptException | FrameworkException ex) {

			final Logger logger        = LoggerFactory.getLogger(DOMNode.class);
			final boolean isShadowPage = isSharedComponent();

			if (!isShadowPage) {

				final DOMNode ownerDocument = this.getOwnerDocumentAsSuperUser();
				DOMNode.logScriptingError(logger, ex, "Error while evaluating hide condition '{}' in page {}[{}], DOMNode[{}]", _hideConditions, ownerDocument.getProperty(AbstractNode.name), ownerDocument.getProperty(AbstractNode.id), this.getUuid());

			} else {

				DOMNode.logScriptingError(logger, ex, "Error while evaluating hide condition '{}' in shared component, DOMNode[{}]", _hideConditions, this.getUuid());
			}
		}

		try {
			// If show conditions evaluates to "false", don't render
			if (StringUtils.isNotBlank(_showConditions) && Boolean.FALSE.equals(Scripting.evaluate(renderContext, this, "${".concat(_showConditions.trim()).concat("}"), "showConditions", this.getUuid()))) {
				return false;
			}

		} catch (UnlicensedScriptException | FrameworkException ex) {

			final Logger logger        = LoggerFactory.getLogger(DOMNode.class);
			final boolean isShadowPage = isSharedComponent();

			if (!isShadowPage) {

				final DOMNode ownerDocument = this.getOwnerDocumentAsSuperUser();
				DOMNode.logScriptingError(logger, ex, "Error while evaluating show condition '{}' in page {}[{}], DOMNode[{}]", _showConditions, ownerDocument.getProperty(AbstractNode.name), ownerDocument.getProperty(AbstractNode.id), this.getUuid());

			} else {

				DOMNode.logScriptingError(logger, ex, "Error while evaluating show condition '{}' in shared component, DOMNode[{}]", _showConditions, this.getUuid());
			}
		}

		return true;
	}

	public static void logScriptingError (final Logger logger, final Throwable t, String message,  Object... arguments) {

		if (t instanceof UnlicensedScriptException) {

			message += "\n{}";
			arguments = ArrayUtils.add(arguments, t.getMessage());

		} else if (t.getCause() instanceof UnlicensedScriptException) {

			message += "\n{}";
			arguments = ArrayUtils.add(arguments, t.getCause().getMessage());

		} else {

			arguments = ArrayUtils.add(arguments, t);
		}

		logger.error(message, arguments);
	}

	public boolean shouldBeRendered(final RenderContext renderContext) {

		// In raw, widget or deployment mode, render everything
		EditMode editMode = renderContext.getEditMode(renderContext.getSecurityContext().getUser(false));
		if (EditMode.DEPLOYMENT.equals(editMode) || EditMode.RAW.equals(editMode) || EditMode.WIDGET.equals(editMode)) {
			return true;
		}

		if (this.isHidden() || !this.displayForLocale(renderContext) || !this.displayForConditions(renderContext)) {
			return false;
		}

		return true;
	}

	public boolean renderDeploymentExportComments(final AsyncBuffer out, final boolean isContentNode) {

		final Set<String> instructions = new LinkedHashSet<>();

		this.getVisibilityInstructions(instructions);
		this.getLinkableInstructions(instructions);
		this.getSecurityInstructions(instructions);

		if (this.isHidden()) {
			instructions.add("@structr:hidden");
		}

		if (isContentNode) {

			// special rules apply for content nodes: since we can not store
			// structr-specific properties in the attributes of the element,
			// we need to encode those attributes in instructions.
			this.getContentInstructions(instructions);
		}

		if (!instructions.isEmpty()) {

			out.append("<!-- ");

			for (final Iterator<String> it = instructions.iterator(); it.hasNext();) {

				final String instruction = it.next();

				out.append(instruction);

				if (it.hasNext()) {
					out.append(", ");
				}
			}

			out.append(" -->");

			return true;

		} else {

			return false;
		}
	}

	public void renderSharedComponentConfiguration(final AsyncBuffer out, final EditMode editMode) {

		if (EditMode.DEPLOYMENT.equals(editMode)) {

			final String configuration = this.getProperty(sharedComponentConfigurationProperty);
			if (StringUtils.isNotBlank(configuration)) {

				out.append(" data-structr-meta-shared-component-configuration=\"");
				out.append(escapeForHtmlAttributes(configuration));
				out.append("\"");
			}
		}
	}

	public void getContentInstructions(final Set<String> instructions) {

		final String _contentType = this.getProperty(Content.contentTypeProperty);
		if (_contentType != null) {

			instructions.add("@structr:content(" + escapeForHtmlAttributes(_contentType) + ")");
		}

		if (!this.getType().equals("Template")) {

			final String _name = this.getProperty(AbstractNode.name);
			if (StringUtils.isNotEmpty(_name)) {

				instructions.add("@structr:name(" + escapeForHtmlAttributes(_name) + ")");
			}

			final String _showConditions = this.getShowConditions();
			if (StringUtils.isNotEmpty(_showConditions)) {

				instructions.add("@structr:show(" + escapeForHtmlAttributes(_showConditions) + ")");
			}

			final String _hideConditions = this.getHideConditions();
			if (StringUtils.isNotEmpty(_hideConditions)) {

				instructions.add("@structr:hide(" + escapeForHtmlAttributes(_hideConditions) + ")");
			}

			final String _showForLocales = this.getProperty(showForLocalesProperty);
			if (StringUtils.isNotEmpty(_showForLocales)) {

				instructions.add("@structr:show-for-locales(" + escapeForHtmlAttributes(_showForLocales) + ")");
			}

			final String _hideForLocales = this.getProperty(hideForLocalesProperty);
			if (StringUtils.isNotEmpty(_hideForLocales)) {

				instructions.add("@structr:hide-for-locales(" + escapeForHtmlAttributes(_hideForLocales) + ")");
			}
		}
	}

	public void getLinkableInstructions(final Set<String> instructions) {

		if (this instanceof LinkSource) {

			final LinkSource linkSourceElement = (LinkSource)this;
			final Linkable linkable            = linkSourceElement.getLinkable();

			if (linkable != null) {

				final String linkableInstruction = (linkable instanceof Page) ? "pagelink" : "link";

				String path                = linkable.getPath();

				if (linkable instanceof Page && path == null) {
					path = linkable.getName();
				}

				if (path != null) {

					instructions.add("@structr:" + linkableInstruction + "(" + path + ")");

				} else {

					final Logger logger = LoggerFactory.getLogger(DOMNode.class);
					logger.warn("Cannot export linkable relationship, no path.");
				}
			}
		}
	}

	public void getVisibilityInstructions(final Set<String> instructions) {

		final DOMNode _parentNode       = (DOMNode)this.getParent();

		final boolean elementPublic     = this.isVisibleToPublicUsers();
		final boolean elementProtected  = this.isVisibleToAuthenticatedUsers();
		final boolean elementPrivate    = !elementPublic && !elementProtected;
		final boolean elementPublicOnly = elementPublic && !elementProtected;


		boolean addVisibilityInstructions = false;

		if (_parentNode == null) {

			// no parent -> output visibility flags
			addVisibilityInstructions = true;

		} else {

			// parents visibility flags are different or parent is shadowpage -> output visibility flags
			final boolean parentPublic      = _parentNode.isVisibleToPublicUsers();
			final boolean parentProtected   = _parentNode.isVisibleToAuthenticatedUsers();

			addVisibilityInstructions = (_parentNode instanceof ShadowDocument) || (parentPublic != elementPublic || parentProtected != elementProtected);
		}

		if (addVisibilityInstructions) {

			if (elementPublicOnly) {
				instructions.add("@structr:public-only");
				return;
			}
			if (elementPublic && elementProtected) {
				instructions.add("@structr:public");
				return;
			}
			if (elementProtected) {
				instructions.add("@structr:protected");
				return;
			}
			if (elementPrivate) {
				instructions.add("@structr:private");
				return;
			}
		}
	}

	public void getSecurityInstructions(final Set<String> instructions) {

		final PrincipalInterface _owner = this.getOwnerNode();
		if (_owner != null) {

			instructions.add("@structr:owner(" + _owner.getProperty(AbstractNode.name) + ")");
		}

		this.getSecurityRelationships().stream().filter(Objects::nonNull).sorted(Comparator.comparing(security -> security.getSourceNode().getProperty(AbstractNode.name))).forEach(security -> {

			final PrincipalInterface grantee = security.getSourceNode();
			final Set<String> perms = security.getPermissions();
			final StringBuilder shortPerms = new StringBuilder();

			for (final Permission p : Permission.allPermissions) {

				if (perms.contains(p.name())) {
					// first character only
					shortPerms.append(p.name().substring(0, 1));
				}
			}

			if (shortPerms.length() > 0) {
				// ignore SECURITY-relationships without permissions
				instructions.add("@structr:grant(" + grantee.getProperty(AbstractNode.name) + "," + shortPerms.toString() + ")");
			}
		});
	}

	public void renderCustomAttributes(final AsyncBuffer out, final SecurityContext securityContext, final RenderContext renderContext) throws FrameworkException {

		final EditMode editMode = renderContext.getEditMode(securityContext.getUser(false));

		Set<PropertyKey> dataAttributes = this.getDataPropertyKeys();

		if (EditMode.DEPLOYMENT.equals(editMode)) {
			List sortedAttributes = new LinkedList(dataAttributes);
			Collections.sort(sortedAttributes);
			dataAttributes = new LinkedHashSet<>(sortedAttributes);
		}

		for (final PropertyKey key : dataAttributes) {

			// do not render attributes that are on the blacklist
			if (DataAttributeOutputBlacklist.contains(key.jsonName()) && !EditMode.DEPLOYMENT.equals(editMode)) {
				continue;
			}

			String value = "";

			if (EditMode.DEPLOYMENT.equals(editMode)) {

				final Object obj = this.getProperty(key);
				if (obj != null) {

					value = obj.toString();
				}

			} else {

				value = this.getPropertyWithVariableReplacement(renderContext, key);
				if (value != null) {

					value = value.trim();
				}
			}

			if (!(EditMode.RAW.equals(editMode) || EditMode.WIDGET.equals(editMode))) {

				value = escapeForHtmlAttributes(value);
			}

			if (StringUtils.isNotBlank(value)) {

				if (key instanceof CustomHtmlAttributeProperty) {
					out.append(" ").append(((CustomHtmlAttributeProperty)key).cleanName()).append("=\"").append(value).append("\"");
				} else {
					out.append(" ").append(key.dbName()).append("=\"").append(value).append("\"");
				}
			}
		}

		if (EditMode.DEPLOYMENT.equals(editMode) || EditMode.RAW.equals(editMode) || EditMode.WIDGET.equals(editMode)) {

			if (EditMode.DEPLOYMENT.equals(editMode)) {

				// export name property if set
				final String name = this.getProperty(AbstractNode.name);
				if (name != null) {

					out.append(" data-structr-meta-name=\"").append(escapeForHtmlAttributes(name)).append("\"");
				}

				out.append(" data-structr-meta-id=\"").append(this.getUuid()).append("\"");
			}

			for (final String p : rawProps) {

				final String htmlName = "data-structr-meta-" + CaseHelper.toUnderscore(p, false).replaceAll("_", "-");
				final PropertyKey key = StructrApp.key(DOMNode.class, p, false);

				if (key != null) {

					final Object value    = this.getProperty(key);
					if (value != null) {

						final boolean isBoolean  = key instanceof BooleanProperty;
						final String stringValue = value.toString();

						if ((isBoolean && "true".equals(stringValue)) || (!isBoolean && StringUtils.isNotBlank(stringValue))) {
							out.append(" ").append(htmlName).append("=\"").append(escapeForHtmlAttributes(stringValue)).append("\"");
						}
					}
				}
			}
		}
	}

	public Set<PropertyKey> getDataPropertyKeys() {

		final Set<PropertyKey> customProperties = new TreeSet<>();
		final org.structr.api.graph.Node dbNode = this.getNode();
		final Iterable<String> props            = dbNode.getPropertyKeys();

		for (final String key : props) {

			PropertyKey propertyKey = StructrApp.key(this.getClass(), key, false);
			if (propertyKey == null) {

				// support arbitrary data-* attributes
				propertyKey = new StringProperty(key);
			}

			if (key.startsWith("data-")) {

				if (propertyKey instanceof BooleanProperty && dbNode.hasProperty(key)) {

					final Object defaultValue = propertyKey.defaultValue();
					final Object nodeValue    = dbNode.getProperty(key);

					// don't export boolean false values (which is the default)
					if (nodeValue != null && Boolean.FALSE.equals(nodeValue) && (defaultValue == null || nodeValue.equals(defaultValue))) {

						continue;
					}
				}

				customProperties.add(propertyKey);

			} else if (key.startsWith(CustomHtmlAttributeProperty.CUSTOM_HTML_ATTRIBUTE_PREFIX)) {

				final CustomHtmlAttributeProperty customProp = new CustomHtmlAttributeProperty(propertyKey);

				customProperties.add(customProp);
			}
		}

		return customProperties;
	}

	public void handleNewChild(Node newChild) {


		try {

			final Page page            = (Page)this.getOwnerDocument();
			final DOMNode newChildNode = (DOMNode)newChild;

			newChildNode.setOwnerDocument(page);

			for (final DOMNode child : DOMNode.getAllChildNodes(newChildNode)) {

					child.setOwnerDocument(page);
			}

		} catch (FrameworkException ex) {

			final Logger logger = LoggerFactory.getLogger(DOMNode.class);
			logger.warn("", ex);
		}
	}

	@Override
	public void render(final RenderContext renderContext, final int depth) throws FrameworkException {

		final SecurityContext securityContext = renderContext.getSecurityContext();
		final EditMode editMode = renderContext.getEditMode(securityContext.getUser(false));

		// admin-only edit modes ==> visibility check not necessary
		final boolean isAdminOnlyEditMode = (EditMode.RAW.equals(editMode) || EditMode.WIDGET.equals(editMode) || EditMode.DEPLOYMENT.equals(editMode));
		final boolean isPartial           = renderContext.isPartialRendering(); // renderContext.getPage() == null;

		if (!isAdminOnlyEditMode && !securityContext.isVisible(this)) {
			return;
		}

		// special handling for tree items that explicitly opt-in to be controlled automatically, configured with the toggle-tree-item event.
		final String treeItemDataKey = this.getProperty(StructrApp.key(DOMElement.class, "data-structr-tree-children"));
		if (treeItemDataKey != null) {

			final GraphObject treeItem = renderContext.getDataNode(treeItemDataKey);
			if (treeItem != null) {

				final String key = this.getTreeItemSessionIdentifier(treeItem.getUuid());

				if (this.getSessionAttribute(renderContext.getSecurityContext(), key) == null) {

					// do not render children of tree elements
					return;
				}
			}
		}

		final GraphObject details = renderContext.getDetailsDataObject();
		final boolean detailMode = details != null;

		if (isAdminOnlyEditMode) {

			this.renderContent(renderContext, depth);

		} else {

			final String subKey = this.getDataKey();

			if (StringUtils.isNotBlank(subKey)) {

				// fetch (optional) list of external data elements
				final Iterable<GraphObject> listData = checkListSources(securityContext, renderContext);

				final PropertyKey propertyKey;

				// Make sure the closest 'page' keyword is always set also for partials
				if (depth == 0 && isPartial) {

					renderContext.setPage(this.getClosestPage());

				}

				final GraphObject dataObject = renderContext.getDataNode(subKey); // renderContext.getSourceDataObject();

				// Render partial with possible top-level repeater limited to a single data object
				if (depth == 0 && isPartial && dataObject != null) {

					renderContext.putDataObject(subKey, dataObject);
					this.renderContent(renderContext, depth);

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

								for (Object o : (Iterable)elements) {

									if (o instanceof GraphObject) {

										GraphObject graphObject = (GraphObject)o;
										renderContext.putDataObject(subKey, graphObject);
										this.renderContent(renderContext, depth);

									}
								}

							}

						} else {

							propertyKey = StructrApp.getConfiguration().getPropertyKeyForJSONName(currentDataNode.getClass(), subKey, false);
							renderContext.setRelatedProperty(propertyKey);

							if (propertyKey != null) {

								final Object value = currentDataNode.getProperty(propertyKey);
								if (value != null) {

									if (value instanceof Iterable) {

										for (final Object o : ((Iterable)value)) {

											if (o instanceof GraphObject) {

												renderContext.putDataObject(subKey, (GraphObject)o);
												this.renderContent(renderContext, depth);

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
						this.renderNodeList(securityContext, renderContext, depth, subKey);

					}
				}

			} else {

				this.renderContent(renderContext, depth);
			}
		}
	}

	public Iterable<GraphObject> checkListSources(final SecurityContext securityContext, final RenderContext renderContext) {

		// try registered data sources first
		for (final GraphDataSource<Iterable<GraphObject>> source : DataSources.getDataSources()) {

			try {

				final Iterable<GraphObject> graphData = source.getData(renderContext, this);
				if (graphData != null && !Iterables.isEmpty(graphData)) {

					return graphData;
				}

			} catch (FrameworkException fex) {

				LoggerFactory.getLogger(DOMNode.class).warn("Could not retrieve data from graph data source {} in {} {}: {}", source.getClass().getSimpleName(), this.getType(), this.getUuid(), fex.getMessage());
			}
		}

		return Collections.EMPTY_LIST;
	}

	// ----- interface org.w3c.dom.Node -----
	@Override
	public String getPrefix() {
		return null;
	}

	@Override
	public void setPrefix(final String prefix) throws DOMException {
	}

	@Override
	public String getNamespaceURI() {
		return null;
	}

	@Override
	public String getBaseURI() {
		return null;
	}

	@Override
	public boolean isEqualNode(final Node arg) {
		return equals(arg);
	}

	@Override
	public String lookupNamespaceURI(final String prefix) {
		return null;
	}

	@Override
	public boolean isDefaultNamespace(final String namespaceURI) {
		return true;
	}

	@Override
	public String lookupPrefix(final String namespaceURI) {
		return null;
	}

	@Override
	public void setTextContent(String textContent) throws DOMException {
	}

	@Override
	public Object setUserData(final String key, final Object data, final UserDataHandler handler) {
		return null;
	}

	@Override
	public Object getUserData(final String key) {
		return null;
	}

	@Override
	public Object getFeature(final String feature, final String version) {
		return null;
	}
	
	@Override
	public boolean isSupported(final String feature, final String version) {
		return false;
	}

	@Override
	public short compareDocumentPosition(final Node node) {
		return 0;
	}

	@Override
	public boolean isSameNode(final Node node) {

		if (node != null && node instanceof DOMNode) {

			String otherId = ((DOMNode)node).getProperty(GraphObject.id);
			String ourId   = this.getProperty(GraphObject.id);

			if (ourId != null && otherId != null && ourId.equals(otherId)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public Node cloneNode(final boolean deep) {

		final SecurityContext securityContext = this.getSecurityContext();

		if (deep) {

			return cloneAndAppendChildren(securityContext, this);

		} else {

			final PropertyMap properties = new PropertyMap();

			for (final PropertyKey key : this.getPropertyKeys(PropertyView.Ui)) {

				// skip blacklisted properties
				if (cloneBlacklist.contains(key.jsonName())) {
					continue;
				}


				if (!key.isUnvalidated()) {
					properties.put(key, this.getProperty(key));
				}
			}

			// htmlView is necessary for the cloning of DOM nodes - otherwise some properties won't be cloned
			for (final PropertyKey key : this.getPropertyKeys(PropertyView.Html)) {

				// skip blacklisted properties
				if (cloneBlacklist.contains(key.jsonName())) {
					continue;
				}

				if (!key.isUnvalidated()) {
					properties.put(key, this.getProperty(key));
				}
			}

			// also clone data-* attributes
			for (final PropertyKey key : this.getDataPropertyKeys()) {

				// skip blacklisted properties
				if (cloneBlacklist.contains(key.jsonName())) {
					continue;
				}

				if (!key.isUnvalidated()) {
					properties.put(key, this.getProperty(key));
				}
			}

			if (this instanceof LinkSource) {

				final LinkSource linkSourceElement = (LinkSource)this;

				properties.put(StructrApp.key(LinkSource.class, "linkable"), linkSourceElement.getLinkable());
			}

			final App app = StructrApp.getInstance(securityContext);

			try {

				final DOMNode clone = app.create(this.getClass(), properties);

				// for clone, always copy permissions
				this.copyPermissionsTo(securityContext, clone, true);

				return clone;

			} catch (FrameworkException ex) {

				throw new DOMException(DOMException.INVALID_STATE_ERR, ex.toString());
			}
		}
	}

	public Node doAdopt(final Page _page) throws DOMException {

		if (_page != null) {

			try {

				this.setOwnerDocument(_page);

			} catch (FrameworkException fex) {

				throw new DOMException(DOMException.INVALID_STATE_ERR, fex.getMessage());

			}
		}

		return this;
	}

	public Node insertBefore(final Node newChild, final Node refChild) throws DOMException {

		// according to DOM spec, insertBefore with null refChild equals appendChild
		if (refChild == null) {

			return this.appendChild(newChild);
		}

		this.checkWriteAccess();

		this.checkSameDocument(newChild);
		this.checkSameDocument(refChild);

		this.checkHierarchy(newChild);
		this.checkHierarchy(refChild);

		if (newChild instanceof DocumentFragment) {

			// When inserting document fragments, we must take
			// care of the special case that the nodes already
			// have a NEXT_LIST_ENTRY relationship coming from
			// the document fragment, so we must first remove
			// the node from the document fragment and then
			// add it to the new parent.
			final DocumentFragment fragment = (DocumentFragment)newChild;
			Node currentChild = fragment.getFirstChild();

			while (currentChild != null) {

				// save next child in fragment list for later use
				Node savedNextChild = currentChild.getNextSibling();

				// remove child from document fragment
				fragment.removeChild(currentChild);

				// insert child into new parent
				this.insertBefore(currentChild, refChild);

				// next
				currentChild = savedNextChild;
			}

		} else {

			final Node _parent = newChild.getParentNode();
			if (_parent != null) {

				_parent.removeChild(newChild);
			}

			try {

				// do actual tree insertion here
				this.treeInsertBefore((DOMNode)newChild, (DOMNode)refChild);

			} catch (FrameworkException frex) {

				if (frex.getStatus() == 404) {

					throw new DOMException(DOMException.NOT_FOUND_ERR, frex.getMessage());

				} else {

					throw new DOMException(DOMException.INVALID_STATE_ERR, frex.getMessage());
				}
			}

			// allow parent to set properties in new child
			this.handleNewChild(newChild);
		}

		return refChild;
	}

	public Node replaceChild(final Node newChild, final Node oldChild) throws DOMException {

		this.checkWriteAccess();

		this.checkSameDocument(newChild);
		this.checkSameDocument(oldChild);

		this.checkHierarchy(newChild);
		this.checkHierarchy(oldChild);

		if (newChild instanceof DocumentFragment) {

			// When inserting document fragments, we must take
			// care of the special case that the nodes already
			// have a NEXT_LIST_ENTRY relationship coming from
			// the document fragment, so we must first remove
			// the node from the document fragment and then
			// add it to the new parent.
			// replace indirectly using insertBefore and remove
			final DocumentFragment fragment = (DocumentFragment)newChild;
			Node currentChild = fragment.getFirstChild();

			while (currentChild != null) {

				// save next child in fragment list for later use
				final Node savedNextChild = currentChild.getNextSibling();

				// remove child from document fragment
				fragment.removeChild(currentChild);

				// add child to new parent
				this.insertBefore(currentChild, oldChild);

				// next
				currentChild = savedNextChild;
			}

			// finally, remove reference element
			this.removeChild(oldChild);

		} else {

			Node _parent = newChild.getParentNode();
			if (_parent != null && _parent instanceof DOMNode) {

				_parent.removeChild(newChild);
			}

			try {
				// replace directly
				this.treeReplaceChild((DOMNode)newChild, (DOMNode)oldChild);

			} catch (FrameworkException frex) {

				if (frex.getStatus() == 404) {

					throw new DOMException(DOMException.NOT_FOUND_ERR, frex.getMessage());

				} else {

					throw new DOMException(DOMException.INVALID_STATE_ERR, frex.getMessage());
				}
			}

			// allow parent to set properties in new child
			this.handleNewChild(newChild);
		}

		return oldChild;
	}

	List<Node> getAncestors() {

		List<Node> ancestors = new ArrayList<>();

		Node _parent = this.getParentNode();
		while (_parent != null) {

			ancestors.add(_parent);
			_parent = _parent.getParentNode();
		}

		return ancestors;
	}

	void increasePageVersion() throws FrameworkException {

		Page page = null;

		if (this instanceof Page) {

			page = (Page)this;

		} else {

			// ignore page-less nodes
			if (this.getParent() == null) {
				return;
			}
		}

		if (page == null) {

			final List<Node> ancestors = getAncestors();
			if (!ancestors.isEmpty()) {

				final DOMNode rootNode = (DOMNode)ancestors.get(ancestors.size() - 1);
				if (rootNode instanceof Page) {

					page = (Page)rootNode;

				} else {

					rootNode.increasePageVersion();
				}

			} else {

				for (final DOMNode syncedNode : this.getSyncedNodes()) {

					syncedNode.increasePageVersion();
				}
			}

		}

		if (page != null) {

			page.increaseVersion();

		}
	}

	@Override
	public String getEntityContextPath() {

		return getPagePath();
	}

	// ----- LinkedTreeNode -----
	void doAppendChild(final DOMNode node) throws FrameworkException {

		checkWriteAccess();
		treeAppendChild(node);
	}

	void doRemoveChild(final DOMNode node) throws FrameworkException {

		checkWriteAccess();
		treeRemoveChild(node);
	}

	public Node getFirstChild() {

		checkReadAccess();
		return treeGetFirstChild();
	}

	public Node getLastChild() {

		checkReadAccess();
		return treeGetLastChild();
	}

	public DOMNodeList getChildNodes() {

		checkReadAccess();
		return new DOMNodeList(treeGetChildren());
	}

	public Node getParentNode() {

		checkReadAccess();
		return getParent();

	}

	public List<DOMNodeCONTAINSDOMNode> getChildRelationships() {
		return treeGetChildRelationships();
	}

	public String getPagePath() {

		String cachedPagePath = (String)this.getTemporaryStorage().get("cachedPagePath");
		if (cachedPagePath == null) {

			final StringBuilder buf = new StringBuilder();
			DOMNode current         = this;

			while (current != null) {

				buf.insert(0, "/" + current.getContextName());
				current = current.getParent();
			}

			cachedPagePath = buf.toString();

			this.getTemporaryStorage().put("cachedPagePath", cachedPagePath);
		}

		return cachedPagePath;
	}

	void checkName(final ErrorBuffer errorBuffer) {

		final String _name = this.getProperty(AbstractNode.name);
		if (_name != null) {

			if (_name.contains("/")) {

				errorBuffer.add(new SemanticErrorToken(this.getType(), AbstractNode.name.jsonName(), "may_not_contain_slashes").withDetail(_name));

			} else if (this instanceof Page) {

				if (!_name.equals(_name.replaceAll("[#?\\%;/]", ""))) {
					errorBuffer.add(new SemanticErrorToken(this.getType(), AbstractNode.name.jsonName(), "contains_illegal_characters").withDetail(_name));
				}
			}
		}
	}

	void syncName(final ErrorBuffer errorBuffer) throws FrameworkException {

		// sync name only
		final String name = this.getProperty(DOMNode.name);
		if (name!= null) {

			final List<DOMNode> syncedNodes = Iterables.toList(this.getSyncedNodes());
			for (final DOMNode syncedNode : syncedNodes) {

				syncedNode.setProperty(DOMNode.name, name);
			}
		}
	}

	public void setSessionAttribute(final SecurityContext securityContext, final String key, final Object value) {

		final HttpServletRequest request = securityContext.getRequest();
		if (request != null) {

			final HttpSession session = request.getSession(false);
			if (session != null) {

				session.setAttribute(ActionContext.SESSION_ATTRIBUTE_PREFIX + key, value);
			}
		}
	}

	public void removeSessionAttribute(final SecurityContext securityContext, final String key) {

		final HttpServletRequest request = securityContext.getRequest();
		if (request != null) {

			final HttpSession session = request.getSession(false);
			if (session != null) {

				session.removeAttribute(ActionContext.SESSION_ATTRIBUTE_PREFIX + key);
			}
		}
	}

	public Object getSessionAttribute(final SecurityContext securityContext, final String key) {

		final HttpServletRequest request = securityContext.getRequest();
		if (request != null) {

			final HttpSession session = request.getSession(false);
			if (session != null) {

				return session.getAttribute(ActionContext.SESSION_ATTRIBUTE_PREFIX + key);
			}
		}

		return null;
	}

	public String getTreeItemSessionIdentifier(final String target) {
		return "tree-item-" + target + "-is-open";
	}

	public static void prefetchDOMNodes(final String id) {

		TransactionCommand.getCurrentTransaction().prefetch2(

			"MATCH (n:NodeInterface { id: $id })-[r:RELOADS|CONTAINS|SUCCESS_TARGET|FAILURE_TARGET|SUCCESS_NOTIFICATION_ELEMENT|FAILURE_NOTIFICATION_ELEMENT|FLOW|INPUT_ELEMENT|PARAMETER|SYNC|TRIGGERED_BY*]->(x) WITH collect(DISTINCT x) AS nodes, collect(DISTINCT last(r)) AS rels RETURN nodes, rels",

			Set.of(
				"all/OUTGOING/CONTAINS",
				"all/OUTGOING/SUCCESS_TARGET",
				"all/OUTGOING/FAILURE_TARGET",
				"all/OUTGOING/SUCCESS_NOTIFICATION_ELEMENT",
				"all/OUTGOING/FAILURE_NOTIFICATION_ELEMENT",
				"all/OUTGOING/RELOADS",
				"all/OUTGOING/FLOW",
				"all/OUTGOING/INPUT_ELEMENT",
				"all/OUTGOING/PARAMETER",
				"all/OUTGOING/SYNC",
				"all/OUTGOING/TRIGGERED_BY",

				"all/INCOMING/CONTAINS",
				"all/INCOMING/SUCCESS_TARGET",
				"all/INCOMING/FAILURE_TARGET",
				"all/INCOMING/SUCCESS_NOTIFICATION_ELEMENT",
				"all/INCOMING/FAILURE_NOTIFICATION_ELEMENT",
				"all/INCOMING/RELOADS",
				"all/INCOMING/FLOW",
				"all/INCOMING/INPUT_ELEMENT",
				"all/INCOMING/PARAMETER",
				"all/INCOMING/TRIGGERED_BY"
			),

			Set.of(
				"all/OUTGOING/CONTAINS",
				"all/OUTGOING/SUCCESS_TARGET",
				"all/OUTGOING/FAILURE_TARGET",
				"all/OUTGOING/SUCCESS_NOTIFICATION_ELEMENT",
				"all/OUTGOING/FAILURE_NOTIFICATION_ELEMENT",
				"all/OUTGOING/RELOADS",
				"all/OUTGOING/FLOW",
				"all/OUTGOING/INPUT_ELEMENT",
				"all/OUTGOING/PARAMETER",
				"all/OUTGOING/SYNC",
				"all/OUTGOING/TRIGGERED_BY",

				"all/INCOMING/CONTAINS",
				"all/INCOMING/SUCCESS_TARGET",
				"all/INCOMING/FAILURE_TARGET",
				"all/INCOMING/SUCCESS_NOTIFICATION_ELEMENT",
				"all/INCOMING/FAILURE_NOTIFICATION_ELEMENT",
				"all/INCOMING/RELOADS",
				"all/INCOMING/FLOW",
				"all/INCOMING/INPUT_ELEMENT",
				"all/INCOMING/PARAMETER",
				"all/INCOMING/TRIGGERED_BY"
			),

			id
		);

		//TransactionCommand.getCurrentTransaction().prefetch("(n:NodeInterface { id: \"" + id + "\" })-[:CONTAINS*]->(m)",
			//Set.of("all/OUTGOING/CONTAINS"),
			//Set.of("all/INCOMING/CONTAINS")
		//);
	}

	// ----- nested classes -----
	static class TextCollector implements Predicate<Node> {

		private final StringBuilder textBuffer = new StringBuilder(200);

		@Override
		public boolean accept(final Node obj) {

			if (obj instanceof Text) {
				textBuffer.append(((Text)obj).getTextContent());
			}

			return false;
		}

		public String getText() {
			return textBuffer.toString();
		}
	}
}