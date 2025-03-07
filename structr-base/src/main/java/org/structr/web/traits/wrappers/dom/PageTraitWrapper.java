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
package org.structr.web.traits.wrappers.dom;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.web.common.RenderContext;
import org.structr.web.common.StringRenderBuffer;
import org.structr.web.entity.Site;
import org.structr.web.entity.dom.*;
import org.structr.web.entity.path.PagePath;
import org.structr.web.importer.Importer;
import org.structr.web.maintenance.deploy.DeploymentCommentHandler;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Represents a page resource.
 */
public class PageTraitWrapper extends DOMNodeTraitWrapper implements Page {

	public PageTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public void setVersion(final int version) throws FrameworkException {
		wrappedObject.setProperty(traits.key("version"), version);
	}

	@Override
	public void increaseVersion() throws FrameworkException {

		final Integer _version = this.getVersion();

		wrappedObject.unlockReadOnlyPropertiesOnce();
		if (_version == null) {

			setVersion(1);

		} else {

			this.setVersion(_version + 1);
		}
	}

	@Override
	public int getVersion() {
		return wrappedObject.getProperty(traits.key("version"));
	}

	@Override
	public Integer getCacheForSeconds() {
		return wrappedObject.getProperty(traits.key("cacheForSeconds"));
	}

	@Override
	public Integer getPosition() {
		return wrappedObject.getProperty(traits.key("position"));
	}

	@Override
	public String getPath() {
		return wrappedObject.getProperty(traits.key("path"));
	}

	@Override
	public String getCategory() {
		return wrappedObject.getProperty(traits.key("category"));
	}

	@Override
	public final String getContentType() {
		return wrappedObject.getProperty(traits.key("contentType"));
	}

	@Override
	public String getShowOnErrorCodes() {
		return wrappedObject.getProperty(traits.key("showOnErrorCodes"));
	}

	@Override
	public boolean pageCreatesRawData() {
		return wrappedObject.getProperty(traits.key("pageCreatesRawData"));
	}

	@Override
	public Iterable<DOMNode> getElements() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key("elements");

