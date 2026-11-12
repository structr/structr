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

import org.neo4j.exceptions.CypherTypeException;
import org.neo4j.graphdb.QueryExecutionException;
import org.neo4j.graphdb.Result;
import org.structr.api.ConstraintViolationException;
import org.structr.api.DataFormatException;
import org.structr.api.SyntaxErrorException;
import org.structr.api.UnknownClientException;
import org.testng.annotations.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

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

	@Test
	public void testClientErrorsAreTranslatedAsInTheBoltDriver() {

		assertTrue(EmbeddedTransaction.translate(clientError("Neo.ClientError.Statement.SyntaxError", null)) instanceof SyntaxErrorException);
		assertTrue(EmbeddedTransaction.translate(clientError("Neo.ClientError.Schema.ConstraintValidationFailed", null)) instanceof ConstraintViolationException);
		assertTrue(EmbeddedTransaction.translate(clientError("Neo.ClientError.Statement.ArgumentError", null)) instanceof UnknownClientException);
	}

	@Test
	public void testATypeErrorIsADataError() {

		// the error from the report: toString() on a list
		final CypherTypeException typeError = CypherTypeException.functionArgumentWrongType("toString()", "input", "StringArray[a, b]", List.of("STRING"), "LIST<STRING>");
		final RuntimeException translated   = EmbeddedTransaction.translate(clientError("Neo.ClientError.Statement.TypeError", typeError));

		assertTrue(translated instanceof DataFormatException, String.valueOf(translated));
	}

	@Test
	public void testAnOperationalClientErrorKeepsItsCode() {

		final RuntimeException translated = EmbeddedTransaction.translate(clientError("Neo.ClientError.Transaction.TransactionTimedOut", null));

		assertTrue(translated instanceof UnknownClientException);
		assertEquals(((UnknownClientException) translated).getErrorCode(), "Neo.ClientError.Transaction.TransactionTimedOut");
	}

	@Test
	public void testOtherErrorsAreNotTranslated() {

		final RuntimeException transientError = new QueryExecutionException("deadlock", null, "Neo.TransientError.Transaction.DeadlockDetected");
		final RuntimeException other          = new IllegalStateException("boom");

		assertSame(EmbeddedTransaction.translate(transientError), transientError);
		assertSame(EmbeddedTransaction.translate(other), other);
	}

	@Test
	public void testAnErrorWhileReadingTheRowsIsTranslated() {

		final Result result = (Result) Proxy.newProxyInstance(Result.class.getClassLoader(), new Class<?>[] { Result.class }, (proxy, method, args) -> {
			throw clientError("Neo.ClientError.Statement.SyntaxError", null);
		});

		expectThrows(SyntaxErrorException.class, () -> EmbeddedTransaction.hasNext(result));
		expectThrows(SyntaxErrorException.class, () -> EmbeddedTransaction.next(result));
	}

	private static QueryExecutionException clientError(final String code, final Throwable cause) {

		return new QueryExecutionException("query failed", cause, code);
	}
}
