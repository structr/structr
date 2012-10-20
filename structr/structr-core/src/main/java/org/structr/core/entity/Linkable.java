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

import java.util.List;
import org.neo4j.graphdb.Direction;
import org.structr.common.*;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.notion.PropertyNotion;

//~--- interfaces -------------------------------------------------------------

/**
 *
 * @author axel
 */
public interface Linkable extends GraphObject {

	public static final Property<List<AbstractNode>> linkingElements = new Property<List<AbstractNode>>("linkingElements");

	public static final View uiView = new View(PropertyView.Ui, linkingElements);
	
	//~--- inner classes --------------------------------------------------

	static class Impl {

		static {

//			EntityContext.registerPropertySet(Linkable.class, PropertyView.Ui, linkingElements);
			EntityContext.registerPropertyRelation(Linkable.class, Linkable.linkingElements, AbstractNode.class, RelType.LINK, Direction.INCOMING, RelationClass.Cardinality.OneToMany, new PropertyNotion(AbstractNode.uuid));

		}

	}

}
