package org.structr.core.property;

import org.structr.common.StructrTest;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.Tx;

/**
 *
 * @author Christian Morgner
 */
public class FunctionPropertyTest extends StructrTest {

	public void testEscaping() {

		// create test node with offending quote

		try (final Tx tx = app.tx()) {

			final PropertyMap properties = new PropertyMap();

			properties.put(AbstractNode.name, "Test");
			properties.put(new StringProperty("_functionTest"), "Function({ // \"})");

			app.create(SchemaNode.class, properties);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

}
