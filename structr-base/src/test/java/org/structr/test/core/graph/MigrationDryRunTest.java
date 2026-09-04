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
package org.structr.test.core.graph;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.MigrationService;
import org.structr.test.common.StructrTest;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.fail;

/**
 * Whether a dry run reports anything pending on a database that needs no migration.
 *
 * This is what decides whether an instance may start: the startup dry run stops the instance only when
 * something would actually change, because a rolled back schema migration leaves the compiled schema out
 * of step with the database. While that exit was unconditional, a fresh instance with no data and no
 * structr.conf could not start at all.
 *
 * The startup path itself cannot be tested here, since Services skips migrations entirely under
 * isTesting(). What is testable is the signal that path now depends on.
 */
public class MigrationDryRunTest extends StructrTest {

	@Test
	public void testADatabaseThatNeedsNoMigrationReportsNothingPending() {

		try {

			assertFalse("a dry run on a database with nothing to migrate must not report changes, "
				+ "or a fresh instance would refuse to start", MigrationService.execute(true));

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}
}
