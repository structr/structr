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
package org.structr.test.common;

import org.structr.api.DatabaseFeature;
import org.structr.api.DatabaseService;
import org.structr.api.NativeQuery;
import org.structr.api.util.Iterables;
import org.structr.api.util.QueryHistogram;
import org.structr.core.Services;
import org.structr.core.graph.GraphDatabaseCommand;
import org.structr.core.graph.Tx;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Every driver that runs Cypher has to time its queries, or the histogram endpoint answers with an
 * empty list. The in-memory driver runs no Cypher and is skipped.
 */
public class QueryHistogramTest extends StructrTest {

	@Test
	public void testQueriesAreRecordedPerStatement() {

		final DatabaseService<?> db = Services.getInstance().getDatabaseService();

		if (!db.supportsFeature(DatabaseFeature.QueryLanguage, "application/x-cypher-query")) {

			return;
		}

		final String statement = "MATCH (n:" + randomTenantId + ") RETURN count(n) AS count // query histogram test";

		QueryHistogram.clear();

		try {

			createTestNodes("TestOne", 3);

			for (int i = 0; i < 2; i++) {

				try (final Tx tx = app.tx()) {

					// read to the end, so the query is finished while the transaction is still open
					final NativeQuery<Iterable> query = db.query(statement, Iterable.class);

					Iterables.toList(db.execute(query));

					tx.success();
				}
			}

			final List<Map<String, Object>> histogram = QueryHistogram.analyze("count", 10_000);
			final Map<String, Object> entry           = histogram.stream().filter(e -> statement.equals(e.get("Query"))).findFirst().orElse(null);

			assertNotNull("the statement must appear in the histogram, it has " + histogram.size() + " entries", entry);
			assertEquals(BigDecimal.valueOf(2), entry.get("Count"));
			assertTrue(((BigDecimal) entry.get("Overall time (s)")).signum() >= 0);
			assertTrue(((BigDecimal) entry.get("Minimum time (s)")).compareTo((BigDecimal) entry.get("Maximum time (s)")) <= 0);

		} catch (Exception ex) {

			ex.printStackTrace();
			fail("Unexpected exception: " + ex.getMessage());
		}
	}
}
