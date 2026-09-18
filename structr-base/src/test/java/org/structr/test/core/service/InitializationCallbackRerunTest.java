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
package org.structr.test.core.service;

import org.structr.api.service.InitializationCallback;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 * The opt-in that decides whether an initialization callback runs again after the database is replaced.
 *
 * The re-run itself happens when NodeService restarts onto a newly configured database, which no test
 * can reach: the harness never reconfigures a running instance. What is worth pinning is the default,
 * because it is what keeps every existing callback on its exactly-once behaviour. Flipping it would make
 * five community registrants, and an unknown number of Enterprise ones, start running twice without any
 * of them being written for it.
 */
public class InitializationCallbackRerunTest {

	@Test
	public void testACallbackDoesNotRunAgainUnlessItAsksTo() {

		// a callback written the ordinary way, as all current registrants are
		final InitializationCallback ordinary = () -> {};

		assertFalse("re-running must be opt-in: every existing callback assumes it runs exactly once", ordinary.rerunAfterDatabaseChange());
	}

	@Test
	public void testACallbackThatSeedsDataCanAskToRunAgain() {

		final InitializationCallback seeding = new InitializationCallback() {

			@Override
			public void initializationDone() {
			}

			@Override
			public boolean rerunAfterDatabaseChange() {

				return true;
			}
		};

		assertTrue(seeding.rerunAfterDatabaseChange());
	}
}
