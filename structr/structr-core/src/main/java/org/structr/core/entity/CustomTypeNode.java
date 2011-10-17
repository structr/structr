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
package org.structr.core.entity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.neo4j.graphdb.Direction;
import org.structr.common.PropertyKey;
import org.structr.common.RelType;
import org.structr.common.RenderMode;
import org.structr.common.renderer.ExternalTemplateRenderer;
import org.structr.core.Command;
import org.structr.core.NodeRenderer;
import org.structr.core.Services;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.DeleteRelationshipCommand;
import org.structr.core.node.FindNodeCommand;

/**
 * 
 * @author amorgner
 * 
 */
public class CustomTypeNode extends AbstractNode {

	private final static String ICON_SRC = "/images/error.png";
	
	public enum Key implements PropertyKey {
		iconSrc, typeNodeId;
	}
	
	private NodeType typeNode;

	@Override
	public String getType() {
		typeNode = getTypeNode();
		if (typeNode != null) {
			return typeNode.getName();
		} else {
			return super.getType();
		}
	}

	@Override
	public String getIconSrc() {

		if (typeNode == null) {
			typeNode = getTypeNode();
		}

		Object iconSrc = null;
		if (typeNode != null) {
			iconSrc = typeNode.getProperty(Key.iconSrc.name());
		}

		if (iconSrc != null && iconSrc instanceof String) {
			return (String) iconSrc;
		} else {
			return ICON_SRC;
		}
	}

	@Override
	public Iterable<AbstractNode> getDataNodes() {

		// CustomTypeNode nodes should not return their child nodes
		return null;
	}

	@Override
	public Iterable<String> getPropertyKeys() {

		if (typeNode == null) {
			typeNode = getTypeNode();
		}

		if (typeNode != null) {
			return typeNode.getPropertyKeys();
		}
		return Collections.EMPTY_LIST;
	}

	@Override
	public void initializeRenderers(Map<RenderMode, NodeRenderer> renderers) {
		renderers.put(RenderMode.Default, new ExternalTemplateRenderer(false));
	}

	public Long getTypeNodeId() {
		NodeType n = getTypeNode();
		return (n != null ? n.getId() : null);
	}

	/**
	 * Return (cached) type node
	 *
	 * @return
	 */
	public NodeType getTypeNode() {

		if (typeNode != null) {
			return typeNode;
		}

		for (StructrRelationship s : getRelationships(RelType.TYPE, Direction.OUTGOING)) {
			AbstractNode n = s.getEndNode();
			if (n instanceof NodeType) {
				return (NodeType) n;
			}
		}
		return null;
	}

	public void setTypeNodeId(final Long value) {

		// find type node
		Command findNode = Services.command(FindNodeCommand.class);
		NodeType newTypeNode = (NodeType) findNode.execute(new SuperUser(), value);

		// delete existing type node relationships
		List<StructrRelationship> templateRels = this.getOutgoingRelationships(RelType.TYPE);
		Command delRel = Services.command(DeleteRelationshipCommand.class);
		if (templateRels != null) {
			for (StructrRelationship r : templateRels) {
				delRel.execute(r);
			}
		}

		// create new link target relationship
		Command createRel = Services.command(CreateRelationshipCommand.class);
		createRel.execute(this, newTypeNode, RelType.TYPE);
	}
}
