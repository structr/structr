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

import org.structr.core.entity.OneToOne;
import org.structr.core.property.*;

/**
 *
 *
 */
public class OneFourOneToOne extends OneToOne<TestOne, TestFour> {

	public static final Property<String>   startNodeId         = new StringProperty("startNodeId");
	public static final Property<String>   endNodeId           = new StringProperty("endNodeId");
	public static final Property<String[]> stringArrayProperty = new ArrayProperty<>("stringArrayProperty", String.class);
	public static final Property<Boolean>  booleanProperty     = new BooleanProperty("booleanProperty").indexed();
	public static final Property<Double>   doubleProperty      = new DoubleProperty("doubleProperty").indexed();
	public static final Property<Integer>  integerProperty     = new IntProperty("integerProperty").indexed();
	public static final Property<Long>     longProperty        = new LongProperty("longProperty").indexed();
	public static final Property<String>   stringProperty      = new StringProperty("stringProperty").indexed();
	public static final Property<TestEnum> enumProperty        = new EnumProperty("enumProperty", TestEnum.class).indexed();

	@Override
	public Class<TestOne> getSourceType() {
		return TestOne.class;
	}

	@Override
	public String name() {
		return "IS_AT";
	}

	@Override
	public Class<TestFour> getTargetType() {
		return TestFour.class;
	}

	@Override
	public Property<String> getSourceIdProperty() {
		return startNodeId;
	}

	@Override
	public Property<String> getTargetIdProperty() {
		return endNodeId;
	}
}
