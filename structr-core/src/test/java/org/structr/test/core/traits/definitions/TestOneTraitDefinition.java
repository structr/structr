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
package org.structr.test.core.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;

import java.util.Date;
import java.util.Map;
import java.util.Set;

public class TestOneTraitDefinition extends AbstractNodeTraitDefinition {

	public TestOneTraitDefinition() {
		super("TestOne");
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final PropertyKey<Integer>                 anInt              = new IntProperty("anInt").indexed().indexedWhenEmpty();
		final PropertyKey<Long>                    aLong              = new LongProperty("aLong").indexed().indexedWhenEmpty();
		final PropertyKey<Double>                  aDouble            = new DoubleProperty("aDouble").indexed().indexedWhenEmpty();
		final PropertyKey<Date>                    aDate              = new ISO8601DateProperty("aDate").indexed().indexedWhenEmpty();
		final PropertyKey<String>                  anEnum             = new EnumProperty("anEnum", Set.of("One", "Two", "Three", "Four")).indexed();
		final PropertyKey<String>                  aString            = new StringProperty("aString").indexed().indexedWhenEmpty();
		final PropertyKey<Boolean>                 aBoolean           = new BooleanProperty("aBoolean").indexed();
		final PropertyKey<String>                  testString         = new StringProperty("testString");
		final PropertyKey<String>                  anotherString      = new StringProperty("anotherString");
		final PropertyKey<String>                  replaceString      = new StringProperty("replaceString");
		final PropertyKey<String>                  cleanTestString    = new StringProperty("cleanTestString");
		final PropertyKey<String>                  stringWithQuotes   = new StringProperty("stringWithQuotes");
		final PropertyKey<Integer>                 setTestInteger1    = new IntProperty("setTestInteger1");
		final PropertyKey<Integer>                 setTestInteger2    = new IntProperty("setTestInteger2");
		final PropertyKey<Integer>                 setTestInteger3    = new IntProperty("setTestInteger3");
		final PropertyKey<String>                  alwaysNull         = new StringProperty("alwaysNull");
		final PropertyKey<String>                  doResult           = new StringProperty("doResult");
		final PropertyKey<String>                  stringWithDefault  = new StringProperty("stringWithDefault").defaultValue("default value").indexedWhenEmpty();
		final PropertyKey<NodeInterface>           testTwo            = new EndNode("testTwo",  "OneTwoOneToOne");
		final PropertyKey<NodeInterface>           testThree          = new EndNode("testThree", "OneThreeOneToOne");
		final PropertyKey<NodeInterface>           testFour           = new EndNode("testFour",  "OneFourOneToOne");
		final PropertyKey<Iterable<NodeInterface>> manyToManyTestSixs = new StartNodes("manyToManyTestSixs", "SixOneManyToMany");
		final PropertyKey<String>                  aCreateString      = new StringProperty("aCreateString").indexed();
		final PropertyKey<Integer>                 aCreateInt         = new IntProperty("aCreateInt").indexed();
		final PropertyKey<String[]>                aStringArray       = new ArrayProperty("aStringArray", String.class).indexed();
		final PropertyKey<Boolean>                 isValid            = new BooleanProperty("isValid");

		return Set.of(
			anInt,
			aLong,
			aDouble,
			aDate,
			anEnum,
			aString,
			aBoolean,
			testString,
			anotherString,
			replaceString,
			cleanTestString,
			stringWithQuotes,
			setTestInteger1,
			setTestInteger2,
			setTestInteger3,
			alwaysNull,
			doResult,
			stringWithDefault,
			testTwo,
			testThree,
			testFour,
			manyToManyTestSixs,
			aCreateString,
			aCreateInt,
			aStringArray,
			isValid
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(

			PropertyView.Public,
			Set.of(
				"name", "anInt", "aDouble", "aLong", "aDate", "createdDate", "aString", "anotherString", "aBoolean", "anEnum", "stringWithDefault", "aStringArray"
			),

			PropertyView.Protected,
			Set.of(
				"name", "anInt", "aString"
			)
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {
		return Map.of();
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
