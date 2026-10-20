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
package org.structr.api;

import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertSame;
import static org.testng.AssertJUnit.fail;

public class RetryExceptionTest {

	@Test
	public void testTransactionThatKeepsFailingIsGivenUp() {

		final RetryException rex = new RetryException("terminated");
		boolean retry            = true;
		int attempts             = 0;

		try {

			while (retry) {

				// a transaction that fails with a transient error every time
				retry = RetryException.retry(rex, ++attempts);
			}

			fail("A transaction that keeps failing must not be repeated forever.");

		} catch (RetryException thrown) {

			assertSame("The exception that terminated the last attempt must be thrown on", rex, thrown);
		}

		assertEquals("Wrong number of attempts", RetryException.MAX_ATTEMPTS, attempts);
	}

	@Test
	public void testDeadlockIsRepeatedAfterShortPauses() {

		final RetryException rex = new RetryException("deadlock");

		for (int attempts = 1; attempts < RetryException.MAX_ATTEMPTS; attempts++) {

			assertEquals("Wrong pause after attempt " + attempts, attempts * 10L, RetryException.delay(rex, attempts));
		}
	}

	@Test
	public void testSchemaChangeIsRepeatedAfterExponentiallyGrowingPauses() {

		final RetryException rex = new SchemaChangedException("schema changed", null);
		final long[] expected    = { 100, 200, 400, 800, 1600, 3200, 5000, 5000, 5000 };

		for (int attempts = 1; attempts < RetryException.MAX_ATTEMPTS; attempts++) {

			assertEquals("Wrong pause after attempt " + attempts, expected[attempts - 1], RetryException.delay(rex, attempts));
		}
	}

	@Test
	public void testTransactionThatSucceedsAfterAFailureIsRepeated() {

		final RetryException rex = new RetryException("terminated");
		boolean retry            = true;
		int attempts             = 0;

		while (retry) {

			retry = false;

			// the first attempt fails, the second one succeeds
			if (attempts == 0) {

				retry = RetryException.retry(rex, ++attempts);
			}
		}

		assertEquals("Wrong number of failed attempts", 1, attempts);
	}
}
