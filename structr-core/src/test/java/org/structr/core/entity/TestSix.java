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
package org.structr.core.entity;

import java.util.List;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.EndNode;

/**
 *
 * @author Christian Morgner
 */
public class TestSix extends AbstractNode {
	
	public static final Property<List<TestOne>> manyToManyTestOnes  = new EndNodes<>("manyToManyTestOnes", SixOneManyToMany.class);
	public static final Property<List<TestOne>> oneToManyTestOnes   = new EndNodes<>("oneToManyTestOnes", SixOneOneToMany.class);
	
	public static final Property<TestThree>     oneToOneTestThree   = new EndNode<>("oneToOneTestThree",  SixThree.class);
	public static final Property<TestThree>     oneToManyTestThrees = new EndNode<>("oneToManyTestThree", ThreeThree.class);
}
