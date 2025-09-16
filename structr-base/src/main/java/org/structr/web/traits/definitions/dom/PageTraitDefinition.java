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

import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.api.AbstractMethod;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.traits.definitions.LinkableTraitDefinition;
import org.structr.web.traits.operations.CheckHierarchy;
import org.structr.web.traits.operations.HandleNewChild;
import org.structr.web.traits.operations.Render;
import org.structr.web.traits.wrappers.dom.PageTraitWrapper;

import java.util.Map;
import java.util.Set;

import static org.structr.web.entity.dom.DOMNode.PAGE_CATEGORY;

/**
 * Represents a page resource.
 */
public class PageTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String ELEMENTS_PROPERTY              = "elements";
	public static final String PATHS_PROPERTY                 = "paths";
	public static final String SITES_PROPERTY                 = "sites";
	public static final String IS_PAGE_PROPERTY               = "isPage";
	public static final String PAGE_CREATES_RAW_DATA_PROPERTY = "pageCreatesRawData";
	public static final String VERSION_PROPERTY               = "version";
	public static final String POSITION_PROPERTY              = "position";
	public static final String CACHE_FOR_SECONDS_PROPERTY     = "cacheForSeconds";
	public static final String PATH_PROPERTY                  = "path";
	public static final String SHOW_ON_ERROR_CODES_PROPERTY   = "showOnErrorCodes";
	public static final String CONTENT_TYPE_PROPERTY          = "contentType";
	public static final String CATEGORY_PROPERTY              = "category";

	public PageTraitDefinition() {
		super(StructrTraits.PAGE);
	}

	@Override
	public Map<Class, LifecycleMethod> createLifecycleMethods(TraitsInstance traitsInstance) {
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

					DOMNode.prefetchDOMNodes(page.getUuid());

					renderContext.setPage(page);

					for (final DOMNode subNode : node.getChildren()) {

						// output doctype definition only if first child is not a template
						if (subNode.is(StructrTraits.HTML)) {
							renderContext.getBuffer().append("<!DOCTYPE html>\n");
						}

						if (renderContext.getSecurityContext().isVisible(subNode)) {

							subNode.render(renderContext, depth);
						}
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

					if (!(otherNode.is(StructrTraits.HTML) || otherNode.is(StructrTraits.COMMENT) || otherNode.is(StructrTraits.TEMPLATE))) {

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

		return Map.of(
			Page.class, (traits, node) -> new PageTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {
		return Set.of();
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<Iterable<NodeInterface>> elementsProperty = new StartNodes(traitsInstance, ELEMENTS_PROPERTY, StructrTraits.DOM_NODE_PAGE_PAGE).category(PAGE_CATEGORY);
		final Property<Iterable<NodeInterface>> pathsProperty    = new EndNodes(traitsInstance, PATHS_PROPERTY, StructrTraits.PAGE_HAS_PATH_PAGE_PATH);
		final Property<Iterable<NodeInterface>> sitesProperty    = new StartNodes(traitsInstance, SITES_PROPERTY, StructrTraits.SITE_CONTAINS_PAGE);

		final Property<Boolean> isPageProperty                   = new ConstantBooleanProperty(IS_PAGE_PROPERTY, true);
		final Property<Boolean> pageCreatesRawDataProperty       = new BooleanProperty(PAGE_CREATES_RAW_DATA_PROPERTY).defaultValue(false);

		final Property<Integer> versionProperty                  = new IntProperty(VERSION_PROPERTY).indexed().readOnly().defaultValue(0);
		final Property<Integer> positionProperty                 = new IntProperty(POSITION_PROPERTY).indexed();
		final Property<Integer> cacheForSecondsProperty          = new IntProperty(CACHE_FOR_SECONDS_PROPERTY);

		final Property<String> pathProperty                      = new StringProperty(PATH_PROPERTY).indexed();
		final Property<String> showOnErrorCodesProperty          = new StringProperty(SHOW_ON_ERROR_CODES_PROPERTY).indexed();
		final Property<String> contentTypeProperty               = new StringProperty(CONTENT_TYPE_PROPERTY).indexed();
		final Property<String> categoryProperty                  = new StringProperty(CATEGORY_PROPERTY).indexed();

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
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
					IS_PAGE_PROPERTY, PAGE_CREATES_RAW_DATA_PROPERTY, SITES_PROPERTY, VERSION_PROPERTY, POSITION_PROPERTY,
					CACHE_FOR_SECONDS_PROPERTY, PATH_PROPERTY, SHOW_ON_ERROR_CODES_PROPERTY, CONTENT_TYPE_PROPERTY, CATEGORY_PROPERTY, PATHS_PROPERTY,
					DOMNodeTraitDefinition.DONT_CACHE_PROPERTY, DOMNodeTraitDefinition.CHILDREN_PROPERTY,
					LinkableTraitDefinition.LINKING_ELEMENTS_PROPERTY, LinkableTraitDefinition.ENABLE_BASIC_AUTH_PROPERTY,
					LinkableTraitDefinition.BASIC_AUTH_REALM_PROPERTY, NodeInterfaceTraitDefinition.OWNER_PROPERTY
			),

			PropertyView.Ui,
			newSet(
					IS_PAGE_PROPERTY, PAGE_CREATES_RAW_DATA_PROPERTY, SITES_PROPERTY, VERSION_PROPERTY, POSITION_PROPERTY,
					CACHE_FOR_SECONDS_PROPERTY, PATH_PROPERTY, SHOW_ON_ERROR_CODES_PROPERTY, CONTENT_TYPE_PROPERTY, CATEGORY_PROPERTY, PATHS_PROPERTY,
					DOMNodeTraitDefinition.DONT_CACHE_PROPERTY, DOMNodeTraitDefinition.CHILDREN_PROPERTY
			),

			"category",
			newSet(
				CATEGORY_PROPERTY
			)
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
