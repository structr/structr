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
package org.structr.core.entity;

import org.neo4j.graphdb.Direction;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.core.EntityContext;

/**
 * A simple entity for the most basic tests.
 * 
 * 
 * @author Axel Morgner
 */
public class TestOne extends AbstractNode {
	
	static {
		
		EntityContext.registerPropertySet(TestOne.class, PropertyView.Public, Key.values());
		EntityContext.registerEntityRelation(TestOne.class, TestTwo.class, RelType.UNDEFINED, Direction.OUTGOING, RelationClass.Cardinality.OneToOne, RelationClass.DELETE_IF_CONSTRAINT_WOULD_BE_VIOLATED);
		EntityContext.registerEntityRelation(TestOne.class, TestThree.class, RelType.LINK, Direction.OUTGOING, RelationClass.Cardinality.OneToOne, RelationClass.DELETE_IF_CONSTRAINT_WOULD_BE_VIOLATED);
		EntityContext.registerEntityRelation(TestOne.class, TestFour.class, RelType.DATA, Direction.OUTGOING, RelationClass.Cardinality.OneToOne, RelationClass.DELETE_IF_CONSTRAINT_WOULD_BE_VIOLATED);
	}
	
}
