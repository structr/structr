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
package org.structr.core.property;

import java.util.Date;
import org.structr.common.StructrTest;
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.entity.TestFour;
import org.structr.core.graph.Tx;

/**
 *
 *
 */
public class DatePropertyTest extends StructrTest {

	public void test() {

		try {

			final Property<Date> instance = TestFour.dateProperty;
			final TestFour testEntity     = createTestNode(TestFour.class);

			assertNotNull(testEntity);

			// store Date in the test entitiy
			final Date value = new Date(123456789L);

			try (final Tx tx = app.tx()) {

				instance.setProperty(securityContext, testEntity, value);
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				// check value from database
				assertEquals(value, instance.getProperty(securityContext, testEntity, true));
			}

		} catch (FrameworkException fex) {

			fail("Unable to store array");
		}
	}

	public void testSimpleSearchOnNode() {

		try {

			final PropertyMap properties  = new PropertyMap();
			final PropertyKey<Date> key   = TestFour.dateProperty;
			final Date value              = new Date(123456789L);

			properties.put(key,value);

			final TestFour testEntity     = createTestNode(TestFour.class, properties);

			assertNotNull(testEntity);

			try (final Tx tx = app.tx()) {

				// check value from database
				assertEquals(value, testEntity.getProperty(key));

				Result<TestFour> result = app.nodeQuery(TestFour.class).and(key, value).getResult();

				assertEquals(1, result.size());
				assertEquals(testEntity, result.get(0));
			}

		} catch (FrameworkException fex) {

			fail("Unable to store array");
		}

	}

	public void testRangeSearchOnNode() {

		try {

			final PropertyMap properties = new PropertyMap();
			final PropertyKey<Date> key  = TestFour.dateProperty;
			final Date minValue          = new Date(1234567880L);
			final Date value             = new Date(1234567890L);
			final Date maxValue          = new Date(1234567900L);
			final Date maxMaxValue       = new Date(1234567910L);

			properties.put(key, value);

			final TestFour testEntity = createTestNode(TestFour.class, properties);

			assertNotNull(testEntity);

			try (final Tx tx = app.tx()) {

				// check value from database
				assertEquals(value, testEntity.getProperty(key));

				Result<TestFour> result = app.nodeQuery(TestFour.class).andRange(key, minValue, maxValue).getResult();

				assertEquals(1, result.size());
				assertEquals(testEntity, result.get(0));

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				Result<TestFour> result = app.nodeQuery(TestFour.class).andRange(key, maxValue, maxMaxValue).getResult();

				assertEquals(0, result.size());

				tx.success();
			}

		} catch (FrameworkException fex) {

			fail("Unable to store array");
		}

	}
}
