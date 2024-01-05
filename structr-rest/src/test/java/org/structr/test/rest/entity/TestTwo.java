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
import org.structr.core.property.*;

import java.util.Date;

/**
 * Another simple entity for the most basic tests.
 *
 *
 */
public class TestTwo extends AbstractNode {

	public static final Property<Integer>       anInt    = new IntProperty("anInt").indexed().indexedWhenEmpty();
	public static final Property<Long>          aLong    = new LongProperty("aLong").indexed().indexedWhenEmpty();
	public static final Property<Date>          aDate    = new ISO8601DateProperty("aDate").indexed();

	public static final Property<Iterable<TestOne>> testOnes = new EndNodes<>("test_ones", TwoOneOneToMany.class);
	public static final Property<Iterable<TestOne>> testOnesAlt = new EndNodes<>("testOnes", TwoOneOneToMany.class);

	public static final View publicView = new View(TestTwo.class, PropertyView.Public,
		name, anInt, aLong, aDate, testOnes, testOnesAlt
	);
}

