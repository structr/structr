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

import org.structr.common.error.FrameworkException;
import org.structr.core.api.AbstractMethod;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.definitions.AbstractTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.dom.*;
import org.structr.web.traits.operations.CheckHierarchy;
import org.structr.web.traits.operations.HandleNewChild;
import org.structr.web.traits.operations.Render;

import java.util.Map;
import java.util.Set;

import static org.structr.web.entity.dom.DOMNode.PAGE_CATEGORY;

/**
 * Represents a page resource.
 */
public class PageTraitDefinition extends AbstractTraitDefinition {

	/*
	public static final View defaultView = new View(Page.class, PropertyView.Public,
		linkingElementsProperty, enableBasicAuthProperty, basicAuthRealmProperty, dontCacheProperty, childrenProperty, name, owner, sitesProperty,
		isPageProperty, pageCreatesRawDataProperty, versionProperty, positionProperty, cacheForSecondsProperty, pathProperty,
		showOnErrorCodesProperty, contentTypeProperty, categoryProperty, pathsProperty
	);

	public static final View uiView = new View(Page.class, PropertyView.Ui,
		isPageProperty, pageCreatesRawDataProperty, dontCacheProperty, childrenProperty, sitesProperty, versionProperty, positionProperty, cacheForSecondsProperty,
		pathProperty, showOnErrorCodesProperty, contentTypeProperty, categoryProperty, pathsProperty
	);

	public static final View categoryView = new View(Page.class, "category",
		categoryProperty
	);
	*/

	public PageTraitDefinition() {
		super("Page");
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {
		return Map.of();
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			Render.class,
			new Render() {

				@Override
				public void render(final DOMNode node, final RenderContext renderContext, final int depth) throws FrameworkException {

					final Page page = node.as(Page.class);

					renderContext.setPage(page);

					DOMNode subNode = node.getFirstChild();

					// output doctype definition only if first child is not a template
					if (subNode.is("Html")) {
						renderContext.getBuffer().append("<!DOCTYPE html>\n");
					}

					while (subNode != null) {

						if (renderContext.getSecurityContext().isVisible(subNode.getWrappedNode())) {

							subNode.render(renderContext, depth);
						}

						subNode = subNode.getNextSibling();

					}
				}
			},

			HandleNewChild.class,
			new HandleNewChild() {

				@Override
				public void handleNewChild(final DOMNode node, final DOMNode newChild) throws FrameworkException {

					final Page thisPage = node.as(Page.class);

					newChild.setOwnerDocument(thisPage);

					for (final NodeInterface child : newChild.getAllChildNodes()) {

						child.as(DOMNode.class).setOwnerDocument(thisPage);

					}
				}
			},

			CheckHierarchy.class,
			new CheckHierarchy() {

				@Override
				public void checkHierarchy(DOMNode thisNode, DOMNode otherNode) throws FrameworkException {

					/*
					// verify that this document has only one document element
					if (thisNode.getDocumentElement() != null) {
						throw new FrameworkException(422, HIERARCHY_REQUEST_ERR_MESSAGE_DOCUMENT);
					}
					*/

					if (!(otherNode.is("Html") || otherNode.is("Comment") || otherNode.is("Template"))) {

						throw new FrameworkException(422, DOMNode.HIERARCHY_REQUEST_ERR_MESSAGE_ELEMENT);
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
		return Map.of();
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {
		return Set.of();
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Iterable<NodeInterface>> elementsProperty = new StartNodes("elements", "DOMNodePAGEPage").category(PAGE_CATEGORY).partOfBuiltInSchema();
		final Property<Iterable<NodeInterface>> pathsProperty   = new EndNodes("paths", "PageHAS_PATHPagePath").partOfBuiltInSchema();
		final Property<Iterable<NodeInterface>> sitesProperty   = new StartNodes("sites", "SiteCONTAINSPage").partOfBuiltInSchema();

		final Property<Boolean> isPageProperty             = new ConstantBooleanProperty("isPage", true).partOfBuiltInSchema();
		final Property<Boolean> pageCreatesRawDataProperty = new BooleanProperty("pageCreatesRawData").defaultValue(false).partOfBuiltInSchema();

		final Property<Integer> versionProperty         = new IntProperty("version").indexed().readOnly().defaultValue(0).partOfBuiltInSchema();
		final Property<Integer> positionProperty        = new IntProperty("position").indexed().partOfBuiltInSchema();
		final Property<Integer> cacheForSecondsProperty = new IntProperty("cacheForSeconds").partOfBuiltInSchema();

		final Property<String> pathProperty             = new StringProperty("path").indexed().partOfBuiltInSchema();
		final Property<String> showOnErrorCodesProperty = new StringProperty("showOnErrorCodes").indexed().partOfBuiltInSchema();
		final Property<String> contentTypeProperty      = new StringProperty("contentType").indexed().partOfBuiltInSchema();
		final Property<String> categoryProperty         = new StringProperty("category").indexed().partOfBuiltInSchema();

		return Set.of(
			elementsProperty,
			pathsProperty,
			sitesProperty,
			isPageProperty,
			pageCreatesRawDataProperty,
			versionProperty,
			positionProperty,
			cacheForSecondsProperty,
			pathProperty,
			showOnErrorCodesProperty,
			contentTypeProperty,
			categoryProperty
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	/*
	public Node importNode(final Node node, final boolean deep, final boolean removeParentFromSourceNode) throws DOMException {

		if (node instanceof DOMNode) {

			final DOMNode domNode = (DOMNode) node;

			// step 1: use type-specific import impl.
			Node importedNode = domNode.doImport(this);

			// step 2: do recursive import?
			if (deep && domNode.hasChildNodes()) {

				// FIXME: is it really a good idea to do the
				// recursion inside of a transaction?
				Node child = domNode.getFirstChild();

				while (child != null) {

					// do not remove parent for child nodes
					importNode(child, deep, false);
					child = child.getNextSibling();

					final Logger logger = LoggerFactory.getLogger(Page.class);
					logger.info("sibling is {}", child);
				}

			}

			// step 3: remove node from its current parent
			// (Note that this step needs to be done last in
			// (order for the child to be able to find its
			// siblings.)
			if (removeParentFromSourceNode) {

				// only do this for the actual source node, do not remove
				// child nodes from its parents
				Node _parent = domNode.getParentNode();
				if (_parent != null) {
					_parent.removeChild(domNode);
				}
			}

			return importedNode;

		}

		return null;
	}

	public Node adoptNode(final Node node, final boolean removeParentFromSourceNode) throws DOMException {

		if (node instanceof DOMNode) {

			final DOMNode domNode = (DOMNode) node;

			// step 2: do recursive import?
			if (domNode.hasChildNodes()) {

				Node child = domNode.getFirstChild();
				while (child != null) {

					// do not remove parent for child nodes
					adoptNode(child, false);
					child = child.getNextSibling();
				}

			}

			// step 3: remove node from its current parent
			// (Note that this step needs to be done last in
			// (order for the child to be able to find its
			// siblings.)
			if (removeParentFromSourceNode) {

				// only do this for the actual source node, do not remove
				// child nodes from its parents
				Node _parent = domNode.getParentNode();
				if (_parent != null) {
					_parent.removeChild(domNode);
				}
			}

			// step 1: use type-specific adopt impl.
			Node adoptedNode = domNode.doAdopt(this);

			return adoptedNode;

		}

		return null;
	}

	public Element getDocumentElement() {

		Node node = this.treeGetFirstChild();

		if (node instanceof Element) {

			return (Element) node;

		} else {

			return null;
		}
	}
	*/
}
