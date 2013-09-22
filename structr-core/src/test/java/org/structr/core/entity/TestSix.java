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
package org.structr.core.entity;

import org.structr.core.TestRelType;
import org.structr.core.property.CollectionProperty;
import org.structr.core.property.EntityProperty;

/**
 *
 * @author Christian Morgner
 */
public class TestSix extends AbstractNode {
	
	public static final CollectionProperty<TestOne> manyToManyTestOnes = new CollectionProperty<TestOne>("manyToManyTestOnes", TestOne.class, TestRelType.MANY_TO_MANY, false);
	public static final CollectionProperty<TestOne> manyToOneTestOnes  = new CollectionProperty<TestOne>("manyToOneTestOnes",  TestOne.class, TestRelType.MANY_TO_ONE,  true);
	
	public static final EntityProperty<TestThree>   oneToOneTestThree  = new EntityProperty<TestThree>("oneToOneTestThree",  TestThree.class, TestRelType.ONE_TO_ONE,  false);
	public static final EntityProperty<TestThree>   oneToManyTestThrees = new EntityProperty<TestThree>("oneToManyTestThree", TestThree.class, TestRelType.ONE_TO_MANY, true);
}
