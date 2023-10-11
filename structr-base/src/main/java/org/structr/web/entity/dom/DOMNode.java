/*
 * Copyright (C) 2010-2023 Structr GmbH
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
import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonReferenceType;
import org.structr.api.schema.JsonSchema;
import org.structr.api.service.LicenseManager;
import org.structr.api.util.Iterables;
import org.structr.common.*;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.SemanticErrorToken;
import org.structr.common.error.UnlicensedScriptException;
import org.structr.common.event.RuntimeEventLog;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.datasources.DataSources;
import org.structr.core.datasources.GraphDataSource;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.LinkedTreeNode;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Security;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.*;
import org.structr.core.script.Scripting;
import org.structr.schema.SchemaService;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.RenderContext;
import org.structr.web.common.RenderContext.EditMode;
import org.structr.web.common.StringRenderBuffer;
import org.structr.web.entity.LinkSource;
import org.structr.web.entity.Linkable;
import org.structr.web.entity.Renderable;
import org.structr.web.property.CustomHtmlAttributeProperty;
import org.structr.web.property.MethodProperty;
import org.structr.websocket.command.CreateComponentCommand;
import org.w3c.dom.*;

import java.net.URI;
import java.util.*;

/**
 * Combines AbstractNode and org.w3c.dom.Node.
 */
public interface DOMNode extends NodeInterface, Node, Renderable, DOMAdoptable, DOMImportable, LinkedTreeNode<DOMNode>, ContextAwareEntity {

	static final Set<String> DataAttributeOutputBlacklist = Set.of("data-structr-manual-reload-target");

	static class Impl { static {

		final JsonSchema schema    = SchemaService.getDynamicSchema();
		final JsonObjectType page  = (JsonObjectType)schema.getType("Page");
		final JsonObjectType type  = schema.addType("DOMNode");

		type.setIsAbstract();
		type.setImplements(URI.create("https://structr.org/v1.1/definitions/DOMNode"));
		type.setExtends(URI.create("https://structr.org/v1.1/definitions/LinkedTreeNodeImpl?typeParameters=org.structr.web.entity.dom.DOMNode"));
		type.setCategory("html");

		// DOMNodes can be targets of a reload or redirect after a success or failure of actions defined by ActionMapping nodes
		type.addViewProperty(PropertyView.Ui, "reloadingActions");
		type.addViewProperty(PropertyView.Ui, "failureActions");
		type.addViewProperty(PropertyView.Ui, "successNotificationActions");
		type.addViewProperty(PropertyView.Ui, "failureNotificationActions");

		type.addStringProperty("dataKey").setIndexed(true).setCategory(QUERY_CATEGORY);
		type.addStringProperty("cypherQuery").setCategory(QUERY_CATEGORY);
		type.addStringProperty("xpathQuery").setCategory(QUERY_CATEGORY);
		type.addStringProperty("restQuery").setCategory(QUERY_CATEGORY);
		type.addStringProperty("functionQuery").setCategory(QUERY_CATEGORY);

		type.addStringProperty("showForLocales").setIndexed(true).setCategory(VISIBILITY_CATEGORY);
		type.addStringProperty("hideForLocales").setIndexed(true).setCategory(VISIBILITY_CATEGORY);
		type.addStringProperty("showConditions").setIndexed(true).setCategory(VISIBILITY_CATEGORY).setHint("Conditions which have to be met in order for the element to be shown.<br><br>This is an 'auto-script' environment, meaning that the text is automatically surrounded with ${}");
		type.addStringProperty("hideConditions").setIndexed(true).setCategory(VISIBILITY_CATEGORY).setHint("Conditions which have to be met in order for the element to be hidden.<br><br>This is an 'auto-script' environment, meaning that the text is automatically surrounded with ${}");

		type.addStringProperty("sharedComponentConfiguration").setFormat("multi-line").setCategory(PAGE_CATEGORY).setHint("The contents of this field will be evaluated before rendering this component. This is usually used to customize shared components to make them more flexible.<br><br>This is an 'auto-script' environment, meaning that the text is automatically surrounded with ${}");

		type.addStringProperty("data-structr-id").setHint("Set to ${current.id} most of the time").setCategory(PAGE_CATEGORY);
		type.addStringProperty("data-structr-hash").setCategory(PAGE_CATEGORY);

		type.addBooleanProperty("renderDetails").setCategory(QUERY_CATEGORY);
		type.addBooleanProperty("hideOnIndex").setCategory(QUERY_CATEGORY);
		type.addBooleanProperty("hideOnDetail").setCategory(QUERY_CATEGORY);
		type.addBooleanProperty("dontCache").setDefaultValue("false");
		type.addBooleanProperty("isDOMNode").setReadOnly(true).addTransformer(ConstantBooleanTrue.class.getName()).setCategory(PAGE_CATEGORY);

		type.addIntegerProperty("domSortPosition").setCategory(PAGE_CATEGORY);

		type.addPropertyGetter("restQuery", String.class);
		type.addPropertyGetter("cypherQuery", String.class);
		type.addPropertyGetter("xpathQuery", String.class);
		type.addPropertyGetter("functionQuery", String.class);
		type.addPropertyGetter("dataKey", String.class);
		type.addPropertyGetter("showConditions", String.class);
		type.addPropertyGetter("hideConditions", String.class);

		type.addPropertyGetter("parent", DOMNode.class);
		type.addPropertyGetter("children", Iterable.class);
		type.addPropertyGetter("nextSibling", DOMNode.class);
		type.addPropertyGetter("previousSibling", DOMNode.class);
		type.addPropertyGetter("syncedNodes", Iterable.class);
		type.addPropertyGetter("ownerDocument", Page.class);
		type.addPropertyGetter("sharedComponent", DOMNode.class);
		type.addPropertyGetter("sharedComponentConfiguration", String.class);

		type.addCustomProperty("sortedChildren", MethodProperty.class.getName()).setTypeHint("DOMNode[]").setFormat(DOMNode.class.getName() + ", getChildNodes");

		type.overrideMethod("onCreation",                  true,  DOMNode.class.getName() + ".onCreation(this, arg0, arg1);");
		type.overrideMethod("onModification",              true,  DOMNode.class.getName() + ".onModification(this, arg0, arg1, arg2);");

