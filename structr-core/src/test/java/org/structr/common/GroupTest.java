package org.structr.common;

import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Group;
import org.structr.core.entity.TestOne;
import org.structr.core.entity.TestUser;
import org.structr.core.graph.Tx;

/**
 *
 * @author Christian Morgner
 */
public class GroupTest extends StructrTest {

	public void testGroupMembershipVisibility() {

		TestUser user1 = null;
		TestUser user2 = null;
		Group group    = null;

		// ################################################################################################################
		// create two users

		try (final Tx tx = app.tx()) {

			user1 = createTestNode(TestUser.class, "user1");
			user2 = createTestNode(TestUser.class, "user2");

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		final SecurityContext user1Context = SecurityContext.getInstance(user1, AccessMode.Backend);
		final App user1App                 = StructrApp.getInstance(user1Context);

		// ################################################################################################################
		// create a group and a test object that becomes accessible for the second user by group membership

		try (final Tx tx = user1App.tx()) {

			group = user1App.create(Group.class, "group");
			user1App.create(TestOne.class, "testone");

			assertEquals("Invalid group owner", user1, group.getOwnerNode());

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		// ################################################################################################################
		// user1 is owner of the test object
		// we now grant the group read access to the test object

		try (final Tx tx = user1App.tx()) {

			final TestOne test = user1App.nodeQuery(TestOne.class).getFirst();

			assertNotNull(test);

			test.grant(Permission.read, group);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		// ################################################################################################################
		// user2 is not yet member of the group, so
		// it should not be possible to access the object

		final SecurityContext user2Context = SecurityContext.getInstance(user2, AccessMode.Backend);
		final App user2App                 = StructrApp.getInstance(user2Context);

		try (final Tx tx = user2App.tx()) {

			final TestOne test = user2App.nodeQuery(TestOne.class).getFirst();
			assertNull(test);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// ################################################################################################################
		// now we add user2 to the group

		try (final Tx tx = user1App.tx()) {

			group.addMember(user2);
			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		// ################################################################################################################
		// user2 is now member of the group, so
		// it should be possible to access the object

		try (final Tx tx = user2App.tx()) {

			final TestOne test = user2App.nodeQuery(TestOne.class).getFirst();
			assertNotNull(test);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// ################################################################################################################
		// user2 should NOT be able to write the object

		try (final Tx tx = user2App.tx()) {

			final TestOne test = user2App.nodeQuery(TestOne.class).getFirst();
			assertNotNull(test);

			test.setProperty(TestOne.name, "newname");

			tx.success();

			fail("User should not be able to write an object that it doesn't own.");

		} catch (FrameworkException fex) {

			assertEquals("Invalid group permissions result", 403, fex.getStatus());
			assertEquals("Invalid group permissions result", "Modification not permitted.", fex.getMessage());
		}

		// ################################################################################################################
		// now we grant write access to the group

		try (final Tx tx = user1App.tx()) {

			final TestOne test = app.nodeQuery(TestOne.class).getFirst();
			assertNotNull(test);

			test.grant(Permission.write, group);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		// ################################################################################################################
		// user2 should now be able to write the object

		try (final Tx tx = user2App.tx()) {

			final TestOne test = user2App.nodeQuery(TestOne.class).getFirst();
			assertNotNull(test);

			test.setProperty(TestOne.name, "newname");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

	}

}
