/**
 * Copyright (C) 2010-2017 Structr GmbH
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
import org.junit.Assert;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.structr.common.error.FrameworkException;
import org.structr.common.fulltext.Indexable;
import org.structr.core.graph.Tx;
import org.structr.dynamic.File;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.FileBase;

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
	public void testODT() {

		try (final Tx tx = app.tx()) {

			final InputStream is = FulltextIndexingTest.class.getResourceAsStream("/test/test.odt");
			FileHelper.createFile(securityContext, is, "", File.class, "test.odt");

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

			final InputStream is = FulltextIndexingTest.class.getResourceAsStream("/test/test.pdf");
			FileHelper.createFile(securityContext, is, "", File.class, "test.pdf");

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

			final InputStream is = FulltextIndexingTest.class.getResourceAsStream("/test/test.txt");
			FileHelper.createFile(securityContext, is, "", File.class, "test.txt");

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

			final InputStream is = FulltextIndexingTest.class.getResourceAsStream("/test/test2.txt");
			FileHelper.createFile(securityContext, is, "", File.class, "test2.txt");

			tx.success();

		} catch (FrameworkException|IOException fex) {
			fail("Unexpected exception.");
		}

		delay();

		// test result
		try (final Tx tx = app.tx()) {

			final FileBase file = app.nodeQuery(File.class).getFirst();

			Assert.assertNotNull("File should exist", file);

			final String[] rawIndexedWords  = file.getProperty(Indexable.indexedWords);

			Assert.assertNotNull("There should be at least one indexed word", rawIndexedWords);

			final List<String> indexedWords = Arrays.asList(rawIndexedWords);
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

			final FileBase file = app.nodeQuery(File.class).getFirst();

			Assert.assertNotNull("File should exist", file);

			final String[] rawIndexedWords  = file.getProperty(Indexable.indexedWords);

			Assert.assertNotNull("There should be at least one indexed word", rawIndexedWords);

			final List<String> indexedWords = Arrays.asList(rawIndexedWords);

			Assert.assertEquals("Invalid number of extracted words", 100, indexedWords.size());
			Assert.assertEquals("Invalid extracted word list", testWords, indexedWords);

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

