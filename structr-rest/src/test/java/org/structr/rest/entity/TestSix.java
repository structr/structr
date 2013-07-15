/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.rest.entity;

import java.util.List;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.entity.AbstractNode;
import org.structr.core.notion.PropertyNotion;
import org.structr.core.property.CollectionNotionProperty;
import org.structr.core.property.CollectionProperty;
import org.structr.core.property.EntityNotionProperty;
import org.structr.core.property.EntityProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.rest.common.TestRestRelType;

/**
 *
 * @author Christian Morgner
 */
public class TestSix extends AbstractNode {

	public static final EntityProperty<TestSeven>     testSeven        = new EntityProperty<TestSeven>("testSeven", TestSeven.class, TestRestRelType.HAS, true);
	public static final Property<String>              testSevenName    = new EntityNotionProperty("testSevenName", testSeven, new PropertyNotion(TestSeven.name));
	
	public static final CollectionProperty<TestEight> testEights       = new CollectionProperty<TestEight>("testEights", TestEight.class, TestRestRelType.HAS, false);
	public static final Property<List<Integer>>       testEightInts    = new CollectionNotionProperty("testEightInts", testEights, new PropertyNotion(TestEight.anInt));
	public static final Property<List<String>>        testEightStrings = new CollectionNotionProperty("testEightStrings", testEights, new PropertyNotion(TestEight.aString));
	    
	public static final Property<String>              aString          = new StringProperty("aString").indexed();
	public static final Property<Integer>             anInt            = new IntProperty("anInt").indexed();
	
	public static final View defaultView = new View(TestSix.class, PropertyView.Public,
		name, testSevenName, testEightInts, testEightStrings, aString, anInt
	);
	
}
