/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.core.entity;

import java.util.List;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.EndNode;
import org.structr.core.property.IntProperty;

/**
 *
 *
 */
public class TestSix extends AbstractNode {

	public static final Property<List<TestOne>>   manyToManyTestOnes                   = new EndNodes<>("manyToManyTestOnes", SixOneManyToMany.class);
	public static final Property<List<TestOne>>   oneToManyTestOnes                    = new EndNodes<>("oneToManyTestOnes",  SixOneOneToMany.class);

	public static final Property<TestThree>       oneToOneTestThree                    = new EndNode<>("oneToOneTestThree",    SixThreeOneToOne.class);
	public static final Property<List<TestThree>> oneToManyTestThrees                  = new EndNodes<>("oneToManyTestThrees", SixThreeOneToMany.class);

	public static final Property<List<TestThree>> oneToManyTestThreesCascadeOut        = new EndNodes<>("oneToManyTestThreesCascadeOut",       SixThreeOneToManyCascadeOutgoing.class);
	public static final Property<List<TestThree>> oneToManyTestThreesCascadeIn         = new EndNodes<>("oneToManyTestThreesCascadeIn",        SixThreeOneToManyCascadeIncoming.class);
	public static final Property<List<TestThree>> oneToManyTestThreesCascadeBoth       = new EndNodes<>("oneToManyTestThreesCascadeBoth",      SixThreeOneToManyCascadeBoth.class);

	public static final Property<List<TestNine>>  oneToManyTestNinesCascadeConstraint  = new EndNodes<>("oneToManyTestNinesCascadeConstraint", SixNineOneToManyCascadeConstraint.class);

	public static final Property<Integer>         index                                = new IntProperty("index");
}
