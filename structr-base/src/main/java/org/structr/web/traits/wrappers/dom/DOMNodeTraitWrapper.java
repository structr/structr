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
package org.structr.web.traits.wrappers.dom;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.Iterables;
import org.structr.common.AccessControllable;
import org.structr.common.Permission;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.SemanticErrorToken;
import org.structr.common.error.UnlicensedScriptException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.LinkedTreeNode;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;
import org.structr.core.script.Scripting;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.schema.action.Function;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.RenderContext;
import org.structr.web.common.RenderContext.EditMode;
import org.structr.web.common.StringRenderBuffer;
import org.structr.web.entity.LinkSource;
import org.structr.web.entity.Linkable;
import org.structr.web.entity.dom.*;
import org.structr.web.entity.event.ActionMapping;
import org.structr.web.property.CustomHtmlAttributeProperty;
import org.structr.web.traits.definitions.LinkSourceTraitDefinition;
import org.structr.web.traits.definitions.dom.DOMElementTraitDefinition;
import org.structr.web.traits.definitions.dom.DOMNodeTraitDefinition;
import org.structr.web.traits.operations.*;
import org.structr.websocket.command.CreateComponentCommand;

import java.util.*;

/**
 * Combines NodeInterface and DOMnode
 */
public class DOMNodeTraitWrapper extends AbstractNodeTraitWrapper implements DOMNode {

	public DOMNodeTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	/**
	 * Recursively clone given node, all its direct children and connect the cloned child nodes to the clone parent node.
	 *
	 * @param securityContext
	 * @param nodeToClone
	 * @return
	 */
	public static DOMNode cloneAndAppendChildren(final SecurityContext securityContext, final DOMNode nodeToClone, final Map<String, DOMNode> cloneMap) throws FrameworkException {

		final DOMNode newNode                   = nodeToClone.cloneNode(false);
		final Iterable<DOMNode> childrenToClone = nodeToClone.getChildren();

		if (cloneMap.put(nodeToClone.getUuid(), newNode) != null) {
			throw new FrameworkException(422, "Node already cloned!");
		}

		for (final DOMNode childNodeToClone : childrenToClone) {

			newNode.appendChild(cloneAndAppendChildren(securityContext, childNodeToClone, cloneMap));
		}

		return newNode;
	}

	@Override
	public final String getChildLinkType() {
		return StructrTraits.DOM_NODE_CONTAINS_DOM_NODE;
	}

	@Override
	public final PropertyKey<Integer> getPositionProperty() {
		return Traits.of(StructrTraits.DOM_NODE_CONTAINS_DOM_NODE).key("position");
	}

	@Override
	public final NodeInterface treeGetParent() {

		final RelationshipInterface prevRel = getIncomingRelationship(getChildLinkType());
		if (prevRel != null) {

			return prevRel.getSourceNode();
		}

		return null;
	}

	@Override
	public final void treeAppendChild(final NodeInterface childElement) throws FrameworkException {

		final NodeInterface lastChild = treeGetLastChild();

		PropertyMap properties = new PropertyMap();
		properties.put(getPositionProperty(), treeGetChildCount());

		// create child relationship
		linkChildren(this, childElement, properties);

		// add new node to linked list
		if (lastChild != null) {
			//listInsertAfter(lastChild, childElement);
		}

		ensureCorrectChildPositions();
	}

	@Override
	public final void treeInsertBefore(final NodeInterface newChild, final NodeInterface refChild) throws FrameworkException {

		final List<RelationshipInterface> rels = treeGetChildRelationships();
		boolean found = false;
		int position = 0;

		// when there are no child rels, this is an append operation
		if (rels.isEmpty()) {

			// we have no children, but the ref child is non-null => can't be ours.. :)
			if (refChild != null) {
				throw new FrameworkException(404, "Referenced child is not a child of parent node.");
			}

			treeAppendChild(newChild);
			return;
		}

		for (RelationshipInterface rel : rels) {

			NodeInterface node = rel.getTargetNode();
			if (node.equals(refChild)) {

				// will be used only once here..
				PropertyMap properties = new PropertyMap();
				properties.put(getPositionProperty(), position);

				linkChildren(this, newChild, properties);

				found = true;

				position++;
			}

			rel.setProperty(getPositionProperty(), position);

			position++;
		}

		// if child is not found, raise an exception
		if (!found) {
			throw new FrameworkException(404, "Referenced child is not a child of parent node.");
		}

		// insert new node in linked list
		//listInsertBefore(refChild, newChild);

		ensureCorrectChildPositions();
	}

	@Override
	public final void treeInsertAfter(final NodeInterface newChild, final NodeInterface refChild) throws FrameworkException {

		final List<RelationshipInterface> rels = treeGetChildRelationships();
		int position = 0;

		// when there are no child rels, this is an append operation
		if (rels.isEmpty()) {

			treeAppendChild(newChild);
			return;
		}

		for (final RelationshipInterface rel : rels) {

			NodeInterface node = rel.getTargetNode();

			rel.setProperty(getPositionProperty(), position);
			position++;

			if (node.equals(refChild)) {

				// will be used only once here..
				PropertyMap properties = new PropertyMap();
				properties.put(getPositionProperty(), position);

				linkChildren(this, newChild, properties);

				position++;
			}
		}

		// insert new node in linked list
		//listInsertAfter(refChild, newChild);

		ensureCorrectChildPositions();

	}

	@Override
	public final void treeRemoveChild(final NodeInterface childToRemove) throws FrameworkException {

		// remove element from linked list
		//listRemove(childToRemove);

		unlinkChildren(this, childToRemove);

		ensureCorrectChildPositions();
	}

	@Override
	public final void treeReplaceChild(final NodeInterface newChild, final NodeInterface oldChild) throws FrameworkException {

		// save old position
		int oldPosition = treeGetChildPosition(oldChild);

		// remove old node
		unlinkChildren(this, oldChild);

		// insert new node with position from old node
		PropertyMap properties = new PropertyMap();

		properties.put(getPositionProperty(), oldPosition);

		linkChildren(this, newChild, properties);

		// replace element in linked list as well
		//listInsertBefore(oldChild, newChild);
		//listRemove(oldChild);

		ensureCorrectChildPositions();
	}

