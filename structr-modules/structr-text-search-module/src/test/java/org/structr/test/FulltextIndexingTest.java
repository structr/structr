/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.test;

import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.test.web.IndexingTest;
import org.structr.web.common.FileHelper;
import org.structr.web.traits.definitions.FileTraitDefinition;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.testng.AssertJUnit.*;

/**
 *
 */
public class FulltextIndexingTest extends IndexingTest {

	@Test
	public void testODTSearch() {

		try (final Tx tx = app.tx()) {

			try( final InputStream is = FulltextIndexingTest.class.getResourceAsStream("/test/test.odt")) {
				FileHelper.createFile(securityContext, is, "", StructrTraits.FILE, "test.odt").getUuid();
			}

			tx.success();

		} catch (FrameworkException|IOException fex) {
			fail("Unexpected exception.");
		}

		waitForIndex();
		testFile("sit");
	}

	@Test
	public void testODT() {

		try (final Tx tx = app.tx()) {

			try( final InputStream is = FulltextIndexingTest.class.getResourceAsStream("/test/test.odt")) {
				FileHelper.createFile(securityContext, is, "", StructrTraits.FILE, "test.odt");
			}

			tx.success();

		} catch (FrameworkException|IOException fex) {
			fail("Unexpected exception.");
		}

		waitForIndex();
		testFile("sit");
	}

	@Test
	public void testPDF() {

		try (final Tx tx = app.tx()) {

			try (final InputStream is = FulltextIndexingTest.class.getResourceAsStream("/test/test.pdf")) {
				FileHelper.createFile(securityContext, is, "", StructrTraits.FILE, "test.pdf");
			}

			tx.success();

		} catch (FrameworkException|IOException fex) {
			fail("Unexpected exception.");
		}

		waitForIndex();
		testFile("sit");
	}

	@Test
	public void testPlaintext01() {

		try (final Tx tx = app.tx()) {

			try(final InputStream is = FulltextIndexingTest.class.getResourceAsStream("/test/test.txt")) {
				FileHelper.createFile(securityContext, is, "", StructrTraits.FILE, "test.txt");
			}

			tx.success();

		} catch (FrameworkException|IOException fex) {
			fail("Unexpected exception.");
		}

		waitForIndex();
		testFile("sit");
	}

	@Test
	public void testPlaintext02() {

		try (final Tx tx = app.tx()) {

			try(final InputStream is = FulltextIndexingTest.class.getResourceAsStream("/test/test2.txt")) {
				FileHelper.createFile(securityContext, is, "", StructrTraits.FILE, "test2.txt");
			}

			tx.success();

		} catch (FrameworkException|IOException fex) {
			fail("Unexpected exception.");
		}

		waitForIndex();
		testFile("ignoring");
	}

	// ----- private methods -----
	private void testFile(final String searchText) {

		final PropertyKey key = Traits.of(StructrTraits.FILE).key(FileTraitDefinition.EXTRACTED_CONTENT_PROPERTY);
		final long timeout    = System.currentTimeMillis() + 20000;
		String value = null;

		// wait for value (indexer is async)
		while (value == null) {

			// test result
			try (final Tx tx = app.tx()) {

				final NodeInterface file = app.nodeQuery(StructrTraits.FILE).getFirst();

				assertNotNull("File should exist", file);

				value = (String) file.getProperty(key);

				System.out.println(value);

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}

			if (value == null) {

				try {
					Thread.sleep(1000);
				} catch (Throwable t) {
				}

				if (System.currentTimeMillis() > timeout) {
					throw new RuntimeException("Timeout waiting for indexer to write content into property value.");
				}
			}
		}

		try (final Tx tx = app.tx()) {

			final List<NodeInterface> result = app.nodeQuery(StructrTraits.FILE).key(key, searchText, false).getAsList();

			assertEquals("Invalid index query result size", 1, result.size());

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final List<NodeInterface> result = app.nodeQuery(StructrTraits.FILE).fulltext(key, searchText).getAsList();

			assertEquals("Invalid index query result size", 1, result.size());

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}
	}

	private void waitForIndex() {
		waitForIndex(60);
	}

	private void waitForIndex(final long seconds) {

		System.out.println("Waiting for index updater..");

		// wait for one minute maximum
		final long timeoutTimestamp = System.currentTimeMillis() + (seconds * 1000);

		while (System.currentTimeMillis() < timeoutTimestamp) {

			if (StructrApp.getInstance().getDatabaseService().isIndexUpdateFinished()) {

				System.out.println("########## Index updater has finished, waiting some more before returning...");
				try { Thread.sleep(10000); } catch (Throwable t) {}

				return;
			}

			try { Thread.sleep(1000); } catch (Throwable t) {}
		}

		throw new RuntimeException("Timeout waiting for index update.");
	}
}

