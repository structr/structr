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
package org.structr.test.files;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.fail;

/**
 * Ticket 1587: "." and ".." are not names, they are the two path segments every filesystem reserves.
 * LocalFSHelper builds the path on disk by concatenating the mount target with the names along the
 * way, so a folder named ".." would walk out of the mount, and the next step creates the directory it
 * points at. These tests pin that such names are refused and that names which merely contain dots
 * stay valid.
 */
public class MountPathTraversalTest extends StructrUiTest {

	@Test
	public void testDotAndDotDotAreNotValidFileNames() {

		for (final String name : new String[] { ".", ".." }) {

			for (final String type : new String[] { StructrTraits.FOLDER, StructrTraits.FILE }) {

				try (final Tx tx = app.tx()) {

					app.create(type, new NodeAttribute<>(Traits.of(type).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name));

					tx.success();

					fail("A " + type + " named \"" + name + "\" must be refused, it is a path segment and not a name");

				} catch (FrameworkException expected) {

					// that is the point
				}
			}
		}
	}

	/**
	 * A name that merely looks suspicious is still a name: only "." and ".." are reserved, and refusing
	 * more than that would break files people legitimately have.
	 */
	@Test
	public void testNamesContainingDotsAreStillAllowed() {

		for (final String name : new String[] { "...", "..foo", "foo..", "a.b", ".hidden" }) {

			try (final Tx tx = app.tx()) {

				app.create(StructrTraits.FOLDER, new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name));

				tx.success();

			} catch (FrameworkException fex) {

				fail("A folder named \"" + name + "\" must remain valid: " + fex.getMessage());
			}
		}
	}
}