		return Iterables.map(n -> n.as(DOMNode.class), wrappedObject.getProperty(key));
	}

	@Override
	public Iterable<PagePath> getPaths() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key("paths");

		return Iterables.map(n -> n.as(PagePath.class), wrappedObject.getProperty(key));
	}

	@Override
	public Iterable<Site> getSites() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key("sites");

		return Iterables.map(n -> n.as(Site.class), wrappedObject.getProperty(key));
	}

	public void setContent(final Map<String, Object> parameters, final SecurityContext ctx) throws FrameworkException {

		final String content = (String)parameters.get("content");
		if (content == null) {

			throw new FrameworkException(422, "Cannot set content of page " + this.getName() + ", no content provided");
		}

		final Importer importer = new Importer(this.getSecurityContext(), content, null, null, false, false, false, false);
		final App app           = StructrApp.getInstance(ctx);

		importer.setIsDeployment(true);
		importer.setCommentHandler(new DeploymentCommentHandler());

		if (importer.parse(false)) {

			for (final NodeInterface node : this.getAllChildNodes()) {
				app.delete(node);
			}

			importer.createChildNodesWithHtml(this, this, true);
		}
	}

	public String getContent(final Page page, final RenderContext.EditMode editMode) throws FrameworkException {

		DOMNode.prefetchDOMNodes(page.getUuid());

		final RenderContext ctx = new RenderContext(page.getSecurityContext(), null, null, editMode);
		final StringRenderBuffer buffer = new StringRenderBuffer();
		ctx.setBuffer(buffer);
		page.render(ctx, 0);

		// extract source
		return buffer.getBuffer().toString();
	}

	@Override
	public DOMElement createElement(final String tag, final boolean suppressException) {

		final Logger logger = LoggerFactory.getLogger(Page.class);
		final App app       = StructrApp.getInstance(getSecurityContext());
		String elementType  = StringUtils.capitalize(tag);

		// Avoid creating an (invalid) 'Content' DOMElement
		if ((elementType == null) || StructrTraits.CONTENT.equals(elementType)) {

			logger.warn("Blocked attempt to create a DOMElement of type Content");

			return null;

		}

		// Template is already taken => we need to modify the type :(
		if (StructrTraits.TEMPLATE.equals(elementType)) {
			elementType = "TemplateElement";
		}

		final Traits traits;

		if (Traits.exists(elementType)) {

			traits = Traits.of(elementType);

		} else {

			// No HTML type element found so lets try the dynamic DOMElement class
			traits = Traits.of(StructrTraits.DOM_ELEMENT);
		}

		try {

			final DOMElement element = app.create(traits.getName(), new NodeAttribute(traits.key("tag"), tag)).as(DOMElement.class);

			element.doAdopt(this);

			return element;

		} catch (Throwable t) {

			if (!suppressException) {

				logger.error("Unable to instantiate element of type " + elementType, t);
			}
		}

		return null;
	}

	public List<DOMNode> getElementsByTagName(final String tagName) throws FrameworkException {

		List<DOMNode> results = new LinkedList<>();

		DOMNode.collectNodesByPredicate(this.getSecurityContext(), this, results, new Predicate<DOMNode>() {

			@Override
			public boolean accept(final DOMNode obj) {

				if (obj.is(StructrTraits.DOM_ELEMENT)) {

					DOMElement elem = obj.as(DOMElement.class);

					if (tagName.equals(elem.getTag())) {
						return true;
					}
				}

				return false;
			}

		}, 0, false);

		return results;
	}

	public DOMNode importNode(final DOMNode node, final boolean deep) throws FrameworkException {
		return importNode(node, deep, true);
	}

	public DOMNode importNode(final DOMNode node, final boolean deep, final boolean removeParentFromSourceNode) throws FrameworkException {

		final DOMNode domNode = node;

		// step 1: use type-specific import impl.
		DOMNode importedNode = domNode.doImport(this);

		// step 2: do recursive import?
		if (deep && domNode.hasChildNodes()) {

			// FIXME: is it really a good idea to do the
			// recursion inside of a transaction?
			DOMNode child = domNode.getFirstChild();

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
			DOMNode _parent = domNode.getParent();
			if (_parent != null) {
				_parent.removeChild(domNode);
			}
		}

		return importedNode;
	}

	public DOMNode adoptNode(final DOMNode domNode, final boolean removeParentFromSourceNode) throws FrameworkException {

		if (domNode.hasChildNodes()) {

			DOMNode child = domNode.getFirstChild();
			while (child != null) {

				// do not remove parent for child nodes
				adoptNode(child, false);
				child = child.getNextSibling();
			}

		}

		// (Note that this step needs to be done last in
		// (order for the child to be able to find its
		// siblings.)
		if (removeParentFromSourceNode) {

			// only do this for the actual source node, do not remove
			// child nodes from its parents
			DOMNode _parent = domNode.getParent();
			if (_parent != null) {
				_parent.removeChild(domNode);
			}
		}

		return domNode.doAdopt(this);
	}

	@Override
	public DOMElement getElementById(final String id) throws FrameworkException {

		final List<DOMNode> results = new LinkedList<>();

		DOMNode.collectNodesByPredicate(this.getSecurityContext(), this, results, new Predicate<DOMNode>() {

			@Override
			public boolean accept(DOMNode obj) {

				if (obj.is(StructrTraits.DOM_ELEMENT)) {

					final DOMElement elem = obj.as(DOMElement.class);

					if (id.equals(elem.getHtmlId())) {
						return true;
					}
				}

				return false;
			}

		}, 0, true);

		// return first result
		if (results.size() == 1) {
			return results.get(0).as(DOMElement.class);
		}

		return null;
	}

	@Override
	public DOMElement createElement(final String tag) throws FrameworkException {
		return createElement(tag, false);

	}

	@Override
	public Content createTextNode(final String text) {

		try {

			final App app       = StructrApp.getInstance(getSecurityContext());
			final Traits traits = Traits.of(StructrTraits.CONTENT);

			// create new content element
			final Content content = app.create(StructrTraits.CONTENT,
				new NodeAttribute(traits.key("content"), text)
			).as(Content.class);

			content.setOwnerDocument(this);

			return content;

		} catch (FrameworkException fex) {

			fex.printStackTrace();

			// FIXME: what to do with the exception here?
			final Logger logger = LoggerFactory.getLogger(Page.class);
			logger.warn("", fex);
		}

		return null;
	}

	@Override
	public Comment createComment(final String comment) {

		try {

			final App app       = StructrApp.getInstance(getSecurityContext());
			final Traits traits = Traits.of(StructrTraits.CONTENT);

			// create new content element
			final Comment commentNode = app.create(StructrTraits.COMMENT, new NodeAttribute(traits.key("content"), comment)).as(Comment.class);

			commentNode.setOwnerDocument(this);

			return commentNode;

		} catch (FrameworkException fex) {

			// FIXME: what to do with the exception here?
			final Logger logger = LoggerFactory.getLogger(Page.class);
			logger.warn("", fex);
		}

		return null;
	}

	@Override
	public void adoptNode(final DOMNode newHtmlNode) throws FrameworkException {
		adoptNode(newHtmlNode, true);
	}
}
