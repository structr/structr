/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.test.rest.entity;

import org.structr.core.entity.AbstractNode;
import org.structr.core.property.EndNode;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;

/**
 *
 *
 */
public class TestFour extends AbstractNode {

	public static final Property<Iterable<TestOne>> manyToManyTestOnes = new EndNodes<>("manyToManyTestOnes", FourOneManyToMany.class);
	public static final Property<Iterable<TestOne>> oneToManyTestOnes  = new EndNodes<>("oneToManyTestOnes",  FourOneOneToMany.class);

	public static final Property<TestThree>     oneToOneTestThree  = new EndNode<>("oneToOneTestThree",  FourThreeOneToOne.class);
	public static final Property<TestThree>     manyToOneTestThree = new StartNode<>("manyToOneTestThree", ThreeFourOneToMany.class);
}
