/*
 * Copyright (C) 2010-2021 Structr GmbH
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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.test.web.entity;

import java.util.Date;
import org.structr.common.PropertyView;
import org.structr.common.View;
import static org.structr.core.GraphObject.createdDate;
import org.structr.core.entity.AbstractNode;
import static org.structr.core.graph.NodeInterface.name;
import org.structr.core.property.DoubleProperty;
import org.structr.core.property.ISO8601DateProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.LongProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;

/**
 * A simple entity for the most basic tests.
 *
 *
 *
 */
public class TestOne extends AbstractNode {

	public static final Property<Integer>       anInt              = new IntProperty("anInt").indexed().indexedWhenEmpty();
	public static final Property<Long>          aLong              = new LongProperty("aLong").indexed().indexedWhenEmpty();
	public static final Property<Double>        aDouble            = new DoubleProperty("aDouble").indexed().indexedWhenEmpty();
	public static final Property<Date>          aDate              = new ISO8601DateProperty("aDate").indexed().indexedWhenEmpty();
	public static final Property<String>        aString            = new StringProperty("aString").indexed().indexedWhenEmpty();
	public static final Property<String>        htmlString         = new StringProperty("htmlString");

	public static final View publicView = new View(TestOne.class, PropertyView.Public,
		name, anInt, aDouble, aLong, aDate, createdDate, aString, htmlString
	);

	public static final View uiView = new View(TestOne.class, PropertyView.Ui,
		name, anInt, aDouble, aLong, aDate, createdDate, aString, htmlString
	);
}
