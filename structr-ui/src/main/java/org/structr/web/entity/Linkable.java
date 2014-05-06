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
import org.structr.common.PropertyView;
import org.structr.common.Syncable;
import org.structr.core.GraphObject;
import org.structr.core.graph.NodeInterface;
import org.structr.core.notion.PropertyNotion;
import org.structr.core.property.Property;
import org.structr.core.property.StartNodes;
import org.structr.web.entity.html.relation.ResourceLink;

//~--- interfaces -------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public interface Linkable extends NodeInterface, Syncable {

	public static final Property<List<LinkSource>> linkingElements = new StartNodes<>("linkingElements", ResourceLink.class, new PropertyNotion(GraphObject.id));

	public static final org.structr.common.View uiView = new org.structr.common.View(Linkable.class, PropertyView.Ui, linkingElements);

	public String getPath();
}
