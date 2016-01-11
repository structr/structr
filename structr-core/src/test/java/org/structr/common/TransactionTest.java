package org.structr.common;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.TestOne;
import org.structr.core.graph.Tx;
import org.structr.core.script.Scripting;
import org.structr.schema.action.ActionContext;

/**
 *
 * @author Christian Morgner
 */
public class TransactionTest extends StructrTest {

	public void testRollbackOnError () {

		final ActionContext ctx = new ActionContext(securityContext, null);

		/**
		 * first the old scripting style
		 */
		TestOne testNode = null;

		try (final Tx tx = app.tx()) {

			testNode = createTestNode(TestOne.class);
			testNode.setProperty(TestOne.aString, "InitialString");
			testNode.setProperty(TestOne.anInt, 42);

			tx.success();

		} catch (FrameworkException ex) {

			ex.printStackTrace();
			fail("Unexpected exception");

		}

		try (final Tx tx = app.tx()) {

			Scripting.replaceVariables(ctx, testNode, "${ ( set(this, 'aString', 'NewString'), set(this, 'anInt', 'NOT_AN_INTEGER') ) }");
			fail("StructrScript: setting anInt to 'NOT_AN_INTEGER' should cause an Exception");

			tx.success();

		} catch (FrameworkException expected) { }


		try {

			try (final Tx tx = app.tx()) {

				assertEquals("Property value should still have initial value!", "InitialString", testNode.getProperty(TestOne.aString));

				tx.success();
			}

		} catch (FrameworkException ex) {

			ex.printStackTrace();
			fail("Unexpected exception");

		}
	}

}
