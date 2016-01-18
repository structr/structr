package org.structr.web.test;

import java.util.List;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.web.common.StructrUiTest;
import org.structr.web.entity.User;

/**
 *
 * @author Christian Morgner
 */
public class UserTest extends StructrUiTest {

	public void test001EMailAddressConstraint() {

		try (final Tx tx = app.tx()) {

			app.create(User.class,
				new NodeAttribute(User.name, "TestUser1"),
				new NodeAttribute(User.eMail, "user@structr.test")
			);

			app.create(User.class,
				new NodeAttribute(User.name, "TestUser2"),
				new NodeAttribute(User.eMail, "user@structr.test")
			);

			tx.success();

			fail("Expected exception to be thrown.");

		} catch (FrameworkException fex) {
			assertEquals("Invalid error code", 422, fex.getStatus());
		}

		check();

		try (final Tx tx = app.tx()) {

			app.create(User.class,
				new NodeAttribute(User.name, "TestUser1"),
				new NodeAttribute(User.eMail, "user@structr.test")
			);

			app.create(User.class,
				new NodeAttribute(User.name, "TestUser2"),
				new NodeAttribute(User.eMail, "User@Structr.test")
			);

			tx.success();

			fail("Expected exception to be thrown.");

		} catch (FrameworkException fex) {
			assertEquals("Invalid error code", 422, fex.getStatus());
		}

		check();
	}

	private void check() {

		try (final Tx tx = app.tx()) {

			final List<User> users = app.nodeQuery(User.class).getAsList();

			assertEquals("Expected no users to be created because of constraints", 0, users.size());

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			fex.printStackTrace();
		}

	}
}
