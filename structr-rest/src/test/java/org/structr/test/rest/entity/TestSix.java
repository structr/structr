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

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.entity.AbstractNode;
import org.structr.core.notion.PropertyNotion;
import org.structr.core.property.*;

/**
 *
 *
 */
public class TestSix extends AbstractNode {

	public static final Property<TestSeven>       testSeven        = new StartNode<>("testSeven", SevenSixOneToMany.class);
	public static final Property<String>          testSevenName    = new EntityNotionProperty("testSevenName", testSeven, new PropertyNotion(TestSeven.name));

	public static final Property<Iterable<TestEight>> testEights       = new EndNodes<>("testEights", SixEightManyToMany.class);
	public static final Property<Iterable<Integer>>   testEightInts    = new CollectionNotionProperty("testEightInts", testEights, new PropertyNotion(TestEight.anInt));
	public static final Property<Iterable<String>>    testEightStrings = new CollectionNotionProperty("testEightStrings", testEights, new PropertyNotion(TestEight.aString));

	public static final Property<String>          aString          = new StringProperty("aString").indexed();
	public static final Property<Integer>         anInt            = new IntProperty("anInt").indexed();

	public static final View defaultView = new View(TestSix.class, PropertyView.Public,
		name, testSevenName, testEightInts, testEightStrings, aString, anInt
	);

}
