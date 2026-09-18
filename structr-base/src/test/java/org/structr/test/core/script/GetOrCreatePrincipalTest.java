/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.test.core.script;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.Tx;
import org.structr.core.script.Scripting;
import org.structr.core.traits.StructrTraits;
import org.structr.schema.action.ActionContext;
import org.structr.test.common.StructrTest;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * getOrCreate() and the second principal of the same name.
 *
 * <p>The function queries the type it was given, so asking for a Group called "team" on an instance that
 * has a USER called "team" finds nothing and creates the group beside the user. Nothing fails at that
 * moment. What fails is a deployment, later: it resolves owners and permissions by name over Principal,
 * finds two, and drops each one as ambiguous, which from inside the application is indistinguishable from
 * permissions that never arrived.</p>
 *
 * <p>This matters because the documented remedy for missing principals is a pre-deploy.conf built on this
 * very function, so the fix for one deployment was the way to break the next.</p>
 */
public class GetOrCreatePrincipalTest extends StructrTest {

	@Test
	public void testASecondPrincipalOfTheSameNameIsRefused() {

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.USER, "team");

			tx.success();

		} catch (final FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			Scripting.evaluate(new ActionContext(securityContext), null, "${{ $.getOrCreate('Group', { name: 'team' }); }}", "test");

			fail("Creating a Group beside a User of the same name should be refused: both answer to nodeQuery(Principal).name(), "
				+ "so every ownership and permission naming it would be dropped as ambiguous.");

			tx.success();

		} catch (final FrameworkException expected) {

			assertEquals("the refusal should be a 422", 422, expected.getStatus());

			assertTrue("the message should name what already exists: " + expected.getMessage(),
				expected.getMessage().contains("team"));
		}
	}

	@Test
	public void testAnExistingPrincipalOfTheSameTypeIsReturnedAsBefore() {

		final String existingId;

		try (final Tx tx = app.tx()) {

			existingId = app.create(StructrTraits.GROUP, "editors").getUuid();

			tx.success();

		} catch (final FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");

			return;
		}

		try (final Tx tx = app.tx()) {

			// the ordinary case the function exists for: the query finds it, so the guard never runs
			final Object result = Scripting.evaluate(new ActionContext(securityContext), null,
				"${{ $.getOrCreate('Group', { name: 'editors' }).id; }}", "test");

			assertEquals("an existing principal of the same type must still be returned", existingId, result);

			tx.success();

		} catch (final FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testANewPrincipalNobodyElseIsNamedAfterIsCreated() {

		try (final Tx tx = app.tx()) {

			final Object result = Scripting.evaluate(new ActionContext(securityContext), null,
				"${{ $.getOrCreate('Group', { name: 'nobody-else-has-this-name' }).id; }}", "test");

			assertNotNull("a principal whose name is free must still be created", result);

			tx.success();

		} catch (final FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testANonPrincipalTypeIsNotAffected() {

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.GROUP, "shared-name");

			tx.success();

		} catch (final FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			// the ambiguity only matters between principals, so nothing else is second-guessed
			final Object result = Scripting.evaluate(new ActionContext(securityContext), null,
				"${{ $.getOrCreate('MailTemplate', { name: 'shared-name' }).id; }}", "test");

			assertNotNull("a non-principal type sharing a name with a group must still be created", result);

			tx.success();

		} catch (final FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}
}