		type.overrideMethod("getPositionProperty",         false, "return DOMNodeCONTAINSDOMNode.positionProperty;");

		type.overrideMethod("getSiblingLinkType",          false, "return DOMNodeCONTAINS_NEXT_SIBLINGDOMNode.class;");
		type.overrideMethod("getChildLinkType",            false, "return DOMNodeCONTAINSDOMNode.class;");
		type.overrideMethod("getChildRelationships",       false, "return treeGetChildRelationships();");
		type.overrideMethod("renderNodeList",              false, DOMNode.class.getName() + ".renderNodeList(this, arg0, arg1, arg2, arg3);");

		type.overrideMethod("setVisibility",               false, "setProperty(visibleToPublicUsers, arg0); setProperty(visibleToAuthenticatedUsers, arg1);");

		type.overrideMethod("getClosestPage",              false, "return " + DOMNode.class.getName() + ".getClosestPage(this);");
		type.overrideMethod("getClosestTemplate",          false, "return " + DOMNode.class.getName() + ".getClosestTemplate(this, arg0);");
		type.overrideMethod("getContent",                  false, "return " + DOMNode.class.getName() + ".getContent(this, arg0);");
		type.overrideMethod("getOwnerDocumentAsSuperUser", false, "return " + DOMNode.class.getName() + ".getOwnerDocumentAsSuperUser(this);");
		type.overrideMethod("isSharedComponent",           false, "return " + DOMNode.class.getName() + ".isSharedComponent(this);");

		type.overrideMethod("getChildPosition",            false, "return treeGetChildPosition(arg0);");
		type.overrideMethod("getPositionPath",             false, "return " + DOMNode.class.getName() + ".getPositionPath(this);");

		type.overrideMethod("getIdHash",                   false, "return getUuid();");
		type.overrideMethod("getIdHashOrProperty",         false, "return " + DOMNode.class.getName() + ".getIdHashOrProperty(this);");
		type.overrideMethod("getDataHash",                 false, "return getProperty(datastructrhashProperty);");

		type.overrideMethod("avoidWhitespace",             false, "return false;");
		type.overrideMethod("isVoidElement",               false, "return false;");

		type.overrideMethod("inTrash",                     false, "return getParent() == null && getOwnerDocumentAsSuperUser() == null;");
		type.overrideMethod("dontCache",                   false, "return getProperty(dontCacheProperty);");
		type.overrideMethod("renderDetails",               false, "return getProperty(renderDetailsProperty);");
		type.overrideMethod("hideOnIndex",                 false, "return getProperty(hideOnIndexProperty);");
		type.overrideMethod("hideOnDetail",                false, "return getProperty(hideOnDetailProperty);");
		type.overrideMethod("isSynced",                    false, "return Iterables.count(getSyncedNodes()) > 0 || getSharedComponent() != null;");

		// ----- interface org.w3c.dom.Node -----
		type.overrideMethod("setUserData",                         false, "return null;");
		type.overrideMethod("getUserData",                         false, "return null;");
		type.overrideMethod("getFeature",                          false, "return null;");
		type.overrideMethod("isEqualNode",                         false, "return equals(arg0);");
		type.overrideMethod("lookupNamespaceURI",                  false, "return null;");
		type.overrideMethod("lookupPrefix",                        false, "return null;");
		type.overrideMethod("compareDocumentPosition",             false, "return 0;");
		type.overrideMethod("isDefaultNamespace",                  false, "return true;");
		type.overrideMethod("isSameNode",                          false, "return " + DOMNode.class.getName() + ".isSameNode(this, arg0);");
		type.overrideMethod("isSupported",                         false, "return false;");
		type.overrideMethod("getPrefix",                           false, "return null;");
		type.overrideMethod("setPrefix",                           false, "");
		type.overrideMethod("getNamespaceURI",                     false, "return null;");
		type.overrideMethod("getBaseURI",                          false, "return null;");
		type.overrideMethod("cloneNode",                           false, "return " + DOMNode.class.getName() + ".cloneNode(this, arg0);");
		type.overrideMethod("setTextContent",                      false, "");
		type.overrideMethod("getTextContent",                      false, "");

		// DOM operations
		type.overrideMethod("normalize",                           false, DOMNode.class.getName() + ".normalize(this);");
		type.overrideMethod("checkHierarchy",                      false, DOMNode.class.getName() + ".checkHierarchy(this, arg0);");
		type.overrideMethod("checkSameDocument",                   false, DOMNode.class.getName() + ".checkSameDocument(this, arg0);");
		type.overrideMethod("checkWriteAccess",                    false, DOMNode.class.getName() + ".checkWriteAccess(this);");
		type.overrideMethod("checkReadAccess",                     false, DOMNode.class.getName() + ".checkReadAccess(this);");
		type.overrideMethod("checkIsChild",                        false, DOMNode.class.getName() + ".checkIsChild(this, arg0);");
		type.overrideMethod("handleNewChild",                      false, DOMNode.class.getName() + ".handleNewChild(this, arg0);");
		type.overrideMethod("insertBefore",                        false, "return " + DOMNode.class.getName() + ".insertBefore(this, arg0, arg1);");
		type.overrideMethod("replaceChild",                        false, "return " + DOMNode.class.getName() + ".replaceChild(this, arg0, arg1);");
		type.overrideMethod("removeChild",                         false, "return " + DOMNode.class.getName() + ".removeChild(this, arg0);");
		type.overrideMethod("appendChild",                         false, "return " + DOMNode.class.getName() + ".appendChild(this, arg0);");
		type.overrideMethod("hasChildNodes",                       false, "return getProperty(childrenProperty).iterator().hasNext();");

		type.overrideMethod("displayForLocale",                    false, "return " + DOMNode.class.getName() + ".displayForLocale(this, arg0);");
		type.overrideMethod("displayForConditions",                false, "return " + DOMNode.class.getName() + ".displayForConditions(this, arg0);");
		type.overrideMethod("shouldBeRendered",                    false, "return " + DOMNode.class.getName() + ".shouldBeRendered(this, arg0);");

		// Renderable
		type.overrideMethod("render",                              false, DOMNode.class.getName() + ".render(this, arg0, arg1);");

