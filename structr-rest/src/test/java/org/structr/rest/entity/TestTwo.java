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
package org.structr.rest.entity;

import java.util.Date;
import java.util.List;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.property.ISO8601DateProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.LongProperty;
import org.structr.core.property.Property;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.EndNodes;

/**
 * Another simple entity for the most basic tests.
 *
 *
 */
public class TestTwo extends AbstractNode {

	public static final Property<Integer>       anInt    = new IntProperty("anInt").indexed();
	public static final Property<Long>          aLong    = new LongProperty("aLong").indexed();
	public static final Property<Date>          aDate    = new ISO8601DateProperty("aDate").indexed();

	public static final Property<List<TestOne>> testOnes = new EndNodes<>("test_ones", TwoOneOneToMany.class);
	public static final Property<List<TestOne>> testOnesAlt = new EndNodes<>("testOnes", TwoOneOneToMany.class);

	public static final View publicView = new View(TestTwo.class, PropertyView.Public,
		name, anInt, aLong, aDate, testOnes, testOnesAlt
	);
}

