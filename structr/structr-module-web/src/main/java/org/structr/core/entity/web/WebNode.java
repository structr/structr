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

import org.structr.common.CurrentRequest;
import org.structr.common.RenderMode;
import org.structr.common.renderer.ExternalTemplateRenderer;
import org.structr.common.renderer.RenderContext;
import org.structr.core.Command;
import org.structr.core.NodeRenderer;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.ArbitraryNode;
import org.structr.core.node.NodeFactoryCommand;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author axel
 */
public class WebNode extends ArbitraryNode {

	public final static String SESSION_BLOCKED = "sessionBlocked";
	public final static String USERNAME_KEY    = "username";
	private static final Logger logger         = Logger.getLogger(AbstractNode.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void initializeRenderers(Map<RenderMode, NodeRenderer> renderers) {
		renderers.put(RenderMode.Default, new ExternalTemplateRenderer(true));
	}

	@Override
	public void onNodeCreation() {}

	@Override
	public void onNodeInstantiation() {}

	@Override
	public boolean renderingAllowed(final RenderContext context) {
		return true;
	}

	//~--- get methods ----------------------------------------------------

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

		logger.log(Level.FINE, "No home page found for node {0}", this.getId());

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
	public String getNodeURL(final Enum renderMode, final String contextPath) {

		String domain = "";
		String site   = "";
		String path   = "";

		if (RenderMode.PUBLIC.equals(renderMode)) {

			// create bean node
			Command nodeFactory = Services.command(NodeFactoryCommand.class);
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

			String scheme = CurrentRequest.getRequest().getScheme();

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

	public String getNodeURL(final String contextPath) {
		return getNodeURL(RenderMode.PUBLIC, contextPath);
	}

	@Override
	public String getIconSrc() {
		return "/images/folder.png";
	}
}
