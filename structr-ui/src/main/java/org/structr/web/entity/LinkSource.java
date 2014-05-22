/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.entity;

import java.util.List;
import org.structr.common.Syncable;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.notion.PropertyNotion;
import org.structr.core.property.EndNode;
import org.structr.core.property.EntityIdProperty;
import org.structr.core.property.Property;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.html.relation.ResourceLink;

/**
 * This class represents elements which can have an outgoing link to a resource.
 *
 * @author Axel Morgner
 */
public class LinkSource extends DOMElement {

	public static final Property<Linkable> linkable = new EndNode<>("linkable", ResourceLink.class, new PropertyNotion(AbstractNode.name));
	public static final Property<String> linkableId = new EntityIdProperty("linkableId", linkable);

	// ----- interface Syncable -----
	@Override
	public List<Syncable> getSyncData() {

		final List<Syncable> data = super.getSyncData();

		data.add(getProperty(linkable));

		for (final ResourceLink link : getRelationships(ResourceLink.class)) {
			data.add(link);
		}

		return data;
	}

	@Override
	public boolean isNode() {
		return true;
	}

	@Override
	public boolean isRelationship() {
		return false;
	}

	@Override
	public NodeInterface getSyncNode() {
		return this;
	}

	@Override
	public RelationshipInterface getSyncRelationship() {
		return null;
	}

}
