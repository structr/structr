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
package org.structr.rest.entity;

import java.util.List;
import org.structr.common.PropertyView;
import org.structr.core.EntityContext;
import static org.structr.core.GraphObject.uuid;
import org.structr.core.entity.AbstractNode;
import org.structr.core.notion.PropertyNotion;
import org.structr.core.property.EndNode;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;

/**
 *
 * @author Axel Morgner
 */
public class TestFive extends AbstractNode {
	
	public static final Property<List<TestOne>> manyToManyTestOnes = new EndNodes<>("manyToManyTestOnes", FiveOneManyToMany.class, new PropertyNotion(uuid));
	public static final Property<List<TestOne>> oneToManyTestOnes  = new EndNodes<>("oneToManyTestOnes",  FiveOneOneToMany.class, new PropertyNotion(uuid));
	
	public static final Property<TestThree>     oneToOneTestThree  = new EndNode<>("oneToOneTestThree",  FiveThreeOneToOne.class);
	public static final Property<TestThree>     manyToOneTestThree = new EndNode<>("manyToOneTestThree", FiveThreeManyToOne.class);
	
	static {
		
		EntityContext.registerPropertySet(TestFive.class, PropertyView.Public, AbstractNode.name, manyToManyTestOnes, oneToManyTestOnes, oneToOneTestThree, manyToOneTestThree);
	}
}
