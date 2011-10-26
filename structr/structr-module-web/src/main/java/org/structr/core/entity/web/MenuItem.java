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

import org.neo4j.graphdb.Direction;

import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.renderer.RenderContext;
import org.structr.core.Command;
import org.structr.core.EntityContext;
import org.structr.core.Services;
import org.structr.core.converter.NodeIdNodeConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.entity.SuperUser;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.DeleteRelationshipCommand;
import org.structr.core.node.FindNodeCommand;
import org.structr.core.node.NodeFactoryCommand;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author amorgner
 *
 */
public class MenuItem extends WebNode {

	static {

		EntityContext.registerPropertySet(MenuItem.class,
						  PropertyView.All,
						  Key.values());
	}

	//~--- constant enums -------------------------------------------------

	public enum Key implements PropertyKey{ linkedPage, linkTarget; }

	//~--- methods --------------------------------------------------------

	@Override
	public boolean renderingAllowed(final RenderContext context) {
		return true;
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getIconSrc() {
		return "/images/page_link.png";
	}

//
//      public String getLinkTarget() {
//          return (String) getProperty(LINK_TARGET_KEY);
//      }
//
//      public void setLinkTarget(final String linkTarget) {
//          setProperty(LINK_TARGET_KEY, linkTarget);
//      }
	public Page getLinkedPage() {

		if (hasRelationship(RelType.PAGE_LINK, Direction.OUTGOING)) {

			Command nodeFactory = Services.command(securityContext,
				NodeFactoryCommand.class);

			return ((Page) nodeFactory.execute(getRelationships(RelType.PAGE_LINK,
				Direction.OUTGOING).get(0).getEndNode()));

		} else {
			return null;
		}
	}

	public Long getLinkTarget() {

		Page n = getLinkedPage();

		return ((n != null)
			? n.getId()
			: null);
	}

	//~--- set methods ----------------------------------------------------

	public void setLinkTarget(final Long value) {

		// find link target node
		Command findNode            = Services.command(securityContext,
			FindNodeCommand.class);
		AbstractNode linkTargetNode = (AbstractNode) findNode.execute(value);

		// delete existing link target relationships
		List<StructrRelationship> pageLinkRels = getRelationships(RelType.PAGE_LINK,
			Direction.OUTGOING);
		Command delRel                         = Services.command(securityContext,
			DeleteRelationshipCommand.class);

		if (pageLinkRels != null) {

			for (StructrRelationship r : pageLinkRels) {
				delRel.execute(r);
			}
		}

		// create new link target relationship
		Command createRel = Services.command(securityContext,
			CreateRelationshipCommand.class);

		createRel.execute(this,
				  linkTargetNode,
				  RelType.PAGE_LINK);
	}
}
