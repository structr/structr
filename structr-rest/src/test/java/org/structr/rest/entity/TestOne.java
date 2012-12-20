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
import org.structr.core.property.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.core.property.ISO8601DateProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.LongProperty;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeService.NodeIndex;

/**
 * A simple entity for the most basic tests.
 * 
 * @author Axel Morgner
 */
public class TestOne extends AbstractNode {
	
	public static final PropertyKey<Integer> anInt = new IntProperty("anInt");
	public static final PropertyKey<Long> aLong    = new LongProperty("aLong");
	public static final PropertyKey<Date> aDate    = new ISO8601DateProperty("aDate");
	
	static {
		
		EntityContext.registerPropertySet(TestOne.class, PropertyView.Public, AbstractNode.name, anInt, aLong, aDate);
		
//		EntityContext.registerPropertyConverter(TestOne.class, anInt, IntConverter.class);
//		EntityContext.registerPropertyConverter(TestOne.class, aDate, DateConverter.class);
		
		EntityContext.registerSearchablePropertySet(TestOne.class, NodeIndex.fulltext.name(), AbstractNode.name, anInt, aLong, aDate);
		EntityContext.registerSearchablePropertySet(TestOne.class, NodeIndex.keyword.name(), AbstractNode.name, anInt, aLong, aDate);
	}
		
}
