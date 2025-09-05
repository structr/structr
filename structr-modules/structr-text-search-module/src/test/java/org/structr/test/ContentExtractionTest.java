/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MimeTypes;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.structr.common.error.FrameworkException;
import org.structr.core.function.Functions;
import org.structr.core.graph.Tx;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import org.structr.test.web.StructrUiTest;
import org.structr.text.FulltextTokenizer;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import static org.testng.AssertJUnit.*;

/**
 *
 */
public class ContentExtractionTest extends StructrUiTest {

	@Test
	public void testContentExtraction() {

		try (final Tx tx = app.tx()) {

			try (final InputStream is = ContentExtractionTest.class.getResourceAsStream("/test/test.pdf")) {

				final Detector detector = new DefaultDetector(MimeTypes.getDefaultMimeTypes());

				try (final FulltextTokenizer tokenizer = new FulltextTokenizer()) {

					final AutoDetectParser parser = new AutoDetectParser(detector);
					final Metadata metadata       = new Metadata();

					parser.parse(is,new BodyContentHandler(tokenizer), metadata);

					final String expected = "Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. \n" +
						"Aenean massa. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus \n" +
						"mus. Donec quam felis, ultricies nec, pellentesque eu, pretium quis, sem. Nulla consequat massa \n" +
						"quis enim. Donec pede justo, fringilla vel, aliquet nec, vulputate eget, arcu. In enim justo, rhoncus \n" +
						"ut, imperdiet a, venenatis vitae, justo. Nullam dictum felis eu pede mollis pretium. Integer tincidunt.\n" +
						"Cras dapibus. Vivamus elementum semper nisi. Aenean vulputate eleifend tellus. Aenean leo ligula, \n" +
						"porttitor eu, consequat vitae, eleifend ac, enim. Aliquam lorem ante, dapibus in, viverra quis, feugiat\n" +
						"a, tellus. Phasellus viverra nulla ut metus varius laoreet. Quisque rutrum. Aenean imperdiet. Etiam \n" +
						"ultricies nisi vel augue. Curabitur ullamcorper ultricies nisi. Nam eget dui. Etiam rhoncus. \n" +
						"Maecenas tempus, tellus eget condimentum rhoncus, sem quam semper libero, sit amet adipiscing \n" +
						"sem neque sed ipsum. Nam quam nunc, blandit vel, luctus pulvinar, hendrerit id, lorem. Maecenas \n" +
						"nec odio et ante tincidunt tempus. Donec vitae sapien ut libero venenatis faucibus. Nullam quis \n" +
						"ante. Etiam sit amet orci eget eros faucibus tincidunt. Duis leo. Sed fringilla mauris sit amet nibh. \n" +
						"Donec sodales sagittis magna.";

					final String actual = tokenizer.getRawText();

					assertEquals("Invalid content extraction result", expected, actual);

				} catch (TikaException e) {
					e.printStackTrace();
					fail("Unexpected exception.");
				} catch (SAXException e) {
					e.printStackTrace();
					fail("Unexpected exception.");
				}
			}

			tx.success();

		} catch (FrameworkException|IOException fex) {
			fail("Unexpected exception.");
		}

		// wait for indexing to finish
		try { Thread.sleep(1000); } catch (Throwable t) {}
	}

	@Test
	public void testStopWordsFunction() {

		try {

			final Function<Object, Object> function = Functions.get("stop_words");

			assertNotNull("StopWords function does not exist", function);

			final Set<String> words = (Set) function.apply(new ActionContext(securityContext), null, new String[] { "en" });

			assertNotNull("StopWords function should return a set of words for English", words);

			assertTrue("StopWords for English should contain some words", words.contains("is"));
			assertTrue("StopWords for English should contain some words", words.contains("not"));
			assertTrue("StopWords for English should contain some words", words.contains("he"));

		} catch (FrameworkException e) {
			throw new RuntimeException(e);
		}


	}
}

