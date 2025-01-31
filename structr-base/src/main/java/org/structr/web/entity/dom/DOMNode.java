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
import org.structr.api.Predicate;
import org.structr.common.Filter;
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

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

//public interface DOMNode extends NodeTrait, LinkedTreeNode, Node, Renderable, DOMAdoptable, DOMImportable, ContextAwareEntity {
public interface DOMNode extends NodeTrait, LinkedTreeNode {

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

	static void collectNodesByPredicate(final SecurityContext securityContext, DOMNode startNode, List<DOMNode> results, Predicate<DOMNode> predicate, int depth, boolean stopOnFirstHit) throws FrameworkException {

		if (predicate instanceof Filter) {

			((Filter)predicate).setSecurityContext(securityContext);
		}

		if (predicate.accept(startNode)) {

			results.add(startNode);

			if (stopOnFirstHit) {

				return;
			}
		}

		List<DOMNode> _children = startNode.getChildNodes();
		if (_children != null) {

			int len = _children.size();
			for (int i = 0; i < len; i++) {

				DOMNode child = _children.get(i);

				collectNodesByPredicate(securityContext, child, results, predicate, depth + 1, stopOnFirstHit);
			}
		}
	}

	List<DOMNode> getAncestors();

	void increasePageVersion() throws FrameworkException;
	void checkName(final ErrorBuffer errorBuffer);
	void syncName(final ErrorBuffer errorBuffer) throws FrameworkException;
	void normalize() throws FrameworkException;
	void setHidden(final boolean hidden) throws FrameworkException;
	void setIdAttribute(final String id) throws FrameworkException;

	boolean isHidden();
	boolean isSynced();
	boolean isSharedComponent();
	boolean hasSharedComponent();
	boolean contentEquals(final DOMNode otherNode);
	boolean isVoidElement();
	boolean avoidWhitespace();
	boolean inTrash();
	boolean dontCache();
	boolean displayForLocale(final RenderContext renderContext);
	boolean displayForConditions(final RenderContext renderContext);
	boolean shouldBeRendered(final RenderContext renderContext);
	boolean isSameNode(final DOMNode otherNode);
	boolean hasChildNodes();

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
	String getNodeValue();

	boolean renderDeploymentExportComments(AsyncBuffer out, boolean isContentNode);

	void render(final RenderContext renderContext, final int depth) throws FrameworkException;
	void renderContent(final RenderContext renderContext, final int depth) throws FrameworkException;

	String getCypherQuery();
	String getRestQuery();
	String getFunctionQuery();

	String getPagePath();
	String getContextName();
	String getSharedComponentConfiguration();
	String getContentType();

	DOMNode getParent();
	DOMNode getSharedComponent();
	DOMNode getNextSibling();
	DOMNode getPreviousSibling();
	DOMNode getFirstChild() throws FrameworkException;
	DOMNode getLastChild() throws FrameworkException;
	Iterable<DOMNode> getChildren();
	Iterable<DOMNode> getSyncedNodes();

	Iterable<ActionMapping> getReloadingActions();
	Iterable<ActionMapping> getFailureActions();
	Iterable<ActionMapping> getSuccessNotificationActions();
	Iterable<ActionMapping> getFailureNotificationActions();

	DOMNode cloneNode(final boolean deep) throws FrameworkException;
	DOMNode appendChild(final DOMNode domNode) throws FrameworkException;
	DOMNode removeChild(final DOMNode newChild) throws FrameworkException;
	DOMNode replaceChild(final DOMNode newNode, final DOMNode refNode) throws FrameworkException;
	DOMNode insertBefore(final DOMNode newClonedTemplate, final DOMNode templateToBeReplaced) throws FrameworkException;

	Page getOwnerDocument();
	Page getOwnerDocumentAsSuperUser();
	Page getClosestPage();

	void setOwnerDocument(final Page page) throws FrameworkException;
	void setSharedComponent(final DOMNode sharedComponent) throws FrameworkException;

	Template getClosestTemplate(final Page page);

	void updateFromNode(final DOMNode otherNode) throws FrameworkException;
	void setVisibility(final boolean publicUsers, final boolean authenticatedUsers) throws FrameworkException;
	void renderNodeList(final SecurityContext securityContext, final RenderContext renderContext, final int depth, final String dataKey) throws FrameworkException;
	void handleNewChild(final DOMNode newChild) throws FrameworkException;
	void checkIsChild(final DOMNode otherNode) throws FrameworkException;
	void checkHierarchy(final DOMNode otherNode) throws FrameworkException;
	void checkSameDocument(final DOMNode otherNode) throws FrameworkException;
	void checkWriteAccess() throws FrameworkException;
	void checkReadAccess() throws FrameworkException;
	DOMNode doAdopt(final Page page) throws FrameworkException;
	DOMNode doImport(final Page newPage) throws FrameworkException;

	void renderManagedAttributes(final AsyncBuffer out, final SecurityContext securityContext, final RenderContext renderContext) throws FrameworkException;
	void renderCustomAttributes(final AsyncBuffer out, final SecurityContext securityContext, final RenderContext renderContext) throws FrameworkException;
	void getSecurityInstructions(final Set<String> instructions);
	void getVisibilityInstructions(final Set<String> instructions);
	void getLinkableInstructions(final Set<String> instructions);
	void getContentInstructions(final Set<String> instructions);
	void renderSharedComponentConfiguration(final AsyncBuffer out, final RenderContext.EditMode editMode);

	List<RelationshipInterface> getChildRelationships();
	List<DOMNode> getChildNodes() throws FrameworkException;

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



	// ----- nested classes -----
	class TextCollector implements Predicate<DOMNode> {

		private final StringBuilder textBuffer = new StringBuilder(200);

		@Override
		public boolean accept(final DOMNode obj) {

			if (obj.is("Content")) {
				textBuffer.append(obj.as(Content.class).getContent());
			}

			return false;
		}

		public String getText() {
			return textBuffer.toString();
		}
	}
}
