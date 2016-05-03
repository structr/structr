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
package org.structr.rest.test;

import org.structr.core.notion.TypeAndPropertySetDeserializationStrategy;
import org.structr.rest.common.StructrRestTest;

/**
 *
 *
 */
public class AmbiguityTest extends StructrRestTest {

	/**
	 * Tests {@link TypeAndPropertySetDeserializationStrategy} for ambiguity avoidance.
	 * 
	 * Before fixing a bug in {@link TypeAndPropertySetDeserializationStrategy}, the creation
	 * of test03 was not possible because the a search was conducted internally with the values
	 * given in the 'testSeven' object, and the result was ambiguous.
	 */
	public void testAmbiguity() {

		// Create a TestTen with a TestSeven on the fly with {'aString':'test'}
		String test01 = createEntity("/test_tens", "{ 'name': 'test01', 'testSeven':{ 'aString' : 'test' } }");
		
		// Create another TestSeven with {'aString':'test'}
		String test02 = createEntity("/test_sevens", "{ 'aString': 'test' }");

		// Create another TestTen with another TestSeven on the fly
		String test03 = createEntity("/test_tens", "{ 'name': 'test02', 'testSeven':{ 'aString' : 'test' } }");
		
		

	}
}
