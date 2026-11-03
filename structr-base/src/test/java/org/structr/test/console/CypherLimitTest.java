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
package org.structr.test.console;

import org.structr.console.Console;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/** The console prints the whole result of a Cypher query only when a LIMIT bounds the rows it returns. */
public class CypherLimitTest {

	@Test
	public void testATrailingLimitBoundsTheResult() {

		assertTrue(Console.isLimited("MATCH (n) RETURN n LIMIT 100"));
		assertTrue(Console.isLimited("match (n) return n order by n.name skip 10 limit 500;"));
		assertTrue(Console.isLimited("MATCH (n)\nRETURN n\nLIMIT 20\n"));
		assertTrue(Console.isLimited("MATCH (n) RETURN n LIMIT $count"));
		assertTrue(Console.isLimited("MATCH (n) RETURN n LIMIT 5 // the first five"));
	}

	@Test
	public void testNoLimitDoesNotBoundTheResult() {

		assertFalse(Console.isLimited(null));
		assertFalse(Console.isLimited("MATCH (n) RETURN n"));
		assertFalse(Console.isLimited("MATCH (n) WITH n LIMIT 10 MATCH (n)--(m) RETURN m"));
		assertFalse(Console.isLimited("MATCH (n:LIMIT) RETURN n.limit"));
	}

	@Test
	public void testALimitInASubqueryStringOrCommentDoesNotCount() {

		assertFalse(Console.isLimited("CALL { MATCH (n) RETURN n LIMIT 10 } MATCH (n)--(m) RETURN m"));
		assertFalse(Console.isLimited("MATCH (n) WHERE n.name = 'LIMIT 10' RETURN n"));
		assertFalse(Console.isLimited("MATCH (n) RETURN n /* LIMIT 10 */"));
		assertFalse(Console.isLimited("MATCH (n) RETURN n, [x IN range(1, 100) | x][0..5] AS l"));
	}

	@Test
	public void testEveryPartOfAUnionNeedsALimit() {

		assertTrue(Console.isLimited("MATCH (n:A) RETURN n LIMIT 10 UNION ALL MATCH (n:B) RETURN n LIMIT 10"));
		assertFalse(Console.isLimited("MATCH (n:A) RETURN n LIMIT 10 UNION MATCH (n:B) RETURN n"));
	}
}
