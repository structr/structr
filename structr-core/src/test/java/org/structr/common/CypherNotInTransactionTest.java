/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.common;

import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.api.NativeResult;
import org.structr.api.NotFoundException;
import org.structr.api.graph.Relationship;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.SixOneOneToOne;
import org.structr.core.entity.TestOne;
import org.structr.core.entity.TestSix;
import org.structr.core.graph.Tx;

/**
 *
 *
 */
public class CypherNotInTransactionTest extends StructrTest {

	private static final Logger logger = LoggerFactory.getLogger(CypherNotInTransactionTest.class.getName());

	@Override
	public void test00DbAvailable() {

		super.test00DbAvailable();

	}

	public void test01DeleteAfterLookupWithCypherInTransaction() {

		try {

			final TestSix testSix = this.createTestNode(TestSix.class);
			final TestOne testOne = this.createTestNode(TestOne.class);
			SixOneOneToOne rel            = null;

			assertNotNull(testSix);
			assertNotNull(testOne);

			try (final Tx tx = app.tx()) {

				rel = app.create(testSix, testOne, SixOneOneToOne.class);
				tx.success();
			}

			assertNotNull(rel);

			DatabaseService graphDb = graphDbCommand.execute();

			try (final Tx tx = app.tx()) {

				NativeResult<Relationship> result = graphDb.execute("start n = node(*) match (n)<-[r:ONE_TO_ONE]-() return r");
				final Iterator<Relationship> rels = result.columnAs("r");

				assertTrue(rels.hasNext());

				rels.next().delete();

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				rel.getUuid();
				fail("Accessing a deleted relationship should thow an exception.");

				tx.success();

			} catch (NotFoundException iex) {
			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test03DeleteDirectly() {

		try {

			final TestOne testOne  = createTestNode(TestOne.class);
			final TestSix testSix  = createTestNode(TestSix.class);
			SixOneOneToOne rel     = null;

			assertNotNull(testOne);
			assertNotNull(testSix);

			try (final Tx tx = app.tx()) {

				rel = app.create(testSix, testOne, SixOneOneToOne.class);
				tx.success();
			}

			assertNotNull(rel);

			try (final Tx tx = app.tx()) {

				testOne.getRelationships().iterator().next().getRelationship().delete();
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				rel.getUuid();
				fail("Accessing a deleted relationship should thow an exception.");

				tx.success();

			} catch (NotFoundException nfex) {
				assertNotNull(nfex.getMessage());
			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test04DeleteAfterIndexLookup() {

		try {

			final TestOne testOne  = createTestNode(TestOne.class);
			final TestSix testSix  = createTestNode(TestSix.class);
			SixOneOneToOne rel     = null;

			assertNotNull(testOne);
			assertNotNull(testSix);

			try (final Tx tx = app.tx()) {

				rel = app.create(testSix, testOne, SixOneOneToOne.class);
				tx.success();
			}

			assertNotNull(rel);

			try (final Tx tx = app.tx()) {

				GraphObject  searchRes = app.getNodeById(testSix.getUuid());
				assertNotNull(searchRes);

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				testSix.getRelationships().iterator().next().getRelationship().delete();

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				rel.getUuid();
				fail("Accessing a deleted relationship should thow an exception.");

				tx.success();

			} catch (NotFoundException nfex) {
				assertNotNull(nfex.getMessage());
			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test05RollbackDelete() {

		try {

			final TestOne testOne = createTestNode(TestOne.class);
			final TestSix testSix = createTestNode(TestSix.class);
			String relId          = null;
			SixOneOneToOne rel    = null;

			assertNotNull(testOne);
			assertNotNull(testSix);

			try (final Tx tx = app.tx()) {

				rel   = app.create(testSix, testOne, SixOneOneToOne.class);
				relId = rel.getUuid();
				tx.success();
			}

			assertNotNull(rel);

			try (final Tx tx = app.tx()) {

				// do not commit transaction
				testOne.getRelationships().iterator().next().getRelationship().delete();
			}

			try (final Tx tx = app.tx()) {

				assertEquals("UUID of relationship should be readable after rollback", relId, rel.getUuid());
				tx.success();

			} catch (NotFoundException iex) {

			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}
}
