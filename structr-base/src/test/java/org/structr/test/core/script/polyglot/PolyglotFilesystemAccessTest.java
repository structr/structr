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
package org.structr.test.core.script.polyglot;

import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.script.polyglot.config.ScriptConfig;
import org.structr.core.traits.StructrTraits;
import org.structr.schema.action.Actions;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.FileHelper;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

/**
 * Ticket 1592: the scripting engine's file system (PolyglotFilesystem) resolved every path through
 * StructrApp.getInstance() without a security context, which is the superuser instance, so a dynamic
 * import() read any File node in the instance no matter who ran the script - and every script is a
 * module, so import() is always available. checkAccess() made it worse by catching FrameworkException
 * and returning normally, and a checkAccess that returns is a granted access.
 */
public class PolyglotFilesystemAccessTest extends StructrUiTest {

	private static final String SECRET = "top-secret-value";

	@Test
	public void testImportCannotReadAFileTheCallerMayNotRead() {

		createFile("unreadable.js", false);

		assertEquals("import() handed a file the caller has no read permission on to the script",
			"denied", importSecretAsTester("/unreadable.js"));
	}

	/**
	 * The counterpart: a file the caller may read still imports, so this is a permission check and not
	 * a blanket ban on importing from the file area.
	 */
	@Test
	public void testImportStillReadsAFileTheCallerMayRead() {

		createFile("readable.js", true);

		assertEquals(SECRET, importSecretAsTester("/readable.js"));
	}

	/**
	 * A path with no file behind it must fail the same way for everyone. It used to be the one case
	 * checkAccess() reported correctly, and the fix must not turn it into a way of telling "no such
	 * file" and "not yours" apart.
	 */
	@Test
	public void testImportOfAMissingFileFails() {

		assertEquals("denied", importSecretAsTester("/does-not-exist.js"));
	}

	// ----- private methods -----
	private void createFile(final String name, final boolean readableByAuthenticatedUsers) {

		try (final Tx tx = app.tx()) {

			final NodeInterface file = FileHelper.createFile(securityContext, ("export const secret = '" + SECRET + "';").getBytes(StandardCharsets.UTF_8),
				"application/javascript", StructrTraits.FILE, name, true);

			file.setVisibleToAuthenticatedUsers(readableByAuthenticatedUsers);

			tx.success();

		} catch (Throwable t) {

			fail("Unexpected exception: " + t.getMessage());
		}
	}

	/**
	 * Imports the given path in the security context of a non-admin user and answers the secret the
	 * module exports, or "denied" if the import did not go through.
	 */
	private String importSecretAsTester(final String path) {

		try (final Tx tx = app.tx()) {

			final Principal tester        = app.create(StructrTraits.USER, "tester").as(Principal.class);
			final SecurityContext context = SecurityContext.getInstance(tester, AccessMode.Frontend);

			final Object result = Actions.execute(context, null, "${{ return (await import('" + path + "')).secret; }}",
				Collections.EMPTY_MAP, "test", null, ScriptConfig.builder().wrapJsInMain(true).build());

			tx.success();

			return result != null ? result.toString() : "denied";

		} catch (FrameworkException fex) {

			return "denied";
		}
	}
}
