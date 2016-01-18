/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.web.common;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.io.IOUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.Tx;
import org.structr.dynamic.File;

/**
 *
 *
 */
public class FileHelperTest extends StructrUiTest {

	public void testExtensionBasedMimeTypeDetection() {

		final Map<String, Map<String, byte[]>> testMap = new LinkedHashMap<>();

		testMap.put("text/html",              toMap(new Pair("test.html", "<!DOCTYPE html><html><head><title>Test</title></head><body><h1>Test</h1></body></html>".getBytes()), new Pair("test.htm", "<!DOCTYPE html>".getBytes())));
		testMap.put("text/plain",             toMap(new Pair("test.txt",  "Hello world!".getBytes())));
		testMap.put("text/css",               toMap(new Pair("test.css",  "body { background-color: #ffffff; }".getBytes())));
		testMap.put("application/javascript", toMap(new Pair("test.js",   "function() { alert('Test'); }".getBytes())));
		testMap.put("application/zip",        toMap(new Pair("test.zip",  "".getBytes())));
		testMap.put("image/jpeg",             toMap(new Pair("test.jpg",  "".getBytes()), new Pair("test.jpeg",   "".getBytes())));
		testMap.put("image/png",              toMap(new Pair("test.png",  "".getBytes())));

		try (final Tx tx = app.tx()) {

			for (final Entry<String, Map<String, byte[]>> entry : testMap.entrySet()) {

				final String mimeType = entry.getKey();

				for (final Entry<String, byte[]> fileEntry : entry.getValue().entrySet()) {

					final String fileName = fileEntry.getKey();
					final byte[] content  = fileEntry.getValue();

					try {
						final File file = FileHelper.createFile(securityContext, content, null, File.class, fileName);
						assertEquals("MIME type detection failed", mimeType, file.getContentType());

					} catch (IOException ioex) {

						ioex.printStackTrace();
						fail("Unexpected exception");
					}

				}
			}

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
		}


	}

	public void testContentBasedMimeTypeDetection() {

		final Map<String, Map<String, byte[]>> testMap = new LinkedHashMap<>();

		try {

			// text-based formats will of course resolved into "text/plain"
			testMap.put("text/plain",               toMap(new Pair("test01", "<!DOCTYPE html><html><head><title>Test</title></head><body><h1>Test</h1></body></html>".getBytes())));
			testMap.put("text/plain",               toMap(new Pair("test02", "Hello world!".getBytes())));
			testMap.put("text/plain",               toMap(new Pair("test03", "body { background-color: #ffffff; }".getBytes())));

			// disabled because jmimemagic detects matlab..
			// testMap.put("text/plain",               toMap(new Pair("test04", "function test() { alert('Test'); return 'Hello world!'; }".getBytes())));

			testMap.put("application/zip",          toMap(new Pair("test05", IOUtils.toByteArray(FileHelperTest.class.getResourceAsStream("/test/test.zip")))));
			testMap.put("image/jpeg",               toMap(new Pair("test06", IOUtils.toByteArray(FileHelperTest.class.getResourceAsStream("/test/test.jpg")))));
			testMap.put("image/png",                toMap(new Pair("test07", IOUtils.toByteArray(FileHelperTest.class.getResourceAsStream("/test/test.png")))));
			testMap.put("image/gif",                toMap(new Pair("test08", IOUtils.toByteArray(FileHelperTest.class.getResourceAsStream("/test/test.gif")))));

			// disabled because jmimemagic v0.1.2 does not properly detect image/tiff cross-OS
			// testMap.put("image/tiff",               toMap(new Pair("test09", IOUtils.toByteArray(FileHelperTest.class.getResourceAsStream("/test/test.tiff")))));

			testMap.put("image/bmp",                toMap(new Pair("test10", IOUtils.toByteArray(FileHelperTest.class.getResourceAsStream("/test/test.bmp")))));
			testMap.put("image/vnd.microsoft.icon", toMap(new Pair("test11", IOUtils.toByteArray(FileHelperTest.class.getResourceAsStream("/test/test.ico")))));

		} catch (IOException ioex) {
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			for (final Entry<String, Map<String, byte[]>> entry : testMap.entrySet()) {

				final String mimeType = entry.getKey();

				for (final Entry<String, byte[]> fileEntry : entry.getValue().entrySet()) {

					final String fileName = fileEntry.getKey();
					final byte[] content  = fileEntry.getValue();

					try {
						final File file = FileHelper.createFile(securityContext, content, null, File.class, fileName);
						assertEquals("MIME type detection failed for " + fileName, mimeType, file.getContentType());

					} catch (IOException ioex) {

						ioex.printStackTrace();
						fail("Unexpected exception");
					}

				}
			}

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
		}


	}
}
