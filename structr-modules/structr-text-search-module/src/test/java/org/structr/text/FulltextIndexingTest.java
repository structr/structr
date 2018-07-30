/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.text;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Assert;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.structr.common.error.FrameworkException;
import org.structr.common.fulltext.Indexable;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;

/**
 *
 */
public class FulltextIndexingTest extends TextSearchModuleTest {

	private static final List<String> testWords = Arrays.asList(new String[] {
		"aenean", "eget", "amet", "donec", "quis", "sit", "ante", "enim", "etiam", "justo", "lorem", "nec", "nisi", "quam", "rhoncus", "sem", "tellus",
		"tincidunt", "ultricies", "vel", "vitae", "adipiscing", "consequat", "dapibus", "dolor", "eleifend", "faucibus", "felis", "fringilla",
		"imperdiet", "ipsum", "leo", "libero", "ligula", "maecenas", "massa", "nam", "nulla", "nullam", "pede", "pretium", "sed", "semper", "tempus",
		"venenatis", "viverra", "vulputate", "aliquam", "aliquet", "arcu", "augue", "blandit", "commodo", "condimentum", "consectetuer", "cras", "cum",
		"curabitur", "dictum", "dis", "dui", "duis", "elementum", "elit", "eros", "feugiat", "hendrerit", "integer", "laoreet", "luctus", "magna",
		"magnis", "mauris", "metus", "mollis", "montes", "mus", "nascetur", "natoque", "neque", "nibh", "nunc", "odio", "orci", "parturient",
		"pellentesque", "penatibus", "phasellus", "porttitor", "pulvinar", "quisque", "ridiculus", "rutrum", "sagittis", "sapien", "sociis", "sodales",
		"ullamcorper", "varius", "vivamus"
	});

	@Test
	public void testSearch() {

		String dates   = null;
		String text    = null;
		String numbers = null;

		try (final Tx tx = app.tx()) {

			dates   = FileHelper.createFile(securityContext, "31.12.2018, 01.01.2017, 2018-01-01, 2019/06/31, 1/2/19, Nov 1, 2019".getBytes("utf-8"), "text/plain", File.class, "dates1.txt").getUuid();
			text    = FileHelper.createFile(securityContext, "This is a test. With several sentences, punctuation, and words; more words in it.".getBytes("utf-8"), "text/plain", File.class, "text.txt").getUuid();
			numbers = FileHelper.createFile(securityContext, "23,32 455.23 32,1 -3 -422,70 1.533,23 1,6753.12 12 1 2 3 12, 145 -12 -14,23".getBytes("utf-8"), "text/plain", File.class, "numbers.txt").getUuid();

			tx.success();

		} catch (FrameworkException|IOException fex) {
			fail("Unexpected exception.");
		}

		delay();

		testResult("31.12.2018",  dates);
		testResult("2019/06/31",  dates);
		testResult("2017",        dates);
		testResult("Punctuation", text);
		testResult("23,32",       numbers);
		testResult("455",         numbers);

		// more than a single result
		testResult("12", dates, numbers);

		// no results expected
		testResult("words");
		testResult("this");
		testResult("and");
		testResult("in");
	}

