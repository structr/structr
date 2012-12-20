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

import java.util.Date;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.View;
import org.structr.core.property.ISO8601DateProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.LongProperty;
import org.structr.core.property.Property;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeService.NodeIndex;
import org.structr.core.property.CollectionProperty;

/**
 * Another simple entity for the most basic tests.
 * 
 * @author Axel Morgner
 */
public class TestTwo extends AbstractNode {
	
	public static final Property<Integer> anInt = new IntProperty("anInt");
	public static final Property<Long> aLong    = new LongProperty("aLong");
	public static final Property<Date> aDate    = new ISO8601DateProperty("aDate");
	
	public static final CollectionProperty<TestOne> testOnes = new CollectionProperty<TestOne>("test_ones", TestOne.class, RelType.CONTAINS, true);

	public static final View publicView = new View(TestTwo.class, PropertyView.Public,
		name, anInt, aLong, aDate
	);
	
	static {
		
		EntityContext.registerSearchablePropertySet(TestTwo.class, NodeIndex.fulltext.name(), AbstractNode.name, anInt, aLong, aDate);
		EntityContext.registerSearchablePropertySet(TestTwo.class, NodeIndex.keyword.name(), AbstractNode.name, anInt, aLong, aDate);
	}
		
}

