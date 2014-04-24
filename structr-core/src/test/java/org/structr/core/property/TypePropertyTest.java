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
 * @author Christian Morgner
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
			assertTrue(labelsBefore.containsAll(Iterables.toSet(testEntity.getNode().getLabels())));

			// save ID for later use
			id = testEntity.getUuid();

			// change type to TestFive
			testEntity.setProperty(GraphObject.type, TestFive.class.getSimpleName());

			// commit transaction
			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
		}


		try (final Tx tx = app.tx()) {

			final TestFive testEntity = app.get(TestFive.class, id);

			assertNotNull(testEntity);

			// check labels after type change
			assertTrue(labelsAfter.containsAll(Iterables.toSet(testEntity.getNode().getLabels())));

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
		}
	}

}
