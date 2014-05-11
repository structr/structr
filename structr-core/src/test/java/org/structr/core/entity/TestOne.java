/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.entity;

import java.util.Date;
import java.util.List;
import org.structr.core.property.Property;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.DoubleProperty;
import org.structr.core.property.ISO8601DateProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.LongProperty;
import org.structr.core.property.EndNode;
import org.structr.core.property.StartNodes;
import org.structr.core.property.StringProperty;

/**
 * A simple entity for the most basic tests.
 *
 *
 * @author Axel Morgner
 */
public class TestOne extends AbstractNode {

	public static final Property<Integer>       anInt              = new IntProperty("anInt").indexed().indexedWhenEmpty();
	public static final Property<Long>          aLong              = new LongProperty("aLong").indexed().indexedWhenEmpty();
	public static final Property<Double>        aDouble            = new DoubleProperty("aDouble").indexed().indexedWhenEmpty();
	public static final Property<Date>          aDate              = new ISO8601DateProperty("aDate").indexed().indexedWhenEmpty();
	public static final Property<String>        aString            = new StringProperty("aString").indexed().indexedWhenEmpty();
	public static final Property<Boolean>       aBoolean           = new BooleanProperty("aBoolean");
	public static final Property<String>        anotherString      = new StringProperty("anotherString");
	public static final Property<String>        replaceString      = new StringProperty("replaceString");
	public static final Property<String>        cleanTestString    = new StringProperty("cleanTestString");
	public static final Property<Integer>       setTestInteger1    = new IntProperty("setTestInteger1");
	public static final Property<Integer>       setTestInteger2    = new IntProperty("setTestInteger2");
	public static final Property<Integer>       setTestInteger3    = new IntProperty("setTestInteger3");
	public static final Property<String>        alwaysNull         = new StringProperty("alwaysNull");
	public static final Property<String>        doResult           = new StringProperty("doResult");

	public static final Property<TestTwo>       testTwo            = new EndNode<>("testTwo",   OneTwoOneToOne.class);
	public static final Property<TestThree>     testThree          = new EndNode<>("testThree", OneThreeOneToOne.class);
	public static final Property<TestFour>      testFour           = new EndNode<>("testFour",  OneFourOneToOne.class);
	public static final Property<List<TestSix>> manyToManyTestSixs = new StartNodes<>("manyToManyTestSixs", SixOneManyToMany.class);

	public static final View publicView = new View(TestOne.class, PropertyView.Public,
		name, anInt, aDouble, aLong, aDate, createdDate, aString, anotherString, aBoolean
	);
}
