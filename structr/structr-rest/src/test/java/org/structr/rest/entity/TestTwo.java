/*
 *  Copyright (C) 2012 Axel Morgner
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
package org.structr.rest.entity;

import org.neo4j.graphdb.Direction;
import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.core.EntityContext;
import org.structr.core.converter.DateConverter;
import org.structr.core.converter.IntConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.RelationClass.Cardinality;
import org.structr.core.node.NodeService.NodeIndex;

/**
 * Another simple entity for the most basic tests.
 * 
 * @author Axel Morgner
 */
public class TestTwo extends AbstractNode {
	
	public enum Key implements PropertyKey {
		
		name, anInt, aLong, aDate
		
	}
	
	static {
		
		EntityContext.registerEntityRelation(TestTwo.class, TestOne.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.OneToMany);
		
		EntityContext.registerPropertySet(TestTwo.class, PropertyView.Public, Key.values());
		
		EntityContext.registerPropertyConverter(TestTwo.class, Key.anInt, IntConverter.class);
		EntityContext.registerPropertyConverter(TestTwo.class, Key.aDate, DateConverter.class);
		
		EntityContext.registerSearchablePropertySet(TestTwo.class, NodeIndex.fulltext.name(), Key.values());
		EntityContext.registerSearchablePropertySet(TestTwo.class, NodeIndex.keyword.name(), Key.values());
	}
		
}
