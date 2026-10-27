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
package org.structr.embedded;

import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * The detection of a query that Cypher gave up compiling because the schema changed too often, which is repeated.
 */
public class EmbeddedTransactionTest {

	private static final String MESSAGE = "Could not compile query due to insanely frequent schema changes";

	@Test
	public void testFrequentSchemaChangeIsDetected() {

		assertTrue(EmbeddedTransaction.isFrequentSchemaChange(new IllegalStateException(MESSAGE)));
		assertTrue(EmbeddedTransaction.isFrequentSchemaChange(new RuntimeException("Query failed", new IllegalStateException(MESSAGE))));
	}

	@Test
	public void testOtherErrorsAreNotRepeated() {

		assertFalse(EmbeddedTransaction.isFrequentSchemaChange(new IllegalStateException("Invalid input 'MATCHX'")));
		assertFalse(EmbeddedTransaction.isFrequentSchemaChange(new RuntimeException((String) null)));
		assertFalse(EmbeddedTransaction.isFrequentSchemaChange(null));
	}
}
