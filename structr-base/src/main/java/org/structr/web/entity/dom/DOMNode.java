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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedScriptException;
import org.structr.core.entity.LinkedTreeNode;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.NodeTrait;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.event.ActionMapping;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

//public interface DOMNode extends NodeTrait, LinkedTreeNode, Node, Renderable, DOMAdoptable, DOMImportable, ContextAwareEntity {
public interface DOMNode extends NodeTrait, LinkedTreeNode, Node {

	String PAGE_CATEGORY = "Page Structure";
	String EDIT_MODE_BINDING_CATEGORY = "Edit Mode Binding";
	String EVENT_ACTION_MAPPING_CATEGORY = "Event Action Mapping";
	String QUERY_CATEGORY = "Query and Data Binding";

	// ----- error messages for DOMExceptions -----
	String NO_MODIFICATION_ALLOWED_MESSAGE = "Permission denied.";
	String INVALID_ACCESS_ERR_MESSAGE = "Permission denied.";
	String INDEX_SIZE_ERR_MESSAGE = "Index out of range.";
	String CANNOT_SPLIT_TEXT_WITHOUT_PARENT = "Cannot split text element without parent and/or owner document.";
	String WRONG_DOCUMENT_ERR_MESSAGE = "Node does not belong to this document.";
	String HIERARCHY_REQUEST_ERR_MESSAGE_SAME_NODE = "A node cannot accept itself as a child.";
	String HIERARCHY_REQUEST_ERR_MESSAGE_ANCESTOR = "A node cannot accept its own ancestor as child.";
	String HIERARCHY_REQUEST_ERR_MESSAGE_DOCUMENT = "A document may only have one html element.";
	String HIERARCHY_REQUEST_ERR_MESSAGE_ELEMENT = "A document may only accept an html element as its document element.";
	String NOT_SUPPORTED_ERR_MESSAGE = "Node type not supported.";
	String NOT_FOUND_ERR_MESSAGE = "Node is not a child.";
	String NOT_SUPPORTED_ERR_MESSAGE_IMPORT_DOC = "Document nodes cannot be imported into another document.";
	String NOT_SUPPORTED_ERR_MESSAGE_ADOPT_DOC = "Document nodes cannot be adopted by another document.";
	String NOT_SUPPORTED_ERR_MESSAGE_RENAME = "Renaming of nodes is not supported by this implementation.";
	Set<String> cloneBlacklist = new LinkedHashSet<>(Arrays.asList(new String[]{
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

	public static final Property<Iterable<ActionMapping>> reloadingActionsProperty           = new EndNodes<>("reloadingActions", DOMNodeSUCCESS_TARGETActionMapping.class).category(EVENT_ACTION_MAPPING_CATEGORY).partOfBuiltInSchema();
	public static final Property<Iterable<ActionMapping>> failureActionsProperty             = new EndNodes<>("failureActions", DOMNodeFAILURE_TARGETActionMapping.class).category(EVENT_ACTION_MAPPING_CATEGORY).partOfBuiltInSchema();
	public static final Property<Iterable<ActionMapping>> successNotificationActionsProperty = new EndNodes<>("successNotificationActions", DOMNodeSUCCESS_NOTIFICATION_ELEMENTActionMapping.class).category(EVENT_ACTION_MAPPING_CATEGORY).partOfBuiltInSchema();
	public static final Property<Iterable<ActionMapping>> failureNotificationActionsProperty = new EndNodes<>("failureNotificationActions", DOMNodeFAILURE_NOTIFICATION_ELEMENTActionMapping.class).category(EVENT_ACTION_MAPPING_CATEGORY).partOfBuiltInSchema();

	public static final Property<java.lang.Object> sortedChildrenProperty = new MethodProperty("sortedChildren").format("org.structr.web.entity.dom.DOMNode, getChildNodes").typeHint("DOMNode[]").partOfBuiltInSchema();
	public static final Property<String> childrenIdsProperty              = new CollectionIdProperty("childrenIds", childrenProperty).format("children, {},").partOfBuiltInSchema().category(PAGE_CATEGORY).partOfBuiltInSchema();
	public static final Property<String> nextSiblingIdProperty            = new EntityIdProperty("nextSiblingId", nextSiblingProperty).format("nextSibling, {},").partOfBuiltInSchema().category(PAGE_CATEGORY).partOfBuiltInSchema();
	public static final Property<String> pageIdProperty                   = new EntityIdProperty("pageId", ownerDocumentProperty).format("ownerDocument, {},").partOfBuiltInSchema().category(PAGE_CATEGORY).partOfBuiltInSchema();
	public static final Property<String> parentIdProperty                 = new EntityIdProperty("parentId", parentProperty).format("parent, {},").partOfBuiltInSchema().category(PAGE_CATEGORY).partOfBuiltInSchema();
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

	void increasePageVersion() throws FrameworkException;

	void checkName(ErrorBuffer errorBuffer);

	void syncName(ErrorBuffer errorBuffer) throws FrameworkException;

	boolean isSynced();
	boolean isSharedComponent();
	boolean hasSharedComponent();
	boolean contentEquals(final Node otherNode);
	boolean isVoidElement();
	boolean avoidWhitespace();
	boolean inTrash();
	boolean dontCache();
	boolean hideOnIndex();
	boolean hideOnDetail();
	boolean renderDetails();
	boolean displayForLocale(final RenderContext renderContext);
	boolean displayForConditions(final RenderContext renderContext);
	boolean shouldBeRendered(final RenderContext renderContext);

	int getChildPosition(final DOMNode otherNode);

	String getIdHash();
	String getIdHashOrProperty();
	String getShowConditions();
	String getHideConditions();
	String getShowForLocales();
	String getHideForLocales();
	String getContent(final RenderContext.EditMode editMode) throws FrameworkException;
	String getDataHash();
	String getDataKey();
	String getPositionPath();
	String getCssClass();
	String getContentType();
	String getEntityContextPath();

	boolean renderDeploymentExportComments(AsyncBuffer out, boolean isContentNode);

	void render(final RenderContext renderContext, final int depth) throws FrameworkException;
	void renderContent(final RenderContext renderContext, final int depth) throws FrameworkException;

	String getCypherQuery();
	String getRestQuery();
	String getFunctionQuery();

	String getPagePath();
	String getContextName();
	String getSharedComponentConfiguration();

	DOMNode getParent();
	DOMNode getSharedComponent();
	Iterable<DOMNode> getChildren();
	Iterable<DOMNode> getSyncedNodes();

	Iterable<ActionMapping> getReloadingActions();
	Iterable<ActionMapping> getFailureActions();
	Iterable<ActionMapping> getSuccessNotificationActions();
	Iterable<ActionMapping> getFailureNotificationActions();

	Page getOwnerDocumentAsSuperUser();
	Page getClosestPage();

	void setOwnerDocument(final Page page) throws FrameworkException;
	void setSharedComponent(final DOMNode sharedComponent) throws FrameworkException;

	Template getClosestTemplate(final Page page);

	void updateFromNode(final DOMNode otherNode) throws FrameworkException;
	void setVisibility(final boolean publicUsers, final boolean authenticatedUsers) throws FrameworkException;
	void renderNodeList(final SecurityContext securityContext, final RenderContext renderContext, final int depth, final String dataKey) throws FrameworkException;
	void handleNewChild(final Node newChild);
	void checkIsChild(final Node otherNode) throws DOMException;
	void checkHierarchy(Node otherNode) throws DOMException;
	void checkSameDocument(Node otherNode) throws DOMException;
	void checkWriteAccess() throws DOMException;
	void checkReadAccess() throws DOMException;
	void doAdopt(final Page page) throws DOMException;
	Node doImport(Page newPage) throws DOMException;

	void renderManagedAttributes(final AsyncBuffer out, final SecurityContext securityContext, final RenderContext renderContext) throws FrameworkException;
	void renderCustomAttributes(final AsyncBuffer out, final SecurityContext securityContext, final RenderContext renderContext) throws FrameworkException;
	void getSecurityInstructions(final Set<String> instructions);
	void getVisibilityInstructions(final Set<String> instructions);
	void getLinkableInstructions(final Set<String> instructions);
	void getContentInstructions(final Set<String> instructions);
	void renderSharedComponentConfiguration(final AsyncBuffer out, final RenderContext.EditMode editMode);

	List<RelationshipInterface> getChildRelationships();

	void doAppendChild(final DOMNode node) throws FrameworkException;
	void doRemoveChild(final DOMNode node) throws FrameworkException;

	Set<PropertyKey> getDataPropertyKeys();

	// ----- static methods -----
	static String escapeForHtml(final String raw) {
		return StringUtils.replaceEach(raw, new String[]{"&", "<", ">"}, new String[]{"&amp;", "&lt;", "&gt;"});
	}

	static String unescapeForHtml(final String raw) {
		return StringUtils.replaceEach(raw, new String[]{"&amp;", "&lt;", "&gt;"}, new String[]{"&", "<", ">"});
	}

	static String escapeForHtmlAttributes(final String raw) {
		return StringUtils.replaceEach(raw, new String[]{"&", "<", ">", "\""}, new String[]{"&amp;", "&lt;", "&gt;", "&quot;"});
	}

	static String unescapeForHtmlAttributes(final String raw) {
		return StringUtils.replaceEach(raw, new String[]{"&amp;", "&lt;", "&gt;", "&quot;"}, new String[]{"&", "<", ">", "\""});
	}

	static String objectToString(final Object source) {

		if (source != null) {
			return source.toString();
		}

		return null;
	}

	static String indent(final int depth, final RenderContext renderContext) {

		if (!renderContext.shouldIndentHtml()) {
			return "";
		}

		StringBuilder indent = new StringBuilder("\n");

		for (int d = 0; d < depth; d++) {

			indent.append("	");

		}

		return indent.toString();
	}

	static void prefetchDOMNodes(final String uuid) {
	}

	static void logScriptingError (final Logger logger, final Throwable t, String message, Object... arguments) {

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
}