		// DOMAdoptable
		type.overrideMethod("doAdopt",                             false, "return " + DOMNode.class.getName() + ".doAdopt(this, arg0);");

		// LinkedTreeNode
		type.overrideMethod("doAppendChild",                       false, "checkWriteAccess(); treeAppendChild(arg0);");
		type.overrideMethod("doRemoveChild",                       false, "checkWriteAccess(); treeRemoveChild(arg0);");
		type.overrideMethod("getFirstChild",                       false, "checkReadAccess(); return (DOMNode)treeGetFirstChild();");
		type.overrideMethod("getLastChild",                        false, "checkReadAccess(); return (DOMNode)treeGetLastChild();");
		type.overrideMethod("getChildNodes",                       false, "checkReadAccess(); return new " + DOMNodeList.class.getName() + "(treeGetChildren());");
		type.overrideMethod("getParentNode",                       false, "checkReadAccess(); return getParent();");

		type.overrideMethod("renderCustomAttributes",              false, DOMNode.class.getName() + ".renderCustomAttributes(this, arg0, arg1, arg2);");
		type.overrideMethod("getSecurityInstructions",             false, DOMNode.class.getName() + ".getSecurityInstructions(this, arg0);");
		type.overrideMethod("getVisibilityInstructions",           false, DOMNode.class.getName() + ".getVisibilityInstructions(this, arg0);");
		type.overrideMethod("getLinkableInstructions",             false, DOMNode.class.getName() + ".getLinkableInstructions(this, arg0);");
		type.overrideMethod("getContentInstructions",              false, DOMNode.class.getName() + ".getContentInstructions(this, arg0);");
		type.overrideMethod("renderSharedComponentConfiguration",  false, DOMNode.class.getName() + ".renderSharedComponentConfiguration(this, arg0, arg1);");
		type.overrideMethod("getPagePath",                         false, "return " + DOMNode.class.getName() + ".getPagePath(this);");
		type.overrideMethod("getDataPropertyKeys",                 false, "return " + DOMNode.class.getName() + ".getDataPropertyKeys(this);");
		type.overrideMethod("getAllChildNodes",                    false, "return " + DOMNode.class.getName() + ".getAllChildNodes(this);");

		// ContextAwareEntity
		type.overrideMethod("getEntityContextPath",                false, "return getPagePath();");

		type.addMethod("setOwnerDocument")
			.setSource("setProperty(ownerDocumentProperty, (Page)ownerDocument);")
			.addException(FrameworkException.class.getName())
			.addParameter("ownerDocument", "org.structr.web.entity.dom.Page");

		type.addMethod("setSharedComponent")
			.setSource("setProperty(sharedComponentProperty, (DOMNode)sharedComponent);")
			.addException(FrameworkException.class.getName())
			.addParameter("sharedComponent", "org.structr.web.entity.dom.DOMNode");

		final JsonReferenceType sibling   = type.relate(type,                                                   "CONTAINS_NEXT_SIBLING", Cardinality.OneToOne,  "previousSibling",  "nextSibling");
		final JsonReferenceType parent    = type.relate(type,                                                   "CONTAINS",              Cardinality.OneToMany, "parent",           "children");
		final JsonReferenceType synced    = type.relate(type,                                                   "SYNC",                  Cardinality.OneToMany, "sharedComponent",  "syncedNodes");
		final JsonReferenceType owner     = type.relate(page,                                                   "PAGE",                  Cardinality.ManyToOne, "elements",         "ownerDocument");

		type.addIdReferenceProperty("parentId",          parent.getSourceProperty()).setCategory(PAGE_CATEGORY);
		type.addIdReferenceProperty("childrenIds",       parent.getTargetProperty()).setCategory(PAGE_CATEGORY);
		type.addIdReferenceProperty("pageId",            owner.getTargetProperty()).setCategory(PAGE_CATEGORY);
		type.addIdReferenceProperty("nextSiblingId",     sibling.getTargetProperty()).setCategory(PAGE_CATEGORY);
		type.addIdReferenceProperty("sharedComponentId", synced.getSourceProperty());
		type.addIdReferenceProperty("syncedNodesIds",    synced.getTargetProperty());

		// sort position of children in page
		parent.addIntegerProperty("position");

		// category and hints
		sibling.getSourceProperty().setCategory(PAGE_CATEGORY);
		sibling.getTargetProperty().setCategory(PAGE_CATEGORY);
		parent.getSourceProperty().setCategory(PAGE_CATEGORY);
		parent.getTargetProperty().setCategory(PAGE_CATEGORY);
		synced.getSourceProperty().setCategory(PAGE_CATEGORY);
		synced.getTargetProperty().setCategory(PAGE_CATEGORY);

		final LicenseManager licenseManager = Services.getInstance().getLicenseManager();
		if (licenseManager == null || licenseManager.isModuleLicensed("api-builder")) {

			try {

				final String name = "org.structr.flow.impl.rels.DOMNodeFLOWFlowContainer";
				Class.forName(name);

				// register flow only if the above class exists
				type.addCustomProperty("flow", EndNode.class.getName()).setFormat(name);

			} catch (Throwable ignore) {}
		}
	}}

