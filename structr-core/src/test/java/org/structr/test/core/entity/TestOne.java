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
package org.structr.test.core.entity;

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.*;

import java.util.Date;

/**
 * A simple entity for the most basic tests.
 *
 *
 *
 */
public class TestOne extends AbstractNode {

	public enum Status {
		One, Two, Three, Four
	}

	public static final Property<Integer>           anInt              = new IntProperty("anInt").indexed().indexedWhenEmpty();
	public static final Property<Long>              aLong              = new LongProperty("aLong").indexed().indexedWhenEmpty();
	public static final Property<Double>            aDouble            = new DoubleProperty("aDouble").indexed().indexedWhenEmpty();
	public static final Property<Date>              aDate              = new ISO8601DateProperty("aDate").indexed().indexedWhenEmpty();
	public static final Property<Status>            anEnum             = new EnumProperty<>("anEnum", Status.class).indexed();
	public static final Property<String>            aString            = new StringProperty("aString").indexed().indexedWhenEmpty();
	public static final Property<Boolean>           aBoolean           = new BooleanProperty("aBoolean").indexed();
	public static final Property<String>            testString         = new StringProperty("testString");
	public static final Property<String>            anotherString      = new StringProperty("anotherString");
	public static final Property<String>            replaceString      = new StringProperty("replaceString");
	public static final Property<String>            cleanTestString    = new StringProperty("cleanTestString");
	public static final Property<String>            stringWithQuotes   = new StringProperty("stringWithQuotes");
	public static final Property<Integer>           setTestInteger1    = new IntProperty("setTestInteger1");
	public static final Property<Integer>           setTestInteger2    = new IntProperty("setTestInteger2");
	public static final Property<Integer>           setTestInteger3    = new IntProperty("setTestInteger3");
	public static final Property<String>            alwaysNull         = new StringProperty("alwaysNull");
	public static final Property<String>            doResult           = new StringProperty("doResult");
	public static final Property<String>            stringWithDefault  = new StringProperty("stringWithDefault").defaultValue("default value").indexedWhenEmpty();

	public static final Property<TestTwo>           testTwo            = new EndNode<>("testTwo",   OneTwoOneToOne.class);
	public static final Property<TestThree>         testThree          = new EndNode<>("testThree", OneThreeOneToOne.class);
	public static final Property<TestFour>          testFour           = new EndNode<>("testFour",  OneFourOneToOne.class);
	public static final Property<Iterable<TestSix>> manyToManyTestSixs = new StartNodes<>("manyToManyTestSixs", SixOneManyToMany.class);

	public static final Property<String>            aCreateString      = new StringProperty("aCreateString").indexed();
	public static final Property<Integer>           aCreateInt         = new IntProperty("aCreateInt").indexed();

	public static final Property<String[]>          aStringArray       = new ArrayProperty("aStringArray", String.class).indexed();

	public static final Property<Boolean>           isValid            = new BooleanProperty("isValid");

	public static final View publicView = new View(TestOne.class, PropertyView.Public,
		name, anInt, aDouble, aLong, aDate, createdDate, aString, anotherString, aBoolean, anEnum, stringWithDefault, aStringArray
	);

	public static final View protectedView = new View(TestOne.class, PropertyView.Protected,
		name, anInt, aString
	);
}
