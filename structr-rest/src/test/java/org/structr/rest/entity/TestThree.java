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
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.*;
import org.structr.rest.common.TestEnum;

/**
 *
 *
 */
public class TestThree extends AbstractNode {

	public static final Property<String[]>      stringArrayProperty         = new ArrayProperty<>("stringArrayProperty", String.class).indexedWhenEmpty();
	public static final Property<Boolean>       booleanProperty             = new BooleanProperty("booleanProperty").indexed();
	public static final Property<Double>        doubleProperty              = new DoubleProperty("doubleProperty").indexed().indexedWhenEmpty();
	public static final Property<Integer>       integerProperty             = new IntProperty("integerProperty").indexed().indexedWhenEmpty();
	public static final Property<Long>          longProperty                = new LongProperty("longProperty").indexed().indexedWhenEmpty();
	public static final Property<String>        stringProperty              = new StringProperty("stringProperty").indexed().indexedWhenEmpty();
	public static final Property<Date>          dateProperty                = new ISO8601DateProperty("dateProperty").indexed().indexedWhenEmpty();
	public static final Property<TestEnum>      enumProperty                = new EnumProperty("enumProperty", TestEnum.class).indexed();
	public static final Property<Boolean>       constantBooleanProperty     = new ConstantBooleanProperty("constantBooleanProperty", true);

	public static final View publicView = new View(TestThree.class, PropertyView.Public,
		name, stringArrayProperty, booleanProperty, doubleProperty, integerProperty, longProperty, stringProperty, dateProperty, enumProperty, constantBooleanProperty
	);
}
