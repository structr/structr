/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.core.entity.web;

import org.apache.commons.lang.StringUtils;

import org.structr.common.RenderMode;
import org.structr.common.renderer.ExternalTemplateRenderer;
import org.structr.common.renderer.RenderContext;
import org.structr.core.Command;
import org.structr.core.EntityContext;
import org.structr.core.NodeRenderer;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.NodeFactoryCommand;
import org.structr.core.node.search.Search;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author axel
 */
public class WebNode extends AbstractNode {

	private static final Logger logger = Logger.getLogger(AbstractNode.class.getName());

	//~--- static initializers --------------------------------------------

	static {

		EntityContext.registerPropertySet(WebNode.class,
						  PropertyView.All,
						  Key.values());
	}

	//~--- constant enums -------------------------------------------------

	public enum Key implements PropertyKey{ sessionBlocked, username; }

	//~--- methods --------------------------------------------------------

	@Override
	public void initializeRenderers(Map<RenderMode, NodeRenderer> renderers) {

		renderers.put(RenderMode.Default,
			      new ExternalTemplateRenderer(true));
	}

	@Override
	public boolean renderingAllowed(final RenderContext context) {
		return true;
	}

	//~--- get methods ----------------------------------------------------

	/**
	 * Use normalized string for indexing of name and title.
	 *
	 * @param key
	 * @return
	 */
	@Override
	public Object getPropertyForIndexing(final String key) {

		if (AbstractNode.Key.name.name().equals(key) || AbstractNode.Key.title.name().equals(key)) {

			String name = (String) getStringProperty(key);

			if (name != null) {

				String normalizedName = Search.normalize(name);

				if ((normalizedName != null) &&!(name.equals(normalizedName))) {
					return name.concat(" ").concat(normalizedName);
				}
			}
		}

		return getProperty(key);
	}

	/**
	 * Traverse over all child nodes to find a home page
	 */
	public HomePage getHomePage() {

		List<AbstractNode> childNodes = getAllChildren(HomePage.class.getSimpleName());

		for (AbstractNode node : childNodes) {

			if (node instanceof HomePage) {
				return ((HomePage) node);
			}
		}

		logger.log(Level.FINE,
			   "No home page found for node {0}",
			   this.getId());

		return null;
	}

	/**
	 * Return the node of next ancestor site (or domain, if no site exists),
	 * or the root node, if no domain or site is in the path.
	 *
	 * @return
	 */
	@Override
	public AbstractNode getContextNode() {

		List<AbstractNode> ancestors = getAncestorNodes();

		// If node has no ancestors, itself is its context node
		if (ancestors.isEmpty()) {
			return this;
		}

		for (AbstractNode n : ancestors) {

			if ((n instanceof Site) || (n instanceof Domain)) {
				return n;
			}
		}

		// Return root node
		return ancestors.get(0);
	}

	/**
	 * Return the node path of next ancestor site (or domain, if no site exists),
	 * or the root node, if no domain or site is in the
	 * path.
	 *
	 * @return
	 */
	@Override
	public String getContextPath() {

		int sublevel                 = 0;
		List<AbstractNode> ancestors = getAncestorNodes();

		for (AbstractNode n : ancestors) {

			if ((n instanceof Site) || (n instanceof Domain)) {
				break;
			}

			sublevel++;
		}

		StringBuilder path = new StringBuilder();

		for (int i = 0; i < sublevel; i++) {
			path.append("../");
		}

		return path.toString();
	}

	/**
	 * Assemble URL for this node.
	 *
	 * This is an inverse method of @getNodeByIdOrPath.
	 *
	 * @param renderMode
	 * @param contextPath
	 * @return
	 */
	public String getNodeURL(HttpServletRequest request, final Enum renderMode, final String contextPath) {

		String domain = "";
		String site   = "";
		String path   = "";

		if (RenderMode.PUBLIC.equals(renderMode)) {

			// create bean node
			Command nodeFactory = Services.command(securityContext, NodeFactoryCommand.class);
			AbstractNode node   = (AbstractNode) nodeFactory.execute(this);

			// stop at root node
			while ((node != null) && (node.getId() > 0)) {

				String urlPart = node.getName();

				if (urlPart != null) {

					if (node instanceof Site) {
						site = urlPart;
					} else if (node instanceof Domain) {
						domain = urlPart;
					} else {
						path = urlPart;
					}
				}

				// check parent nodes
				node = node.getParentNode();

//                              StructrRelationship r = node.getRelationships(RelType.HAS_CHILD, Direction.INCOMING).get(0);
//                              if (r != null) {
//                                  node = r.getStartNode();
//                              }
			}

			String scheme = request.getScheme();

			return scheme + "://" + site + (StringUtils.isNotEmpty(site)
							? "."
							: "") + domain + "/" + path;
		} else if (RenderMode.LOCAL.equals(renderMode)) {

			// assemble relative URL following the pattern
			// /<context-url>/view.htm?nodeId=<path>
			// TODO: move this to a better place
			// TODO: improve handling for renderMode
			String localUrl = contextPath.concat(getNodePath()).concat("&renderMode=local");

			return localUrl;
		} else {

			// TODO implement other modes
			return null;
		}
	}

	public String getNodeURL(HttpServletRequest request, final String contextPath) {

		return getNodeURL(request, RenderMode.PUBLIC,
				  contextPath);
	}

	@Override
	public String getIconSrc() {
		return "/images/folder.png";
	}
}