	static final String PAGE_CATEGORY              = "Page Structure";
	static final String EDIT_MODE_BINDING_CATEGORY = "Edit Mode Binding";
	static final String QUERY_CATEGORY             = "Query and Data Binding";

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
		"dataKey", "restQuery", "cypherQuery", "xpathQuery", "functionQuery", "selectedValues", "flow", "hideOnIndex", "hideOnDetail", "showForLocales", "hideForLocales", "showConditions", "hideConditions"
	};

	boolean isSynced();
	boolean isSharedComponent();
	boolean contentEquals(final DOMNode otherNode);
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
	String getContent(final RenderContext.EditMode editMode) throws FrameworkException;
	String getDataHash();
	String getDataKey();
	String getPositionPath();

	default String getCssClass() {
		return null;
	}

	default void renderManagedAttributes(final AsyncBuffer out, final SecurityContext securityContext, final RenderContext renderContext) throws FrameworkException {
	}

	String getCypherQuery();
	String getRestQuery();
	String getXpathQuery();
	String getFunctionQuery();

	String getPagePath();
	String getContextName();
	String getSharedComponentConfiguration();

	@Override
	DOMNode getPreviousSibling();

	@Override
	DOMNode getNextSibling();

	DOMNode getParent();
	DOMNode getSharedComponent();
	Iterable<DOMNode> getChildren();
	Iterable<DOMNode> getSyncedNodes();

	@Override
	Page getOwnerDocument();
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

	void renderCustomAttributes(final AsyncBuffer out, final SecurityContext securityContext, final RenderContext renderContext) throws FrameworkException;
	void getSecurityInstructions(final Set<String> instructions);
	void getVisibilityInstructions(final Set<String> instructions);
	void getLinkableInstructions(final Set<String> instructions);
	void getContentInstructions(final Set<String> instructions);
	void renderSharedComponentConfiguration(final AsyncBuffer out, final EditMode editMode);

	List<RelationshipInterface> getChildRelationships();

	void doAppendChild(final DOMNode node) throws FrameworkException;
	void doRemoveChild(final DOMNode node) throws FrameworkException;

	Set<PropertyKey> getDataPropertyKeys();

	// ----- public default methods -----
	@Override
	default public void visitForUsage(final Map<String, Object> data) {

		LinkedTreeNode.super.visitForUsage(data);

		final Page page = getOwnerDocument();
		if (page != null) {

			data.put("page", page.getName());
		}

		data.put("path", getPagePath());
	}

	@Override
	default boolean isFrontendNode() {
		return true;
	}

	// ----- static methods -----
	static void onCreation(final DOMNode thisNode, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		DOMNode.checkName(thisNode, errorBuffer);
		DOMNode.syncName(thisNode, errorBuffer);
	}

	static void onModification(final DOMNode thisNode, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		DOMNode.increasePageVersion(thisNode);
		DOMNode.checkName(thisNode, errorBuffer);
		DOMNode.syncName(thisNode, errorBuffer);

		final String uuid = thisNode.getUuid();
		if (uuid != null) {

			// acknowledge all events for this node when it is modified
			RuntimeEventLog.getEvents(e -> uuid.equals(e.getData().get("id"))).stream().forEach(e -> e.acknowledge());
		}
	}

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

	/**
	 * Recursively clone given node, all its direct children and connect the cloned child nodes to the clone parent node.
	 *
	 * @param securityContext
	 * @param nodeToClone
	 * @return
	 */
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

	public static void renderNodeList(final DOMNode node, final SecurityContext securityContext, final RenderContext renderContext, final int depth, final String dataKey) throws FrameworkException {

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

					node.renderContent(renderContext, depth + 1);
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

	public static Template getClosestTemplate(final DOMNode thisNode, final Page page) {

		DOMNode node = thisNode;

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

	public static Page getClosestPage(final DOMNode thisNode) {

		DOMNode node = thisNode;

		while (node != null) {

			if (node instanceof Page) {

				return (Page)node;
			}

			node = (DOMNode)node.getParentNode();

		}

		return null;
	}

	public static String getPositionPath(final DOMNode thisNode) {

		String path = "";

		DOMNode currentNode = thisNode;
		while (currentNode.getParentNode() != null) {

			DOMNode parentNode = (DOMNode)currentNode.getParentNode();

			path = "/" + parentNode.getChildPosition(currentNode) + path;

			currentNode = parentNode;

		}

		return path;

	}

	public static String getContent(final DOMNode thisNode, final RenderContext.EditMode editMode) throws FrameworkException {

		final RenderContext ctx         = new RenderContext(thisNode.getSecurityContext(), null, null, editMode);
		final StringRenderBuffer buffer = new StringRenderBuffer();

		ctx.setBuffer(buffer);
		thisNode.render(ctx, 0);

		// extract source
		return buffer.getBuffer().toString();
	}

	public static String getIdHashOrProperty(final DOMNode thisNode) {

		String idHash = thisNode.getDataHash();
		if (idHash == null) {

			idHash = thisNode.getIdHash();
		}

		return idHash;
	}

	public static Page getOwnerDocumentAsSuperUser(final DOMNode thisNode) {

		final RelationshipInterface ownership = thisNode.getOutgoingRelationshipAsSuperUser(StructrApp.getConfiguration().getRelationshipEntityClass("DOMNodePAGEPage"));
		if (ownership != null) {

			return (Page)ownership.getTargetNode();
		}

		return null;
	}

	public static boolean isSharedComponent(final DOMNode thisNode) {

		final Document _ownerDocument = thisNode.getOwnerDocumentAsSuperUser();
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

	public static boolean isSameNode(final DOMNode thisNode, Node node) {

		if (node != null && node instanceof DOMNode) {

			String otherId = ((DOMNode)node).getProperty(GraphObject.id);
			String ourId   = thisNode.getProperty(GraphObject.id);

			if (ourId != null && otherId != null && ourId.equals(otherId)) {
				return true;
			}
		}

		return false;
	}

	public static Node cloneNode(final DOMNode thisNode, boolean deep) {

		final SecurityContext securityContext = thisNode.getSecurityContext();

		if (deep) {

			return cloneAndAppendChildren(securityContext, thisNode);

		} else {

			final PropertyMap properties = new PropertyMap();

			for (final PropertyKey key : thisNode.getPropertyKeys(PropertyView.Ui)) {

				// skip blacklisted properties
				if (cloneBlacklist.contains(key.jsonName())) {
					continue;
				}


				if (!key.isUnvalidated()) {
					properties.put(key, thisNode.getProperty(key));
				}
			}

			// htmlView is necessary for the cloning of DOM nodes - otherwise some properties won't be cloned
			for (final PropertyKey key : thisNode.getPropertyKeys(PropertyView.Html)) {

				// skip blacklisted properties
				if (cloneBlacklist.contains(key.jsonName())) {
					continue;
				}

				if (!key.isUnvalidated()) {
					properties.put(key, thisNode.getProperty(key));
				}
			}

			// also clone data-* attributes
			for (final PropertyKey key : thisNode.getDataPropertyKeys()) {

				// skip blacklisted properties
				if (cloneBlacklist.contains(key.jsonName())) {
					continue;
				}

				if (!key.isUnvalidated()) {
					properties.put(key, thisNode.getProperty(key));
				}
			}

			if (thisNode instanceof LinkSource) {

				final LinkSource linkSourceElement = (LinkSource)thisNode;

				properties.put(StructrApp.key(LinkSource.class, "linkable"), linkSourceElement.getLinkable());
			}

			final App app = StructrApp.getInstance(securityContext);

			try {

				final DOMNode clone = app.create(thisNode.getClass(), properties);

				// for clone, always copy permissions
				thisNode.copyPermissionsTo(securityContext, clone, true);

				return clone;

			} catch (FrameworkException ex) {

				throw new DOMException(DOMException.INVALID_STATE_ERR, ex.toString());
			}
		}
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

	static String getTextContent(final DOMNode thisNode) throws DOMException {

		final DOMNodeList results         = new DOMNodeList();
		final TextCollector textCollector = new TextCollector();

		DOMNode.collectNodesByPredicate(thisNode.getSecurityContext(), thisNode, results, textCollector, 0, false);

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

	public static void normalize(final DOMNode thisNode) {

		final Document document = thisNode.getOwnerDocument();
		if (document != null) {

			// merge adjacent text nodes until there is only one left
			Node child = thisNode.getFirstChild();
			while (child != null) {

				if (child instanceof Text) {

					Node next = child.getNextSibling();
					if (next != null && next instanceof Text) {

						String text1 = child.getNodeValue();
						String text2 = next.getNodeValue();

						// create new text node
						final Text newText = document.createTextNode(text1.concat(text2));

						thisNode.removeChild(child);
						thisNode.insertBefore(newText, next);
						thisNode.removeChild(next);

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
			if (thisNode.hasChildNodes()) {

				Node currentChild = thisNode.getFirstChild();
				while (currentChild != null) {

					currentChild.normalize();
					currentChild = currentChild.getNextSibling();
				}
			}
		}
	}

	static Node appendChild(final DOMNode thisNode, final Node newChild) throws DOMException {

		thisNode.checkWriteAccess();
		thisNode.checkSameDocument(newChild);
		thisNode.checkHierarchy(newChild);

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
					thisNode.appendChild(currentChild);

					// next
					currentChild = savedNextChild;
				}

			} else {

				final Node _parent = newChild.getParentNode();

				if (_parent != null && _parent instanceof DOMNode) {
					_parent.removeChild(newChild);
				}

				thisNode.doAppendChild((DOMNode)newChild);

				// allow parent to set properties in new child
				thisNode.handleNewChild(newChild);
			}

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());
		}

		return newChild;
	}

	public static Node removeChild(final DOMNode thisNode, final Node node) throws DOMException {

		thisNode.checkWriteAccess();
		thisNode.checkSameDocument(node);
		thisNode.checkIsChild(node);

		try {

			thisNode.doRemoveChild((DOMNode)node);

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());
		}

		return node;
	}

	static void checkIsChild(final DOMNode thisNode, final Node otherNode) throws DOMException {

		if (otherNode instanceof DOMNode) {

			Node _parent = otherNode.getParentNode();

			if (!thisNode.isSameNode(_parent)) {

				throw new DOMException(DOMException.NOT_FOUND_ERR, NOT_FOUND_ERR_MESSAGE);
			}

			// validation successful
			return;
		}

		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, NOT_SUPPORTED_ERR_MESSAGE);
	}

	static void checkHierarchy(final DOMNode thisNode, final Node otherNode) throws DOMException {

		// we can only check DOMNodes
		if (otherNode instanceof DOMNode) {

			// verify that the other node is not this node
			if (thisNode.isSameNode(otherNode)) {
				throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, HIERARCHY_REQUEST_ERR_MESSAGE_SAME_NODE);
			}

			// verify that otherNode is not one of the
			// the ancestors of this node
			// (prevent circular relationships)
			Node _parent = thisNode.getParentNode();
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

	static void checkSameDocument(final DOMNode thisNode, final Node otherNode) throws DOMException {

		Document doc = thisNode.getOwnerDocument();

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

	static void checkWriteAccess(final DOMNode thisNode) throws DOMException {

		if (!thisNode.isGranted(Permission.write, thisNode.getSecurityContext())) {

			throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, NO_MODIFICATION_ALLOWED_MESSAGE);
		}
	}

	static void checkReadAccess(final DOMNode thisNode) throws DOMException {

		final SecurityContext securityContext = thisNode.getSecurityContext();

		if (securityContext.isVisible(thisNode) || thisNode.isGranted(Permission.read, securityContext)) {
			return;
		}

		throw new DOMException(DOMException.INVALID_ACCESS_ERR, INVALID_ACCESS_ERR_MESSAGE);
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

	static boolean displayForLocale(final DOMNode thisNode, final RenderContext renderContext) {

		String localeString = renderContext.getLocale().toString();

		String show = thisNode.getProperty(StructrApp.key(DOMNode.class, "showForLocales"));
		String hide = thisNode.getProperty(StructrApp.key(DOMNode.class, "hideForLocales"));

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

	static boolean displayForConditions(final DOMNode thisNode, final RenderContext renderContext)  {

		String _showConditions = thisNode.getProperty(StructrApp.key(DOMNode.class, "showConditions"));
		String _hideConditions = thisNode.getProperty(StructrApp.key(DOMNode.class, "hideConditions"));

		// If both fields are empty, render node
		if (StringUtils.isBlank(_hideConditions) && StringUtils.isBlank(_showConditions)) {
			return true;
		}

		try {
			// If hide conditions evaluates to "true", don't render
			if (StringUtils.isNotBlank(_hideConditions) && Boolean.TRUE.equals(Scripting.evaluate(renderContext, thisNode, "${".concat(_hideConditions.trim()).concat("}"), "hideConditions", thisNode.getUuid()))) {
				return false;
			}

		} catch (UnlicensedScriptException | FrameworkException ex) {

			final Logger logger        = LoggerFactory.getLogger(DOMNode.class);
			final boolean isShadowPage = DOMNode.isSharedComponent(thisNode);

			if (!isShadowPage) {

				final DOMNode ownerDocument = thisNode.getOwnerDocumentAsSuperUser();
				DOMNode.logScriptingError(logger, ex, "Error while evaluating hide condition '{}' in page {}[{}], DOMNode[{}]", _hideConditions, ownerDocument.getProperty(AbstractNode.name), ownerDocument.getProperty(AbstractNode.id), thisNode.getUuid());

			} else {

				DOMNode.logScriptingError(logger, ex, "Error while evaluating hide condition '{}' in shared component, DOMNode[{}]", _hideConditions, thisNode.getUuid());
			}
		}

		try {
			// If show conditions evaluates to "false", don't render
			if (StringUtils.isNotBlank(_showConditions) && Boolean.FALSE.equals(Scripting.evaluate(renderContext, thisNode, "${".concat(_showConditions.trim()).concat("}"), "showConditions", thisNode.getUuid()))) {
				return false;
			}

		} catch (UnlicensedScriptException | FrameworkException ex) {

			final Logger logger        = LoggerFactory.getLogger(DOMNode.class);
			final boolean isShadowPage = DOMNode.isSharedComponent(thisNode);

			if (!isShadowPage) {

				final DOMNode ownerDocument = thisNode.getOwnerDocumentAsSuperUser();
				DOMNode.logScriptingError(logger, ex, "Error while evaluating show condition '{}' in page {}[{}], DOMNode[{}]", _showConditions, ownerDocument.getProperty(AbstractNode.name), ownerDocument.getProperty(AbstractNode.id), thisNode.getUuid());

			} else {

				DOMNode.logScriptingError(logger, ex, "Error while evaluating show condition '{}' in shared component, DOMNode[{}]", _showConditions, thisNode.getUuid());
			}
		}

		return true;
	}

	static void logScriptingError (final Logger logger, final Throwable t, String message,  Object... arguments) {

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

	static boolean shouldBeRendered(final DOMNode thisNode, final RenderContext renderContext) {

		// In raw, widget or deployment mode, render everything
		EditMode editMode = renderContext.getEditMode(renderContext.getSecurityContext().getUser(false));
		if (EditMode.DEPLOYMENT.equals(editMode) || EditMode.RAW.equals(editMode) || EditMode.WIDGET.equals(editMode)) {
			return true;
		}

		if (thisNode.isHidden() || !thisNode.displayForLocale(renderContext) || !thisNode.displayForConditions(renderContext)) {
			return false;
		}

		return true;
	}

	static boolean renderDeploymentExportComments(final DOMNode thisNode, final AsyncBuffer out, final boolean isContentNode) {

		final Set<String> instructions = new LinkedHashSet<>();

		thisNode.getVisibilityInstructions(instructions);
		thisNode.getLinkableInstructions(instructions);
		thisNode.getSecurityInstructions(instructions);

		if (thisNode.isHidden()) {
			instructions.add("@structr:hidden");
		}

		if (isContentNode) {

			// special rules apply for content nodes: since we can not store
			// structr-specific properties in the attributes of the element,
			// we need to encode those attributes in instructions.
			thisNode.getContentInstructions(instructions);
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

	static void renderSharedComponentConfiguration(final DOMNode thisNode, final AsyncBuffer out, final EditMode editMode) {

		if (EditMode.DEPLOYMENT.equals(editMode)) {

			final String configuration = thisNode.getProperty(StructrApp.key(DOMNode.class, "sharedComponentConfiguration"));
			if (StringUtils.isNotBlank(configuration)) {

				out.append(" data-structr-meta-shared-component-configuration=\"");
				out.append(escapeForHtmlAttributes(configuration));
				out.append("\"");
			}
		}
	}

	static void getContentInstructions(final DOMNode thisNode, final Set<String> instructions) {

		final String _contentType = thisNode.getProperty(StructrApp.key(Content.class, "contentType"));
		if (_contentType != null) {

			instructions.add("@structr:content(" + escapeForHtmlAttributes(_contentType) + ")");
		}

		if (!thisNode.getType().equals("Template")) {

			final String _name = thisNode.getProperty(AbstractNode.name);
			if (StringUtils.isNotEmpty(_name)) {

				instructions.add("@structr:name(" + escapeForHtmlAttributes(_name) + ")");
			}

			final String _showConditions = thisNode.getShowConditions();
			if (StringUtils.isNotEmpty(_showConditions)) {

				instructions.add("@structr:show(" + escapeForHtmlAttributes(_showConditions) + ")");
			}

			final String _hideConditions = thisNode.getHideConditions();
			if (StringUtils.isNotEmpty(_hideConditions)) {

				instructions.add("@structr:hide(" + escapeForHtmlAttributes(_hideConditions) + ")");
			}

			final String _showForLocales = thisNode.getProperty(StructrApp.key(DOMNode.class, "showForLocales"));
			if (StringUtils.isNotEmpty(_showForLocales)) {

				instructions.add("@structr:show-for-locales(" + escapeForHtmlAttributes(_showForLocales) + ")");
			}

			final String _hideForLocales = thisNode.getProperty(StructrApp.key(DOMNode.class, "hideForLocales"));
			if (StringUtils.isNotEmpty(_hideForLocales)) {

				instructions.add("@structr:hide-for-locales(" + escapeForHtmlAttributes(_hideForLocales) + ")");
			}
		}
	}

	static void getLinkableInstructions(final DOMNode thisNode, final Set<String> instructions) {

		if (thisNode instanceof LinkSource) {

			final LinkSource linkSourceElement = (LinkSource)thisNode;
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

	static void getVisibilityInstructions(final DOMNode thisNode, final Set<String> instructions) {

		final DOMNode _parentNode       = (DOMNode)thisNode.getParent();

		final boolean elementPublic     = thisNode.isVisibleToPublicUsers();
		final boolean elementProtected  = thisNode.isVisibleToAuthenticatedUsers();
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

	static void getSecurityInstructions(final DOMNode thisNode, final Set<String> instructions) {

		final Principal _owner = thisNode.getOwnerNode();
		if (_owner != null) {

			instructions.add("@structr:owner(" + _owner.getProperty(AbstractNode.name) + ")");
		}

		thisNode.getSecurityRelationships().stream().filter(Objects::nonNull).sorted(Comparator.comparing(security -> security.getSourceNode().getProperty(AbstractNode.name))).forEach(security -> {

			final Principal grantee = security.getSourceNode();
			final Set<String> perms = security.getPermissions();
			final StringBuilder shortPerms = new StringBuilder();

			// first character only
			for (final String perm : perms) {
				if (perm.length() > 0) {
					shortPerms.append(perm.substring(0, 1));
				}
			}

			if (shortPerms.length() > 0) {
				// ignore SECURITY-relationships without permissions
				instructions.add("@structr:grant(" + grantee.getProperty(AbstractNode.name) + "," + shortPerms.toString() + ")");
			}
		});
	}

	static void renderCustomAttributes(final DOMNode thisNode, final AsyncBuffer out, final SecurityContext securityContext, final RenderContext renderContext) throws FrameworkException {

		final EditMode editMode = renderContext.getEditMode(securityContext.getUser(false));

		Set<PropertyKey> dataAttributes = thisNode.getDataPropertyKeys();

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

				final Object obj = thisNode.getProperty(key);
				if (obj != null) {

					value = obj.toString();
				}

			} else {

				value = thisNode.getPropertyWithVariableReplacement(renderContext, key);
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
				final String name = thisNode.getProperty(AbstractNode.name);
				if (name != null) {

					out.append(" data-structr-meta-name=\"").append(escapeForHtmlAttributes(name)).append("\"");
				}

				final Object actionElement = thisNode.getProperty(StructrApp.key(DOMElement.class, "actionElement", false));
				if (actionElement != null) {
					out.append(" data-structr-meta-action-element=\"").append(((DOMElement) actionElement).getUuid()).append("\"");
				}

//				final PropertyKey<Iterable<DOMElement>> inputsKey = StructrApp.key(DOMElement.class, "inputs", false);
//				final List<DOMElement> inputs                     = Iterables.toList(thisNode.getProperty(inputsKey));
//
//				final Object flow = thisNode.getProperty(StructrApp.key(DOMNode.class, "flow", false));
//
//				if (flow != null || actionElement != null || inputs.size() > 0) {

					out.append(" data-structr-meta-id=\"").append(thisNode.getUuid()).append("\"");
//				}
			}

			for (final String p : rawProps) {

				final String htmlName = "data-structr-meta-" + CaseHelper.toUnderscore(p, false).replaceAll("_", "-");
				final PropertyKey key = StructrApp.key(DOMNode.class, p, false);

				if (key != null) {

					final Object value    = thisNode.getProperty(key);
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

	static Set<PropertyKey> getDataPropertyKeys(final DOMNode thisNode) {

		final Set<PropertyKey> customProperties = new TreeSet<>();
		final org.structr.api.graph.Node dbNode = thisNode.getNode();
		final Iterable<String> props            = dbNode.getPropertyKeys();

		for (final String key : props) {

			PropertyKey propertyKey = StructrApp.key(thisNode.getClass(), key, false);
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

	static void handleNewChild(final DOMNode thisNode, Node newChild) {


		try {

			final Page page            = (Page)thisNode.getOwnerDocument();
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

	static void render(final DOMNode thisNode, final RenderContext renderContext, final int depth) throws FrameworkException {

		final SecurityContext securityContext = renderContext.getSecurityContext();
		final EditMode editMode = renderContext.getEditMode(securityContext.getUser(false));

		// admin-only edit modes ==> visibility check not necessary
		final boolean isAdminOnlyEditMode = (EditMode.RAW.equals(editMode) || EditMode.WIDGET.equals(editMode) || EditMode.DEPLOYMENT.equals(editMode));
		final boolean isPartial           = renderContext.isPartialRendering(); // renderContext.getPage() == null;

		if (!isAdminOnlyEditMode && !securityContext.isVisible(thisNode)) {
			return;
		}

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

		final GraphObject details = renderContext.getDetailsDataObject();
		final boolean detailMode = details != null;

		if (detailMode && thisNode.hideOnDetail()) {
			return;
		}

		if (!detailMode && thisNode.hideOnIndex()) {
			return;
		}

		if (isAdminOnlyEditMode) {

			thisNode.renderContent(renderContext, depth);

		} else {

			final String subKey = thisNode.getDataKey();

			if (StringUtils.isNotBlank(subKey)) {

				// fetch (optional) list of external data elements
				final Iterable<GraphObject> listData = checkListSources(thisNode, securityContext, renderContext);

				final PropertyKey propertyKey;

				// Make sure the closest 'page' keyword is always set also for partials
				if (depth == 0 && isPartial) {

					renderContext.setPage(thisNode.getClosestPage());

				}

				final GraphObject dataObject = renderContext.getDataNode(subKey); // renderContext.getSourceDataObject();

				// Render partial with possible top-level repeater limited to a single data object
				if (depth == 0 && isPartial && dataObject != null) {

					renderContext.putDataObject(subKey, dataObject);
					thisNode.renderContent(renderContext, depth);

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
										thisNode.renderContent(renderContext, depth);

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
												thisNode.renderContent(renderContext, depth);

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
						thisNode.renderNodeList(securityContext, renderContext, depth, subKey);

					}
				}

			} else {

				thisNode.renderContent(renderContext, depth);
			}
		}
	}

	public static Iterable<GraphObject> checkListSources(final DOMNode thisNode, final SecurityContext securityContext, final RenderContext renderContext) {

		// try registered data sources first
		for (final GraphDataSource<Iterable<GraphObject>> source : DataSources.getDataSources()) {

			try {

				final Iterable<GraphObject> graphData = source.getData(renderContext, thisNode);
				if (graphData != null && !Iterables.isEmpty(graphData)) {

					return graphData;
				}

			} catch (FrameworkException fex) {

				final Logger logger = LoggerFactory.getLogger(DOMNode.class);
				logger.warn("Could not retrieve data from graph data source {}: {}", source, fex);
			}
		}

		return Collections.EMPTY_LIST;
	}

	public static Node doAdopt(final DOMNode thisNode, final Page _page) throws DOMException {

		if (_page != null) {

			try {

				thisNode.setOwnerDocument(_page);

			} catch (FrameworkException fex) {

				throw new DOMException(DOMException.INVALID_STATE_ERR, fex.getMessage());

			}
		}

		return thisNode;
	}

	public static Node insertBefore(final DOMNode thisNode, final Node newChild, final Node refChild) throws DOMException {

		// according to DOM spec, insertBefore with null refChild equals appendChild
		if (refChild == null) {

			return thisNode.appendChild(newChild);
		}

		thisNode.checkWriteAccess();

		thisNode.checkSameDocument(newChild);
		thisNode.checkSameDocument(refChild);

		thisNode.checkHierarchy(newChild);
		thisNode.checkHierarchy(refChild);

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
				thisNode.insertBefore(currentChild, refChild);

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
				thisNode.treeInsertBefore((DOMNode)newChild, (DOMNode)refChild);

			} catch (FrameworkException frex) {

				if (frex.getStatus() == 404) {

					throw new DOMException(DOMException.NOT_FOUND_ERR, frex.getMessage());

				} else {

					throw new DOMException(DOMException.INVALID_STATE_ERR, frex.getMessage());
				}
			}

			// allow parent to set properties in new child
			thisNode.handleNewChild(newChild);
		}

		return refChild;
	}

	public static Node replaceChild(final DOMNode thisNode, final Node newChild, final Node oldChild) throws DOMException {

		thisNode.checkWriteAccess();

		thisNode.checkSameDocument(newChild);
		thisNode.checkSameDocument(oldChild);

		thisNode.checkHierarchy(newChild);
		thisNode.checkHierarchy(oldChild);

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
				thisNode.insertBefore(currentChild, oldChild);

				// next
				currentChild = savedNextChild;
			}

			// finally, remove reference element
			thisNode.removeChild(oldChild);

		} else {

			Node _parent = newChild.getParentNode();
			if (_parent != null && _parent instanceof DOMNode) {

				_parent.removeChild(newChild);
			}

			try {
				// replace directly
				thisNode.treeReplaceChild((DOMNode)newChild, (DOMNode)oldChild);

			} catch (FrameworkException frex) {

				if (frex.getStatus() == 404) {

					throw new DOMException(DOMException.NOT_FOUND_ERR, frex.getMessage());

				} else {

					throw new DOMException(DOMException.INVALID_STATE_ERR, frex.getMessage());
				}
			}

			// allow parent to set properties in new child
			thisNode.handleNewChild(newChild);
		}

		return oldChild;
	}

	/**
	 * Get all ancestors of this node
	 *
	 * @return list of ancestors
	 */
	static List<Node> getAncestors(final DOMNode thisNode) {

		List<Node> ancestors = new ArrayList<>();

		Node _parent = thisNode.getParentNode();
		while (_parent != null) {

			ancestors.add(_parent);
			_parent = _parent.getParentNode();
		}

		return ancestors;
	}

	/**
	 * Increase version of the page.
	 *
	 * A {@link Page} is a {@link DOMNode} as well, so we have to check 'this' as well.
	 *
	 * @throws FrameworkException
	 */
	static void increasePageVersion(final DOMNode thisNode) throws FrameworkException {

		Page page = null;

		if (thisNode instanceof Page) {

			page = (Page)thisNode;

		} else {

			// ignore page-less nodes
			if (thisNode.getParent() == null) {
				return;
			}
		}

		if (page == null) {

			final List<Node> ancestors = DOMNode.getAncestors(thisNode);
			if (!ancestors.isEmpty()) {

				final DOMNode rootNode = (DOMNode)ancestors.get(ancestors.size() - 1);
				if (rootNode instanceof Page) {

					page = (Page)rootNode;

				} else {

					DOMNode.increasePageVersion(rootNode);
				}

			} else {

				for (final DOMNode syncedNode : thisNode.getSyncedNodes()) {

					DOMNode.increasePageVersion(syncedNode);
				}
			}

		}

		if (page != null) {

			page.increaseVersion();

		}
	}

	public static String getPagePath(final DOMNode thisNode) {

		String cachedPagePath = (String)thisNode.getTemporaryStorage().get("cachedPagePath");
		if (cachedPagePath == null) {

			final StringBuilder buf = new StringBuilder();
			DOMNode current         = thisNode;

			while (current != null) {

				buf.insert(0, "/" + current.getContextName());
				current = current.getParent();
			}

			cachedPagePath = buf.toString();

			thisNode.getTemporaryStorage().put("cachedPagePath", cachedPagePath);
		}

		return cachedPagePath;
	}

	static void checkName(final DOMNode thisNode, final ErrorBuffer errorBuffer) {

		final String _name = thisNode.getProperty(AbstractNode.name);
		if (_name != null) {

			if (_name.contains("/")) {

				errorBuffer.add(new SemanticErrorToken(thisNode.getType(), AbstractNode.name, "may_not_contain_slashes", _name));

			} else if (thisNode instanceof Page) {

				if (!_name.equals(_name.replaceAll("[#?\\%;/]", ""))) {
					errorBuffer.add(new SemanticErrorToken(thisNode.getType(), AbstractNode.name, "contains_illegal_characters", _name));
				}
			}
		}
	}

	static void syncName(final DOMNode thisNode, final ErrorBuffer errorBuffer) throws FrameworkException {

		// sync name only
		final String name = thisNode.getProperty(DOMNode.name);
		if (name!= null) {

			final List<DOMNode> syncedNodes = Iterables.toList(thisNode.getSyncedNodes());
			for (final DOMNode syncedNode : syncedNodes) {

				syncedNode.setProperty(DOMNode.name, name);
			}
		}
	}

	default public void setSessionAttribute(final SecurityContext securityContext, final String key, final Object value) {

		final HttpServletRequest request = securityContext.getRequest();
		if (request != null) {

			final HttpSession session = request.getSession(false);
			if (session != null) {

				session.setAttribute(ActionContext.SESSION_ATTRIBUTE_PREFIX + key, value);
			}
		}
	}

	default public void removeSessionAttribute(final SecurityContext securityContext, final String key) {

		final HttpServletRequest request = securityContext.getRequest();
		if (request != null) {

			final HttpSession session = request.getSession(false);
			if (session != null) {

				session.removeAttribute(ActionContext.SESSION_ATTRIBUTE_PREFIX + key);
			}
		}
	}

	default public Object getSessionAttribute(final SecurityContext securityContext, final String key) {

		final HttpServletRequest request = securityContext.getRequest();
		if (request != null) {

			final HttpSession session = request.getSession(false);
			if (session != null) {

				return session.getAttribute(ActionContext.SESSION_ATTRIBUTE_PREFIX + key);
			}
		}

		return null;
	}

	default public String getTreeItemSessionIdentifier(final String target) {
		return "tree-item-" + target + "-is-open";
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
