/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.web.entity;

import java.util.List;
import org.structr.common.PropertyView;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.notion.PropertyNotion;
import org.structr.core.property.Property;
import org.structr.core.property.StartNodes;
import org.structr.web.entity.html.Link;
import org.structr.web.entity.html.relation.ResourceLink;

//~--- interfaces -------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public interface Linkable extends NodeInterface {

	public static final Property<List<Link>> linkingElements = new StartNodes<>("linkingElements", ResourceLink.class, new PropertyNotion(AbstractNode.id));

	public static final org.structr.common.View uiView = new org.structr.common.View(Linkable.class, PropertyView.Ui, linkingElements);
}