	@Test
	public void testODTSearch() {

		String uuid = null;

		try (final Tx tx = app.tx()) {

			try( final InputStream is = FulltextIndexingTest.class.getResourceAsStream("/test/test.odt")) {
				uuid = FileHelper.createFile(securityContext, is, "", File.class, "test.odt").getUuid();
			}

			tx.success();

		} catch (FrameworkException|IOException fex) {
			fail("Unexpected exception.");
		}

		delay();

		try (final Tx tx = app.tx()) {

			final PropertyKey key        = StructrApp.key(Indexable.class, "indexedWords");
			final List<Indexable> result = app.nodeQuery(Indexable.class).and(key, Arrays.asList("sit"), false).getAsList();

			Assert.assertEquals("Invalid index query result size", 1, result.size());
			Assert.assertEquals("Invalid index query result", uuid, result.get(0).getUuid());

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testODT() {

		try (final Tx tx = app.tx()) {

			try( final InputStream is = FulltextIndexingTest.class.getResourceAsStream("/test/test.odt")) {
				FileHelper.createFile(securityContext, is, "", File.class, "test.odt");
			}

			tx.success();

		} catch (FrameworkException|IOException fex) {
			fail("Unexpected exception.");
		}

		delay();
		testFile();
	}

	@Test
	public void testPDF() {

		try (final Tx tx = app.tx()) {

			try (final InputStream is = FulltextIndexingTest.class.getResourceAsStream("/test/test.pdf")) {
				FileHelper.createFile(securityContext, is, "", File.class, "test.pdf");
			}

			tx.success();

		} catch (FrameworkException|IOException fex) {
			fail("Unexpected exception.");
		}

		delay();
		testFile();
	}

	@Test
	public void testPlaintext01() {

		try (final Tx tx = app.tx()) {

			try(final InputStream is = FulltextIndexingTest.class.getResourceAsStream("/test/test.txt")) {
				FileHelper.createFile(securityContext, is, "", File.class, "test.txt");
			}

			tx.success();

		} catch (FrameworkException|IOException fex) {
			fail("Unexpected exception.");
		}

		delay();
		testFile();
	}

	@Test
	public void testPlaintext02() {

		try (final Tx tx = app.tx()) {

			try(final InputStream is = FulltextIndexingTest.class.getResourceAsStream("/test/test2.txt")) {
				FileHelper.createFile(securityContext, is, "", File.class, "test2.txt");
			}

			tx.success();

		} catch (FrameworkException|IOException fex) {
			fail("Unexpected exception.");
		}

		delay();

		// test result
		try (final Tx tx = app.tx()) {

			final File file = app.nodeQuery(File.class).getFirst();

			Assert.assertNotNull("File should exist", file);

			final List<String> indexedWords  = file.getProperty(StructrApp.key(File.class, "indexedWords"));

			Assert.assertNotNull("There should be at least one indexed word", indexedWords);

			final List<String> expected     = Arrays.asList(new String[] {
				"characters", "ignoring", "repeated", "repetition", "test"
			});

			Assert.assertEquals("Invalid number of extracted words", 5, indexedWords.size());
			Assert.assertEquals("Invalid extracted word list", expected, indexedWords);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}


	}

	// ----- private methods -----
	private void testFile() {

		// test result
		try (final Tx tx = app.tx()) {

			final File file = app.nodeQuery(File.class).getFirst();

			Assert.assertNotNull("File should exist", file);

			final List<String> indexedWords  = file.getProperty(StructrApp.key(File.class, "indexedWords"));

			Assert.assertNotNull("There should be at least one indexed word", indexedWords);
			Assert.assertEquals("Invalid number of extracted words", 100, indexedWords.size());
			Assert.assertEquals("Invalid extracted word list", testWords, indexedWords);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}
	}

	private void testResult(final String searchTerm, final String... expectedUuids) {

		try (final Tx tx = app.tx()) {

			final PropertyKey key        = StructrApp.key(Indexable.class, "indexedWords");
			final List<Indexable> result = app.nodeQuery(Indexable.class).and(key, Arrays.asList(searchTerm), false).getAsList();
			final Set<String> resultIds  = result.stream().map(GraphObject::getUuid).collect(Collectors.toSet());

			Assert.assertEquals("Invalid index query result size", expectedUuids.length, result.size());

			// assert that all UUIDs are in the set
			for (final String id : expectedUuids) {

				Assert.assertEquals("Invalid index query result", true, resultIds.contains(id));
			}

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

	}

	private void delay() {
		delay(3000);
	}

	private void delay(final long time) {

		try {

			Thread.sleep(time);

		} catch (Throwable t) { }
	}
}