	@Override
	public final NodeInterface treeGetFirstChild() {
		return treeGetChild(0);
	}

	@Override
	public final NodeInterface treeGetLastChild() {

		int last = treeGetChildCount() - 1;
		if (last >= 0) {

			return treeGetChild(last);
		}

		return null;
	}

	@Override
	public final NodeInterface treeGetChild(final int position) {

		for (final RelationshipInterface rel : this.getOutgoingRelationships(getChildLinkType())) {

			Integer pos = rel.getProperty(getPositionProperty());

			if (pos != null && pos == position) {

				return rel.getTargetNode();
			}
		}

		return null;
	}

	@Override
	public final int treeGetChildPosition(final NodeInterface child) {

		final RelationshipInterface rel = child.getIncomingRelationship(getChildLinkType());
		if (rel != null) {

			Integer pos = rel.getProperty(getPositionProperty());
			if (pos != null) {

				return pos;
			}
		}

		return 0;
	}

	@Override
	public final List<NodeInterface> treeGetChildren() {

		List<NodeInterface> abstractChildren = new ArrayList<>();

		for (final RelationshipInterface rel : treeGetChildRelationships()) {

			abstractChildren.add(rel.getTargetNode());
		}

		return abstractChildren;
	}

	@Override
	public final int treeGetChildCount() {
		return Iterables.count(this.getOutgoingRelationships(getChildLinkType()));
	}

	@Override
	public final List<RelationshipInterface> treeGetChildRelationships() {

		// fetch all relationships
		final List<RelationshipInterface> childRels = Iterables.toList(this.getOutgoingRelationships(getChildLinkType()));
		final PropertyKey<Integer> positionProperty = getPositionProperty();

		// sort relationships by position
		Collections.sort(childRels, (o1, o2) -> {

			Integer pos1 = o1.getProperty(positionProperty);
			Integer pos2 = o2.getProperty(positionProperty);

			if (pos1 != null && pos2 != null) {

				return pos1.compareTo(pos2);
			}

			return 0;
		});

		return childRels;
	}

	@Override
	public final void ensureCorrectChildPositions() throws FrameworkException {

		final List<RelationshipInterface> childRels = treeGetChildRelationships();
		int position = 0;

		for (RelationshipInterface childRel : childRels) {
			childRel.setProperty(getPositionProperty(), position++);
		}
	}

	@Override
	public final void linkChildren(final NodeInterface startNode, final NodeInterface endNode) throws FrameworkException {
		linkChildren(startNode, endNode, null);
	}

	@Override
	public final void linkChildren(final NodeInterface startNode, final NodeInterface endNode, final PropertyMap properties) throws FrameworkException {
		StructrApp.getInstance(getSecurityContext()).create(startNode, endNode, getChildLinkType(), properties);
	}

	@Override
	public final void unlinkChildren(NodeInterface startNode, NodeInterface endNode) throws FrameworkException {

		final List<RelationshipInterface> list = Iterables.toList(startNode.getRelationships(getChildLinkType()));
		final App app                          = StructrApp.getInstance(getSecurityContext());

		for (RelationshipInterface rel : list) {

			if (rel != null && rel.getTargetNode().equals(endNode)) {
				app.delete(rel);
			}
		}
	}

	public final Set<NodeInterface> getAllChildNodes() {

		final Set<NodeInterface> allChildNodes = new HashSet();

		for (final NodeInterface child : treeGetChildren()) {

			allChildNodes.add(child);
			allChildNodes.addAll(child.as(DOMNode.class).getAllChildNodes());
		}

		return allChildNodes;
	}

	@Override
	public final Template getClosestTemplate(final Page page) {

		DOMNode node = this;

		while (node != null) {

			if (node.is(StructrTraits.TEMPLATE)) {

				final Template template = node.as(Template.class);

				Page doc = node.getOwnerDocument();

				if (doc == null) {

					doc = node.getClosestPage();
				}

				if (doc != null && (page == null || doc.equals(page))) {

					return template;

				}

				for (final DOMNode syncedNode : node.getSyncedNodes()) {

					doc = syncedNode.getOwnerDocument();

					if (doc != null && (page == null || doc.equals(page))) {

						return (Template)syncedNode;

					}
				}
			}

			node = node.getParent();
		}

		return null;
	}

	@Override
	public final Page getClosestPage() {

		DOMNode node = this;

		while (node != null) {

			if (node.is(StructrTraits.PAGE)) {

				return node.as(Page.class);
			}

			node = node.getParent();

		}

		return null;
	}

	public final String getPositionPath() {

		String path = "";

		DOMNode currentNode = this;
		while (currentNode.getParent() != null) {

			DOMNode parentNode = currentNode.getParent();

			path = "/" + parentNode.getChildPosition(currentNode) + path;

			currentNode = parentNode;

		}

		return path;

	}

	public final String getContent(final RenderContext.EditMode editMode) throws FrameworkException {

		final RenderContext ctx         = new RenderContext(getSecurityContext(), null, null, editMode);
		final StringRenderBuffer buffer = new StringRenderBuffer();

		ctx.setBuffer(buffer);
		render(ctx, 0);

		// extract source
		return buffer.getBuffer().toString();
	}

	public final String getIdHashOrProperty() {

		String idHash = getDataHash();
		if (idHash == null) {

			idHash = getIdHash();
		}

		return idHash;
	}

	public final Page getOwnerDocumentAsSuperUser() {

		final RelationshipInterface ownership = wrappedObject.getOutgoingRelationshipAsSuperUser(StructrTraits.DOM_NODE_PAGE_PAGE);
		if (ownership != null) {

			return ownership.getTargetNode().as(Page.class);
		}

		return null;
	}

