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
package org.structr.core;

import org.structr.common.StructrTest;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.TestSix;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;

/**
 *
 */
public class TestSimpleOperations extends StructrTest {

	public void test00SimpleCreateOperation() {

		try (final Tx tx = app.tx()) {

			final PropertyMap properties = new PropertyMap();

			properties.put(TestSix.name, "name");

			// test null value for a 1:1 related property
			properties.put(TestSix.oneToOneTestThree, null);

			app.create(TestSix.class, properties);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			final TestSix test = app.nodeQuery(TestSix.class).getFirst();

			assertNotNull("Invalid simple object creation result", test);
			assertEquals("Invalid simple object creation result", "name", test.getProperty(AbstractNode.name));
			assertEquals("Invalid simple object creation result", null,   test.getProperty(TestSix.oneToOneTestThree));

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception");
		}
	}

}
