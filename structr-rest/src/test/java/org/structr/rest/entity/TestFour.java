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

import org.structr.core.entity.AbstractNode;
import org.structr.core.property.EndNodes;
import org.structr.core.property.End;
import org.structr.rest.common.TestRestRelType;

/**
 *
 * @author Axel Morgner
 */
public class TestFour extends AbstractNode {
	
	public static final EndNodes<TestOne> manyToManyTestOnes = new EndNodes<TestOne>("manyToManyTestOnes", TestOne.class, TestRestRelType.MANY_TO_MANY, false);
	public static final EndNodes<TestOne> oneToManyTestOnes  = new EndNodes<TestOne>("oneToManyTestOnes",  TestOne.class, TestRestRelType.ONE_TO_MANY,  true);
	
	public static final End<TestThree>   oneToOneTestThree  = new End<TestThree>("oneToOneTestThree",  TestThree.class, TestRestRelType.ONE_TO_ONE,  false);
	public static final End<TestThree>   manyToOneTestThree = new End<TestThree>("manyToOneTestThree", TestThree.class, TestRestRelType.MANY_TO_ONE, true);
}