	public final boolean isSharedComponent() {

		final Page _ownerDocument = getOwnerDocumentAsSuperUser();
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

	public static final void copyAllAttributes(final DOMNode sourceNode, final DOMNode targetNode) throws FrameworkException{

		final SecurityContext securityContext = sourceNode.getSecurityContext();
		final PropertyMap properties          = new PropertyMap();

		for (final PropertyKey key : sourceNode.getTraits().getPropertyKeysForView(PropertyView.Ui)) {

			// skip blacklisted properties
			if (cloneBlacklist.contains(key.jsonName())) {
				continue;
			}

			// skip tagName, otherwise the target node will have mismatching type and tag
			if (DOMElementTraitDefinition.TAG_PROPERTY.equals(key.jsonName())) {
				continue;
			}

			if (!key.isUnvalidated()) {
				properties.put(key, sourceNode.getProperty(key));
			}
		}

		// htmlView is necessary for the cloning of DOM nodes - otherwise some properties won't be cloned
		for (final PropertyKey key : sourceNode.getTraits().getPropertyKeysForView(PropertyView.Html)) {

			// skip blacklisted properties
			if (cloneBlacklist.contains(key.jsonName())) {
				continue;
			}

			// skip tagName, otherwise the target node will have mismatching type and tag
			if (DOMElementTraitDefinition.TAG_PROPERTY.equals(key.jsonName())) {
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

		if (sourceNode.is(StructrTraits.LINK_SOURCE)) {

			final LinkSource linkSourceElement = (LinkSource)sourceNode;

			properties.put(Traits.of(StructrTraits.LINK_SOURCE).key(LinkSourceTraitDefinition.LINKABLE_PROPERTY), linkSourceElement.getLinkable());
		}

		// set the properties we collected above
		targetNode.setProperties(securityContext, properties);

		// for clone, always copy permissions
		sourceNode.copyPermissionsTo(securityContext, targetNode, true);
	}

	@Override
	public final boolean renderDeploymentExportComments(final AsyncBuffer out, final boolean isContentNode) {

		final Set<String> instructions = new LinkedHashSet<>();

		getVisibilityInstructions(instructions);
		getLinkableInstructions(instructions);
		getSecurityInstructions(instructions);

		if (this.isHidden()) {
			instructions.add("@structr:hidden");
		}

		if (isContentNode) {

			// special rules apply for content nodes: since we can not store
			// structr-specific properties in the attributes of the element,
			// we need to encode those attributes in instructions.
			getContentInstructions(instructions);
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

	@Override
	public final void render(final RenderContext renderContext, final int depth) throws FrameworkException {
		traits.getMethod(Render.class).render(this, renderContext, depth);
	}

	@Override
	public final void renderContent(final RenderContext renderContext, final int depth) throws FrameworkException {
		traits.getMethod(RenderContent.class).renderContent(this, renderContext, depth);
	}

	@Override
	public final DOMNode doAdopt(final Page _page) throws FrameworkException {
		return traits.getMethod(DoAdopt.class).doAdopt(this, _page);
	}

	@Override
	public final DOMNode doImport(final Page newPage) throws FrameworkException {
		return traits.getMethod(DoImport.class).doImport(this, newPage);
	}

	/**
	 * Get all ancestors of this node
	 *
	 * @return list of ancestors
	 */
	@Override
	public List<DOMNode> getAncestors() {

		List<DOMNode> ancestors = new ArrayList<>();

		DOMNode _parent = getParent();
		while (_parent != null) {

			ancestors.add(_parent);
			_parent = _parent.getParent();
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
	@Override
	public void increasePageVersion() throws FrameworkException {

		Page page = null;

		if (wrappedObject.getTraits().contains(StructrTraits.PAGE)) {

			page = wrappedObject.as(Page.class);

		} else {

			// ignore page-less nodes
			if (getParent() == null) {
				return;
			}
		}

		if (page == null) {

			final List<DOMNode> ancestors = getAncestors();
			if (!ancestors.isEmpty()) {

				final DOMNode rootNode = ancestors.get(ancestors.size() - 1);
				if (rootNode.is(StructrTraits.PAGE)) {

					page = rootNode.as(Page.class);

				} else {

					rootNode.increasePageVersion();
				}

			} else {

				for (final DOMNode syncedNode : getSyncedNodes()) {

					syncedNode.increasePageVersion();
				}
			}

		}

		if (page != null) {

			page.increaseVersion();

		}
	}

	@Override
	public void checkName(final ErrorBuffer errorBuffer) {

		final String _name = getName();
		if (_name != null) {

			if (_name.contains("/")) {

				errorBuffer.add(new SemanticErrorToken(getType(), "name", "may_not_contain_slashes").withDetail(_name));

			} else if (wrappedObject.getTraits().contains(StructrTraits.PAGE)) {

				if (!_name.equals(_name.replaceAll("[#?\\%;/]", ""))) {
					errorBuffer.add(new SemanticErrorToken(getType(), "name", "contains_illegal_characters").withDetail(_name));
				}
			}
		}
	}

	@Override
	public void syncName(final ErrorBuffer errorBuffer) throws FrameworkException {

		// sync name only
		final String name = getName();
		if (name != null) {

			final List<DOMNode> syncedNodes = Iterables.toList(getSyncedNodes());
			for (final DOMNode syncedNode : syncedNodes) {

				syncedNode.setName(name);
			}
		}
	}

	@Override
	public final boolean isSynced() {
		return Iterables.count(getSyncedNodes()) > 0 || getSharedComponent() != null;
	}

	@Override
	public final boolean isVoidElement() {
		return traits.getMethod(IsVoidElement.class).isVoidElement();
	}

	@Override
	public final boolean avoidWhitespace() {
		return traits.getMethod(AvoidWhitespace.class).avoidWhitespace();
	}

	@Override
	public final boolean inTrash() {
		return getParent() == null && getOwnerDocumentAsSuperUser() == null;
	}

	@Override
	public final boolean dontCache() {
		return wrappedObject.getProperty(traits.key(DOMNodeTraitDefinition.DONT_CACHE_PROPERTY));
	}

	@Override
	public final boolean displayForLocale(final RenderContext renderContext) {

		final String localeString = renderContext.getLocale().toString();
		final String show         = getShowForLocales();
		final String hide         = getHideForLocales();

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

	@Override
	public final boolean displayForConditions(final RenderContext renderContext)  {

		String _showConditions = getShowConditions();
		String _hideConditions = getHideConditions();

		// If both fields are empty, render node
		if (StringUtils.isBlank(_hideConditions) && StringUtils.isBlank(_showConditions)) {
			return true;
		}

		try {
			// If hide conditions evaluates to "true", don't render
			if (StringUtils.isNotBlank(_hideConditions) && Boolean.TRUE.equals(Scripting.evaluate(renderContext, wrappedObject, "${".concat(_hideConditions.trim()).concat("}"), "hideConditions", getUuid()))) {
				return false;
			}

		} catch (UnlicensedScriptException | FrameworkException ex) {

			final Logger logger        = LoggerFactory.getLogger(DOMNode.class);
			final boolean isShadowPage = isSharedComponent();

			if (!isShadowPage) {

				final DOMNode ownerDocument = getOwnerDocumentAsSuperUser();
				DOMNode.logScriptingError(logger, ex, "Error while evaluating hide condition '{}' in page {}[{}], DOMNode[{}]", _hideConditions, ownerDocument.getName(), ownerDocument.getUuid(), getUuid());

			} else {

				DOMNode.logScriptingError(logger, ex, "Error while evaluating hide condition '{}' in shared component, DOMNode[{}]", _hideConditions, getUuid());
			}
		}

		try {
			// If show conditions evaluates to "false", don't render
			if (StringUtils.isNotBlank(_showConditions) && Boolean.FALSE.equals(Scripting.evaluate(renderContext, wrappedObject, "${".concat(_showConditions.trim()).concat("}"), "showConditions", getUuid()))) {
				return false;
			}

		} catch (UnlicensedScriptException | FrameworkException ex) {

			final Logger logger        = LoggerFactory.getLogger(DOMNode.class);
			final boolean isShadowPage = isSharedComponent();

			if (!isShadowPage) {

				final DOMNode ownerDocument = getOwnerDocumentAsSuperUser();
				DOMNode.logScriptingError(logger, ex, "Error while evaluating show condition '{}' in page {}[{}], DOMNode[{}]", _showConditions, ownerDocument.getName(), ownerDocument.getUuid(), getUuid());

			} else {

				DOMNode.logScriptingError(logger, ex, "Error while evaluating show condition '{}' in shared component, DOMNode[{}]", _showConditions, getUuid());
			}
		}

		return true;
	}

	@Override
	public final boolean shouldBeRendered(final RenderContext renderContext) {

		// In raw, widget or deployment mode, render everything
		EditMode editMode = renderContext.getEditMode(renderContext.getSecurityContext().getUser(false));
		if (EditMode.DEPLOYMENT.equals(editMode) || EditMode.RAW.equals(editMode) || EditMode.WIDGET.equals(editMode)) {
			return true;
		}

		if (wrappedObject.isHidden() || !displayForLocale(renderContext) || !displayForConditions(renderContext)) {
			return false;
		}

		return true;
	}

	@Override
	public boolean isSameNode(final DOMNode otherNode) {

		if (otherNode != null) {

			String otherId = otherNode.getUuid();
			String ourId   = this.getUuid();

			if (ourId != null && otherId != null && ourId.equals(otherId)) {
				return true;
			}
		}

		return false;

	}

	@Override
	public final boolean hasSharedComponent() {
		return wrappedObject.getProperty(traits.key(DOMNodeTraitDefinition.HAS_SHARED_COMPONENT_PROPERTY));
	}

	@Override
	public boolean contentEquals(final DOMNode otherNode) {
		return traits.getMethod(ContentEquals.class).contentEquals(this, otherNode);
	}

	@Override
	public final int getChildPosition(final DOMNode otherNode) {

		final LinkedTreeNode node = wrappedObject.as(DOMNode.class);

		return node.treeGetChildPosition(otherNode);
	}

	@Override
	public final String getIdHash() {
		return getUuid();
	}

	@Override
	public final String getShowConditions() {
		return wrappedObject.getProperty(traits.key(DOMNodeTraitDefinition.SHOW_CONDITIONS_PROPERTY));
	}

	@Override
	public final String getHideConditions() {
		return wrappedObject.getProperty(traits.key(DOMNodeTraitDefinition.HIDE_CONDITIONS_PROPERTY));
	}

	@Override
	public final String getShowForLocales() {
		return wrappedObject.getProperty(traits.key(DOMNodeTraitDefinition.SHOW_FOR_LOCALES_PROPERTY));
	}

	@Override
	public final String getHideForLocales() {
		return wrappedObject.getProperty(traits.key(DOMNodeTraitDefinition.HIDE_FOR_LOCALES_PROPERTY));
	}

	@Override
	public final String getDataHash() {
		return wrappedObject.getProperty(traits.key(DOMNodeTraitDefinition.DATA_STRUCTR_HASH_PROPERTY));
	}

	@Override
	public final String getDataKey() {
		return wrappedObject.getProperty(traits.key(DOMNodeTraitDefinition.DATA_KEY_PROPERTY));
	}

	@Override
	public final String getCssClass() {
		return traits.getMethod(GetCssClass.class).getCssClass(wrappedObject);
	}

	@Override
	public String getNodeValue() {
		return traits.getMethod(GetNodeValue.class).getNodeValue(wrappedObject);
	}

	@Override
	public final void renderManagedAttributes(AsyncBuffer out, SecurityContext securityContext, RenderContext renderContext) throws FrameworkException {
		traits.getMethod(RenderManagedAttributes.class).renderManagedAttributes(wrappedObject, out, securityContext, renderContext);
	}

	@Override
	public final String getCypherQuery() {
		return wrappedObject.getProperty(traits.key(DOMNodeTraitDefinition.CYPHER_QUERY_PROPERTY));
	}

	@Override
	public final String getRestQuery() {
		return wrappedObject.getProperty(traits.key(DOMNodeTraitDefinition.REST_QUERY_PROPERTY));
	}

	@Override
	public final String getFunctionQuery() {
		return wrappedObject.getProperty(traits.key(DOMNodeTraitDefinition.FUNCTION_QUERY_PROPERTY));
	}

	@Override
	public final String getPagePath() {
		return traits.getMethod(GetPagePath.class).getPagePath(wrappedObject);
	}

	@Override
	public final String getContextName() {
		return traits.getMethod(GetContextName.class).getContextName(wrappedObject);
	}

	@Override
	public final String getSharedComponentConfiguration() {
		return wrappedObject.getProperty(traits.key(DOMNodeTraitDefinition.SHARED_COMPONENT_CONFIGURATION_PROPERTY));
	}

	@Override
	public final DOMNode getParent() {

		final NodeInterface node = wrappedObject.getProperty(traits.key(DOMNodeTraitDefinition.PARENT_PROPERTY));
		if (node != null) {

			return node.as(DOMNode.class);
		}

		return null;
	}

	@Override
	public final DOMNode getSharedComponent() {

		final NodeInterface node = wrappedObject.getProperty(traits.key(DOMNodeTraitDefinition.SHARED_COMPONENT_PROPERTY));
		if (node != null) {

			return node.as(DOMNode.class);
		}

		return null;
	}

	@Override
	public final Iterable<DOMNode> getChildren() {
		return Iterables.map(n -> n.as(DOMNode.class), treeGetChildren());
	}

	@Override
	public final Iterable<DOMNode> getSyncedNodes() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(DOMNodeTraitDefinition.SYNCED_NODES_PROPERTY);

		return Iterables.map(n -> n.as(DOMNode.class), wrappedObject.getProperty(key));
	}

	@Override
	public final Iterable<ActionMapping> getReloadingActions() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(DOMNodeTraitDefinition.RELOADING_ACTIONS_PROPERTY);

		return Iterables.map(n -> n.as(ActionMapping.class), wrappedObject.getProperty(key));
	}

	@Override
	public final Iterable<ActionMapping> getFailureActions() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(DOMNodeTraitDefinition.FAILURE_ACTIONS_PROPERTY);

		return Iterables.map(n -> n.as(ActionMapping.class), wrappedObject.getProperty(key));
	}

	@Override
	public final Iterable<ActionMapping> getSuccessNotificationActions() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(DOMNodeTraitDefinition.SUCCESS_NOTIFICATION_ACTIONS_PROPERTY);

		return Iterables.map(n -> n.as(ActionMapping.class), wrappedObject.getProperty(key));
	}

	@Override
	public final Iterable<ActionMapping> getFailureNotificationActions() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(DOMNodeTraitDefinition.FAILURE_NOTIFICATION_ACTIONS_PROPERTY);

		return Iterables.map(n -> n.as(ActionMapping.class), wrappedObject.getProperty(key));
	}

	@Override
	public final void setOwnerDocument(final Page page) throws FrameworkException {
		wrappedObject.setProperty(traits.key(DOMNodeTraitDefinition.OWNER_DOCUMENT_PROPERTY), page);
	}

	@Override
	public final void setSharedComponent(final DOMNode sharedComponent) throws FrameworkException {
		wrappedObject.setProperty(traits.key(DOMNodeTraitDefinition.SHARED_COMPONENT_PROPERTY), sharedComponent);
	}

	@Override
	public final void updateFromNode(final DOMNode otherNode) throws FrameworkException {
		traits.getMethod(UpdateFromNode.class).updateFromNode(wrappedObject, otherNode);
	}

	@Override
	public final void updateHasSharedComponentFlag() throws FrameworkException {
		wrappedObject.setProperty(traits.key(DOMNodeTraitDefinition.HAS_SHARED_COMPONENT_PROPERTY), getSharedComponent() != null);
	}

	@Override
	public final void setVisibility(final boolean publicUsers, final boolean authenticatedUsers) throws FrameworkException {
		wrappedObject.setVisibility(publicUsers, authenticatedUsers);
	}

	@Override
	public final void renderNodeList(SecurityContext securityContext, RenderContext renderContext, int depth, String dataKey) throws FrameworkException {

		final Iterable<GraphObject> listSource = renderContext.getListSource();
		if (listSource != null) {

			final GraphObject previousDataObject = renderContext.getDataNode(dataKey);

			try {
				for (final Object dataObject : listSource) {

					// make current data object available in renderContext
					if (dataObject instanceof NodeInterface n) {

						renderContext.putDataObject(dataKey, n);

					} else if (dataObject instanceof GraphObject o) {

						renderContext.putDataObject(dataKey, o);

					} else if (dataObject instanceof Iterable i) {

						renderContext.putDataObject(dataKey, Function.recursivelyWrapIterableInMap(i, 0));
					}

					renderContent(renderContext, depth + 1);
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

	@Override
	public final void handleNewChild(final DOMNode newChild) throws FrameworkException {
		traits.getMethod(HandleNewChild.class).handleNewChild(this, newChild);
	}

	@Override
	public final void checkIsChild(final DOMNode otherNode) throws FrameworkException {

		DOMNode _parent = otherNode.getParent();

		if (!isSameNode(_parent)) {

			throw new FrameworkException(422, NOT_FOUND_ERR_MESSAGE);
		}
	}

	public final void checkHierarchy(final DOMNode otherNode) throws FrameworkException {
		traits.getMethod(CheckHierarchy.class).checkHierarchy(this, otherNode);
	}

	public final void checkSameDocument(final DOMNode otherNode) throws FrameworkException {

		Page doc = getOwnerDocument();
		if (doc != null) {

			Page otherDoc = otherNode.getOwnerDocument();

			// Shadow doc is neutral
			if (otherDoc != null && !doc.equals(otherDoc) && !(doc.is(StructrTraits.SHADOW_DOCUMENT))) {

				final Logger logger = LoggerFactory.getLogger(DOMNode.class);
				logger.warn("{} node with UUID {} has owner document {} with UUID {} whereas this node has owner document {} with UUID {}",
					otherNode.getType(),
					otherNode.getUuid(),
					otherDoc.getType(),
					otherDoc.getUuid(),
					doc.getType(),
					doc.getUuid()
				);

				throw new FrameworkException(422, WRONG_DOCUMENT_ERR_MESSAGE);
			}

			if (otherDoc == null) {

				otherNode.doAdopt(doc);

			}
		}
	}

	@Override
	public final void checkWriteAccess() throws FrameworkException {

		if (!as(AccessControllable.class).isGranted(Permission.write, getSecurityContext())) {

			throw new FrameworkException(422, NO_MODIFICATION_ALLOWED_MESSAGE);
		}
	}

	@Override
	public final void checkReadAccess() throws FrameworkException {

		final SecurityContext securityContext = getSecurityContext();

		// superuser can do everything
		if (securityContext != null && securityContext.isSuperUser()) {
			return;
		}

		final AccessControllable ac = as(AccessControllable.class);

		if (securityContext.isVisible(wrappedObject) || ac.isGranted(Permission.read, securityContext)) {
			return;
		}

		throw new FrameworkException(422, INVALID_ACCESS_ERR_MESSAGE);
	}

	@Override
	public final void renderCustomAttributes(final AsyncBuffer out, final SecurityContext securityContext, final RenderContext renderContext) throws FrameworkException {
		traits.getMethod(RenderCustomAttributes.class).renderCustomAttributes(this, out, securityContext, renderContext);
	}

	@Override
	public final void getSecurityInstructions(final Set<String> instructions) {

		final Principal _owner = as(AccessControllable.class).getOwnerNode();
		if (_owner != null) {

			instructions.add("@structr:owner(" + _owner.getName() + ")");
		}

		as(AccessControllable.class).getSecurityRelationships().stream().filter(Objects::nonNull).sorted(Comparator.comparing(security -> security.getSourceNode().getName())).forEach(security -> {

			final Principal grantee = security.getSourceNode();
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
				instructions.add("@structr:grant(" + grantee.getName() + "," + shortPerms.toString() + ")");
			}
		});
	}

	@Override
	public final void getVisibilityInstructions(final Set<String> instructions) {

		final DOMNode _parentNode       = getParent();
		final boolean elementPublic     = wrappedObject.isVisibleToPublicUsers();
		final boolean elementProtected  = wrappedObject.isVisibleToAuthenticatedUsers();
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

			addVisibilityInstructions = (_parentNode.is(StructrTraits.SHADOW_DOCUMENT)) || (parentPublic != elementPublic || parentProtected != elementProtected);
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

	@Override
	public final void getLinkableInstructions(final Set<String> instructions) {

		if (traits.contains(StructrTraits.LINK_SOURCE)) {

			final LinkSource linkSourceElement = wrappedObject.as(LinkSource.class);
			final Linkable linkable            = linkSourceElement.getLinkable();

			if (linkable != null) {

				final String linkableInstruction = (linkable.is(StructrTraits.PAGE)) ? "pagelink" : "link";

				String path = linkable.getPath();

				if (linkable.is(StructrTraits.PAGE) && path == null) {
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

	@Override
	public final void getContentInstructions(final Set<String> instructions) {

		if (is(StructrTraits.CONTENT)) {

			final String _contentType = as(Content.class).getContentType();
			if (_contentType != null) {

				instructions.add("@structr:content(" + DOMNode.escapeForHtmlAttributes(_contentType) + ")");
			}
		}

		if (!is(StructrTraits.TEMPLATE)) {

			final String _name = wrappedObject.getProperty(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY));
			if (StringUtils.isNotEmpty(_name)) {

				instructions.add("@structr:name(" + DOMNode.escapeForHtmlAttributes(_name) + ")");
			}

			final String _showConditions = getShowConditions();
			if (StringUtils.isNotEmpty(_showConditions)) {

				instructions.add("@structr:show(" + DOMNode.escapeForHtmlAttributes(_showConditions) + ")");
			}

			final String _hideConditions = getHideConditions();
			if (StringUtils.isNotEmpty(_hideConditions)) {

				instructions.add("@structr:hide(" + DOMNode.escapeForHtmlAttributes(_hideConditions) + ")");
			}

			final String _showForLocales = getShowForLocales();
			if (StringUtils.isNotEmpty(_showForLocales)) {

				instructions.add("@structr:show-for-locales(" + DOMNode.escapeForHtmlAttributes(_showForLocales) + ")");
			}

			final String _hideForLocales = getHideForLocales();
			if (StringUtils.isNotEmpty(_hideForLocales)) {

				instructions.add("@structr:hide-for-locales(" + DOMNode.escapeForHtmlAttributes(_hideForLocales) + ")");
			}
		}
	}

	@Override
	public final void renderSharedComponentConfiguration(final AsyncBuffer out, final EditMode editMode) {

		if (EditMode.DEPLOYMENT.equals(editMode)) {

			final String configuration = getSharedComponentConfiguration();
			if (StringUtils.isNotBlank(configuration)) {

				out.append(" data-structr-meta-shared-component-configuration=\"");
				out.append(DOMNode.escapeForHtmlAttributes(configuration));
				out.append("\"");
			}
		}
	}

	@Override
	public final List<RelationshipInterface> getChildRelationships() {
		return treeGetChildRelationships();
	}

	@Override
	public final void doAppendChild(final DOMNode node) throws FrameworkException {

		checkWriteAccess();

		treeAppendChild(node);
	}

	@Override
	public final void doRemoveChild(final DOMNode node) throws FrameworkException {

		checkWriteAccess();

		treeRemoveChild(node);
	}

	@Override
	public final Set<PropertyKey> getDataPropertyKeys() {

		final Set<PropertyKey> customProperties = new TreeSet<>();
		final org.structr.api.graph.Node dbNode = wrappedObject.getNode();
		final Iterable<String> props            = dbNode.getPropertyKeys();

		for (final String key : props) {

			if (key.startsWith("data-")) {

				final PropertyKey propertyKey = traits.hasKey(key) ? traits.key(key) : new StringProperty(key);

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

				final PropertyKey propertyKey = traits.hasKey(key) ? traits.key(key) : new StringProperty(key);

				final CustomHtmlAttributeProperty customProp = new CustomHtmlAttributeProperty(propertyKey);

				customProperties.add(customProp);
			}
		}

		return customProperties;
	}

	/*
	@Override
	public final String getSiblingLinkType() {
		return StructrTraits.DOM_NODE_CONTAINS_NEXT_SIBLING_DOM_NODE;
	}

	@Override
	public final NodeInterface listGetPrevious(final NodeInterface currentElement) {

		final RelationshipInterface prevRel = currentElement.getIncomingRelationship(getSiblingLinkType());
		if (prevRel != null) {

			return prevRel.getSourceNode();
		}

		return null;
	}

	@Override
	public final NodeInterface listGetNext(final NodeInterface currentElement) {

		final RelationshipInterface nextRel = currentElement.getOutgoingRelationship(getSiblingLinkType());
		if (nextRel != null) {

			return nextRel.getTargetNode();
		}

		return null;
	}

	@Override
	public final void listInsertBefore(final NodeInterface currentElement, final NodeInterface newElement) throws FrameworkException {

		if (currentElement.getUuid().equals(newElement.getUuid())) {
			throw new IllegalStateException("Cannot link a node to itself!");
		}

		final NodeInterface previousElement = listGetPrevious(currentElement);
		if (previousElement == null) {

			linkSiblings(newElement, currentElement);

		} else {
			// delete old relationship
			unlinkSiblings(previousElement, currentElement);

			// dont create self link
			if (!previousElement.getUuid().equals(newElement.getUuid())) {
				linkSiblings(previousElement, newElement);
			}

			// dont create self link
			if (!newElement.getUuid().equals(currentElement.getUuid())) {
				linkSiblings(newElement, currentElement);
			}
		}
	}

	@Override
	public final void listInsertAfter(final NodeInterface currentElement, final NodeInterface newElement) throws FrameworkException {

		if (currentElement.getUuid().equals(newElement.getUuid())) {
			throw new IllegalStateException("Cannot link a node to itself!");
		}

		final NodeInterface next = listGetNext(currentElement);
		if (next == null) {

			linkSiblings(currentElement, newElement);

		} else {

			// unlink predecessor and successor
			unlinkSiblings(currentElement, next);

			// link predecessor to new element
			linkSiblings(currentElement, newElement);

			// dont create self link
			if (!newElement.getUuid().equals(next.getUuid())) {

				// link new element to successor
				linkSiblings(newElement, next);
			}
		}
	}

	@Override
	public final void listRemove(final NodeInterface currentElement) throws FrameworkException {

		final NodeInterface previousElement = listGetPrevious(currentElement);
		final NodeInterface nextElement     = listGetNext(currentElement);

		if (currentElement != null) {

			if (previousElement != null) {
				unlinkSiblings(previousElement, currentElement);
			}

			if (nextElement != null) {
				unlinkSiblings(currentElement, nextElement);
			}
		}

		if (previousElement != null && nextElement != null) {

			linkSiblings(previousElement, nextElement);
		}
	}

	@Override
	public final void linkSiblings(final NodeInterface startNode, final NodeInterface endNode) throws FrameworkException {
		linkSiblings(startNode, endNode, null);
	}

	@Override
	public final void linkSiblings(final NodeInterface startNode, final NodeInterface endNode, final PropertyMap properties) throws FrameworkException {
		StructrApp.getInstance(getSecurityContext()).create(startNode, endNode, getSiblingLinkType(), properties);
	}

	@Override
	public final void unlinkSiblings(final NodeInterface startNode, final NodeInterface endNode) throws FrameworkException {

		final App app = StructrApp.getInstance(getSecurityContext());

		for (RelationshipInterface rel : startNode.getRelationships(getSiblingLinkType())) {

			if (rel != null && rel.getTargetNode().equals(endNode)) {
				app.delete(rel);
			}
		}
	}
	*/

	public final DOMNode getFirstChild() throws FrameworkException {

		checkReadAccess();

		final NodeInterface node = treeGetFirstChild();
		if (node != null) {

			return node.as(DOMNode.class);
		}

		return null;
	}

	/*
	public final DOMNode getLastChild() throws FrameworkException {

		checkReadAccess();

		final NodeInterface node = treeGetLastChild();
		if (node != null) {

			return node.as(DOMNode.class);
		}

		return null;
	}

	public final DOMNode getPreviousSibling() {

		final NodeInterface node = wrappedObject.getProperty(traits.key(DOMNodeTraitDefinition.PREVIOUS_SIBLING_PROPERTY));
		if (node != null) {

			return node.as(DOMNode.class);
		}

		return null;
	}
	*/

	public final DOMNode getNextSibling() {

		final DOMNode parent = getParent();
		if (parent != null) {

			final int pos = parent.getChildPosition(this);
			if (pos >= 0) {

				final NodeInterface next = parent.treeGetChild(pos + 1);
				if (next != null && next.is(StructrTraits.DOM_NODE)) {

					return next.as(DOMNode.class);
				}
			}
		}

		return null;
	}

	@Override
	public final Page getOwnerDocument() {

		final NodeInterface node = wrappedObject.getProperty(traits.key(DOMNodeTraitDefinition.OWNER_DOCUMENT_PROPERTY));
		if (node != null) {

			return node.as(Page.class);
		}

		return null;
	}

	public final DOMNode insertBefore(final DOMNode newChild, final DOMNode refChild) throws FrameworkException {

		// according to DOM spec, insertBefore with null refChild equals appendChild
		if (refChild == null) {

			return appendChild(newChild);
		}

		checkWriteAccess();

		checkSameDocument(newChild);
		checkSameDocument(refChild);

		checkHierarchy(newChild);
		checkHierarchy(refChild);

		final DOMNode _parent = newChild.getParent();
		if (_parent != null) {

			_parent.removeChild(newChild);
		}

		try {

			// do actual tree insertion here
			treeInsertBefore(newChild, refChild);

		} catch (FrameworkException frex) {

			if (frex.getStatus() == 404) {

				throw new FrameworkException(422, frex.getMessage());

			} else {

				throw new FrameworkException(422, frex.getMessage());
			}
		}

		// allow parent to set properties in new child
		handleNewChild(newChild);

		return refChild;
	}

	public final DOMNode replaceChild(final DOMNode newChild, final DOMNode oldChild) throws FrameworkException {

		checkWriteAccess();

		checkSameDocument(newChild);
		checkSameDocument(oldChild);

		checkHierarchy(newChild);
		checkHierarchy(oldChild);

		DOMNode _parent = newChild.getParent();
		if (_parent != null) {

			_parent.removeChild(newChild);
		}

		try {
			// replace directly
			treeReplaceChild(newChild, oldChild);

		} catch (FrameworkException frex) {

			if (frex.getStatus() == 404) {

				throw new FrameworkException(422, frex.getMessage());

			} else {

				throw new FrameworkException(422, frex.getMessage());
			}
		}

		// allow parent to set properties in new child
		handleNewChild(newChild);

		return oldChild;
	}

	public final DOMNode removeChild(final DOMNode node) throws FrameworkException {

		checkWriteAccess();
		checkSameDocument(node);
		checkIsChild(node);

		try {

			doRemoveChild(node);

		} catch (FrameworkException fex) {

			throw new FrameworkException(422, fex.toString());
		}

		return node;
	}

	@Override
	public final DOMNode appendChild(final DOMNode newChild) throws FrameworkException {

		checkWriteAccess();
		checkSameDocument(newChild);
		checkHierarchy(newChild);

		try {

			final DOMNode _parent = newChild.getParent();
			if (_parent != null) {

				_parent.removeChild(newChild);
			}

			doAppendChild(newChild);

			// allow parent to set properties in new child
			handleNewChild(newChild);

		} catch (FrameworkException fex) {

			throw new FrameworkException(422, fex.toString());
		}

		return newChild;
	}

	public final boolean hasChildNodes() {

		final PropertyKey<Iterable<NodeInterface>> childrenKey = traits.key(DOMNodeTraitDefinition.CHILDREN_PROPERTY);

		return wrappedObject.getProperty(childrenKey).iterator().hasNext();
	}

	@Override
	public final DOMNode cloneNode(final boolean deep) throws FrameworkException {

		final SecurityContext securityContext = getSecurityContext();

		if (deep) {

			final Map<String, DOMNode> cloneMap = new LinkedHashMap<>();
			final DOMNode clone                 = cloneAndAppendChildren(securityContext, this, cloneMap);
			final App app                       = StructrApp.getInstance(securityContext);

			// clone event action mapping as well (must be done after cloning all DOM nodes)
			for (final String uuid : cloneMap.keySet()) {

				// fetch original node, so we can check for action mappings to clone
				final NodeInterface originalNode = app.getNodeById(uuid);
				if (originalNode != null && originalNode.is(StructrTraits.DOM_ELEMENT)) {

					final DOMElement originalElement = originalNode.as(DOMElement.class);
					final DOMNode clonedNode         = cloneMap.get(uuid);

					if (clonedNode.is(StructrTraits.DOM_ELEMENT)) {

						final List<NodeInterface> clonedActionMappings = new LinkedList<>();
						final DOMElement clonedElement = clonedNode.as(DOMElement.class);

						for (final ActionMapping actionMapping : originalElement.getTriggeredActions()) {

							final NodeInterface clonedActionMapping = actionMapping.cloneActionMapping(cloneMap);
							if (clonedActionMapping != null) {

								clonedActionMappings.add(clonedActionMapping);
							}
						}

						clonedElement.setProperty(clonedElement.getTraits().key(DOMElementTraitDefinition.TRIGGERED_ACTIONS_PROPERTY), clonedActionMappings);
					}
				}
			}

			return clone;

		} else {

			final PropertyMap properties = new PropertyMap();

			for (final PropertyKey key : getPropertyKeys(PropertyView.Ui)) {

				// skip blacklisted properties
				if (cloneBlacklist.contains(key.jsonName())) {
					continue;
				}

				if (!key.isUnvalidated()) {

					final Object value = getProperty(key);
					if (value != null) {

						properties.put(key, getProperty(key));
					}
				}
			}

			// htmlView is necessary for the cloning of DOM nodes - otherwise some properties won't be cloned
			for (final PropertyKey key : getPropertyKeys(PropertyView.Html)) {

				// skip blacklisted properties
				if (cloneBlacklist.contains(key.jsonName())) {
					continue;
				}

				if (!key.isUnvalidated()) {

					final Object value = getProperty(key);
					if (value != null) {

						properties.put(key, getProperty(key));
					}
				}
			}

			// also clone data-* attributes
			for (final PropertyKey key : getDataPropertyKeys()) {

				// skip blacklisted properties
				if (cloneBlacklist.contains(key.jsonName())) {
					continue;
				}

				if (!key.isUnvalidated()) {

					final Object value = getProperty(key);
					if (value != null) {

						properties.put(key, getProperty(key));
					}
				}
			}

			if (traits.contains(StructrTraits.LINK_SOURCE)) {

				final LinkSource linkSourceElement = this.as(LinkSource.class);
				final Traits linkSourceTraits      = Traits.of(StructrTraits.LINK_SOURCE);

				properties.put(linkSourceTraits.key(LinkSourceTraitDefinition.LINKABLE_PROPERTY), linkSourceElement.getLinkable());
			}

			final App app = StructrApp.getInstance(securityContext);

			try {

				final NodeInterface clone = app.create(getType(), properties);

				// for clone, always copy permissions
				copyPermissionsTo(securityContext, clone, true);

				return clone.as(DOMNode.class);

			} catch (FrameworkException ex) {

				throw new FrameworkException(422, ex.toString());
			}
		}
	}

	/*
	public final void normalize() throws FrameworkException {

		final Page document = getOwnerDocument();
		if (document != null) {

			// merge adjacent text nodes until there is only one left
			DOMNode child = getFirstChild();
			while (child != null) {

				if (child.is(StructrTraits.CONTENT)) {

					DOMNode next = child.getNextSibling();
					if (next != null && next.is(StructrTraits.CONTENT)) {

						String text1 = child.as(Content.class).getContent();
						String text2 = next.as(Content.class).getContent();

						// create new text node
						final Content newText = document.createTextNode(text1.concat(text2));

						removeChild(child);
						insertBefore(newText, next);
						removeChild(next);

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
			if (hasChildNodes()) {

				DOMNode currentChild = getFirstChild();
				while (currentChild != null) {

					currentChild.normalize();
					currentChild = currentChild.getNextSibling();
				}
			}
		}
	}
	*/

	@Override
	public void setHidden(final boolean hidden) throws FrameworkException {
		wrappedObject.setHidden(hidden);
	}

	@Override
	public void setIdAttribute(final String id) throws FrameworkException {
		wrappedObject.setProperty(traits.key(DOMElementTraitDefinition._HTML_ID_PROPERTY), id);
	}
}
