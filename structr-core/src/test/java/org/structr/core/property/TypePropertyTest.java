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

import java.util.LinkedHashSet;
import java.util.Set;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.fail;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Label;
import org.neo4j.helpers.collection.Iterables;
import org.structr.common.AccessControllable;
import org.structr.common.StructrTest;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.TestFive;
import org.structr.core.entity.TestFour;
import org.structr.core.graph.Tx;

/**
 *
 *
 */
public class TypePropertyTest extends StructrTest {

	public void testModifyType() {

		final Set<Label> labelsBefore = new LinkedHashSet<>();
		final Set<Label> labelsAfter  = new LinkedHashSet<>();
		String id                     = null;

		labelsBefore.add(DynamicLabel.label(AccessControllable.class.getSimpleName()));
		labelsBefore.add(DynamicLabel.label(TestFour.class.getSimpleName()));

		labelsAfter.add(DynamicLabel.label(AccessControllable.class.getSimpleName()));
		labelsAfter.add(DynamicLabel.label(TestFive.class.getSimpleName()));

		// create a new node, check labels, modify type, check labels again

		try (final Tx tx = app.tx()) {

			// create entity of type TestFour
			final TestFour testEntity = createTestNode(TestFour.class);

			// check if node exists
			assertNotNull(testEntity);

			// check labels before type change
			assertTrue(Iterables.toSet(testEntity.getNode().getLabels()).containsAll(labelsBefore));

			// save ID for later use
			id = testEntity.getUuid();

			// change type to TestFive
			testEntity.setProperty(GraphObject.type, TestFive.class.getSimpleName());

			// commit transaction
			tx.success();

		} catch (FrameworkException fex) {
			
			fex.printStackTrace();
			fail("Unexpected exception");
		}


		try (final Tx tx = app.tx()) {

			final TestFive testEntity = app.get(TestFive.class, id);

			assertNotNull(testEntity);

			// check labels after type change
			assertTrue(Iterables.toSet(testEntity.getNode().getLabels()).containsAll(labelsAfter));

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

}
