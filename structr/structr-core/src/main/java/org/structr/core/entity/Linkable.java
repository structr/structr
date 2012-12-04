/*
 *  Copyright (C) 2012 Axel Morgner
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.core.entity;

import org.neo4j.graphdb.Direction;
import org.structr.common.*;
import org.structr.core.GraphObject;
import org.structr.core.notion.PropertyNotion;
import org.structr.core.property.CollectionProperty;

//~--- interfaces -------------------------------------------------------------

/**
 *
 * @author axel
 */
public interface Linkable extends GraphObject {

	public static final CollectionProperty<AbstractNode> linkingElements = new CollectionProperty<AbstractNode>(AbstractNode.class, RelType.LINK, Direction.INCOMING, new PropertyNotion(AbstractNode.uuid));

	public static final View uiView = new View(Linkable.class, PropertyView.Ui, linkingElements);
}
