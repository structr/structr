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

import org.structr.core.entity.AbstractNode;
import org.structr.core.property.ISO8601DateProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;

import java.util.Date;

/**
 * A simple entity for the most basic tests.
 *
 * This class doesn't have the not-null constraint on TestOne, so it should
 * not be deleted over relationships which are tagged with
 * DELETE_IF_CONSTRAINT_WOULD_BE_VIOLATED
 *
 *
 *
 */
public class TestThree extends AbstractNode {

	public static final String TEST_THREE_CUSTOM_DATE_FORMAT = "dd.MM.yyyy";

	public static final Property<TestOne> testOne          = new StartNode<>("testOne",         OneThreeOneToOne.class);
	public static final Property<TestSix> oneToOneTestSix  = new StartNode<>("oneToOneTestSix", SixThreeOneToOne.class);
	public static final Property<TestSix> oneToManyTestSix = new StartNode<>("oneToManyTestSix", SixThreeOneToMany.class);
	public static final Property<Date>    aDateWithFormat  = new ISO8601DateProperty("aDateWithFormat").format(TEST_THREE_CUSTOM_DATE_FORMAT).indexed().indexedWhenEmpty();

}
