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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.dynamic.File;
import org.structr.web.common.ImageHelper.Thumbnail;
import org.structr.web.entity.FileBase;
import org.structr.web.entity.Folder;
import org.structr.web.entity.Image;
import org.structr.web.entity.TestImage;

/**
 *
 * @author Christian Morgner
 */
public class UiTest extends StructrUiTest {

	private static String base64Image  = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAWAAAABUCAYAAAC8/e1DAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAA2ZpVFh0WE1MOmNvbS5hZG9iZS54bXAAAAAAADw/eHBhY2tldCBiZWdpbj0i77u/IiBpZD0iVzVNME1wQ2VoaUh6cmVTek5UY3prYzlkIj8+IDx4OnhtcG1ldGEgeG1sbnM6eD0iYWRvYmU6bnM6bWV0YS8iIHg6eG1wdGs9IkFkb2JlIFhNUCBDb3JlIDUuMy1jMDExIDY2LjE0NTY2MSwgMjAxMi8wMi8wNi0xNDo1NjoyNyAgICAgICAgIj4gPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4gPHJkZjpEZXNjcmlwdGlvbiByZGY6YWJvdXQ9IiIgeG1sbnM6eG1wTU09Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9tbS8iIHhtbG5zOnN0UmVmPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvc1R5cGUvUmVzb3VyY2VSZWYjIiB4bWxuczp4bXA9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC8iIHhtcE1NOk9yaWdpbmFsRG9jdW1lbnRJRD0ieG1wLmRpZDo2RjYyNjlFMUNFMTNFMjExQTQ2N0ZGMDI2MEZEQ0Q3NSIgeG1wTU06RG9jdW1lbnRJRD0ieG1wLmRpZDo2MDcwOEExQzEzRDMxMUUyQTMyQzlEQjBGNTBBQUUwMSIgeG1wTU06SW5zdGFuY2VJRD0ieG1wLmlpZDo2MDcwOEExQjEzRDMxMUUyQTMyQzlEQjBGNTBBQUUwMSIgeG1wOkNyZWF0b3JUb29sPSJBZG9iZSBQaG90b3Nob3AgQ1M2IChXaW5kb3dzKSI+IDx4bXBNTTpEZXJpdmVkRnJvbSBzdFJlZjppbnN0YW5jZUlEPSJ4bXAuaWlkOkYzODhBQzYwRDIxM0UyMTFBNDY3RkYwMjYwRkRDRDc1IiBzdFJlZjpkb2N1bWVudElEPSJ4bXAuZGlkOjZGNjI2OUUxQ0UxM0UyMTFBNDY3RkYwMjYwRkRDRDc1Ii8+IDwvcmRmOkRlc2NyaXB0aW9uPiA8L3JkZjpSREY+IDwveDp4bXBtZXRhPiA8P3hwYWNrZXQgZW5kPSJyIj8+xNxK9AAAAt9JREFUeNrs3TFOKlEUgGGuIWhGSwmVLoMlWFi8ddiZuA41sTMuw9LlYGWBEqOE4DC8c4mNhSUzhvt9yQ2813GCf46DOmk8Hu9dXl7+q6rqPqU06gGwNev1+mU+n1/c3d099uPfB+Eh/nMYx3QAtmuUmxuPTznAh+IL0Kphbm8O8GC1WhkHQLsGOcCpaRqjAGhXygHu2YAB2ifAAF0GuK5rkwCwAQPYgAEQYIAdDbBLEAA2YAABBqCFALsEASDAAGUF2CUIABswQFkB9tfQAGzAAGUF2DVggI4CvFgsTALABgxQUIDdkBOgfXtGACDAAAIMgAADCDAAAgwgwAAIMIAAAyDAAH/a5leRU0omAWADBrABA2ADBtjNAK/Dm1EAtCOaO8sPOcDL19fXm6Zp3o0FYLuitbNo7nVubxqPx1U8OY1zEucojgvC3egbAYV81516Py9/5jtC5DsD19/Pd3r5jfMR5znOJH/R5xvCTeJM4wwEuNM3JpQg/RKmpoDXnl/nMs5nbm//+Pi4d3V1dVZV1X1KaeS9AbDFAq/XL/P5/OL29vYxb8AH+/v7D03TDI0GYOtGubnx+JQDfBhFHroxJ0Br8sJ7mAM8iO3XOADaNcgBTgIM0Lq0+dGn1WplFAAt2wTYBgzQUYC/vr5MAqCLALsEASDAAGUF2CUIgI4C7EM4gI4CXNe1SQAIMEBBAfYhHIAAA5QVYJcgAGzAAGUF2N8CBugowC5BAAgwQFkBXiwWJgFgAwYoKMA+hANo354RAAgwgAADIMAAAgyAAAMIMAACDCDAAAgwwJ+2+VXklJJJANiAAWzAANiAAXYzwOvwZhQA7YjmzvJDDvByOp3eNE3zbiwA2xWtnUVzr3N70/n5eRVPTuOcxDmK44JwN/pGQCHfdafez8uf+Y4Qqzj19/OdXn7jfMR5jjPJX/T5hnCTONM4AwHu9I0JJUi/hKkp4LXn17mM85nb+1+AAQDuVAgNv/BqVwAAAABJRU5ErkJggg==";
	private static final Logger logger = LoggerFactory.getLogger(UiTest.class.getName());

	@Test
	public void test01CreateThumbnail() {

		try (final Tx tx = app.tx()) {

			TestImage img = (TestImage) ImageHelper.createFileBase64(securityContext, base64Image, TestImage.class);

			img.setProperties(img.getSecurityContext(), new PropertyMap(AbstractNode.name, "test-image.png"));

			assertNotNull(img);
			assertTrue(img instanceof TestImage);

			Image tn = img.getProperty(TestImage.thumbnail);

			assertNotNull(tn);
			assertEquals(new Integer(200), tn.getWidth());
			assertEquals(new Integer(48), tn.getHeight());  // cropToFit = false
			assertEquals("image/" + Thumbnail.FORMAT, tn.getContentType());

			tx.success();

		} catch (Exception ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}
	}

	@Test
	public void test01AutoRenameThumbnail() {

		final String initialImageName = "initial_image_name.png";
		final String renamedImageName = "image_name_after_rename.png";
		Image testImage = null;

		try (final Tx tx = app.tx()) {

			testImage = (Image) ImageHelper.createFileBase64(securityContext, base64Image, Image.class);

			testImage.setProperties(testImage.getSecurityContext(), new PropertyMap(Image.name, initialImageName));

			assertNotNull(testImage);
			assertTrue(testImage instanceof Image);

			final Image tnSmall = testImage.getProperty(Image.tnSmall);
			final Image tnMid = testImage.getProperty(Image.tnMid);

			assertEquals("Initial small thumbnail name not as expected", testImage.getThumbnailName(initialImageName, tnSmall.getWidth(), tnSmall.getHeight()), tnSmall.getProperty(Image.name));
			assertEquals("Initial mid thumbnail name not as expected", testImage.getThumbnailName(initialImageName, tnMid.getWidth(), tnMid.getHeight()), tnMid.getProperty(Image.name));

			tx.success();

		} catch (Exception ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			testImage.setProperties(testImage.getSecurityContext(), new PropertyMap(Image.name, renamedImageName));
			tx.success();

		} catch (Exception ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			final Image tnSmall = testImage.getProperty(Image.tnSmall);
			final Image tnMid = testImage.getProperty(Image.tnMid);

			assertEquals("Small Thumbnail name not auto-renamed as expected", testImage.getThumbnailName(renamedImageName, tnSmall.getWidth(), tnSmall.getHeight()), tnSmall.getProperty(Image.name));
			assertEquals("Mid Thumbnail name not auto-renamed as expected", testImage.getThumbnailName(renamedImageName, tnMid.getWidth(), tnMid.getHeight()), tnMid.getProperty(Image.name));

			tx.success();

		} catch (Exception ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}

	}

	@Test
	public void test01AutoRenameThumbnailForImageSubclass() {

		TestImage subclassTestImage = null;
		final String initialImageName = "initial_image_name.png";
		final String renamedImageName = "image_name_after_rename.png";

		try (final Tx tx = app.tx()) {

			subclassTestImage = (TestImage) ImageHelper.createFileBase64(securityContext, base64Image, TestImage.class);

			subclassTestImage.setProperties(subclassTestImage.getSecurityContext(), new PropertyMap(TestImage.name, initialImageName));

			assertNotNull(subclassTestImage);
			assertTrue(subclassTestImage instanceof TestImage);

			final Image tnSmall = subclassTestImage.getProperty(Image.tnSmall);
			final Image tnMid = subclassTestImage.getProperty(Image.tnMid);
			final Image tnCustom = subclassTestImage.getProperty(TestImage.thumbnail);

			assertEquals("Initial small thumbnail name not as expected", subclassTestImage.getThumbnailName(initialImageName, tnSmall.getWidth(), tnSmall.getHeight()), tnSmall.getProperty(Image.name));
			assertEquals("Initial mid thumbnail name not as expected", subclassTestImage.getThumbnailName(initialImageName, tnMid.getWidth(), tnMid.getHeight()), tnMid.getProperty(Image.name));
			assertEquals("Initial custom thumbnail name not as expected", subclassTestImage.getThumbnailName(initialImageName, tnCustom.getWidth(), tnCustom.getHeight()), tnCustom.getProperty(Image.name));

			tx.success();

		} catch (Exception ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			subclassTestImage.setProperties(subclassTestImage.getSecurityContext(), new PropertyMap(Image.name, renamedImageName));
			tx.success();

		} catch (Exception ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			final Image tnSmall = subclassTestImage.getProperty(Image.tnSmall);
			final Image tnMid = subclassTestImage.getProperty(Image.tnMid);
			final Image tnCustom = subclassTestImage.getProperty(TestImage.thumbnail);

			assertEquals("Small Thumbnail name not auto-renamed as expected for image subclass", subclassTestImage.getThumbnailName(renamedImageName, tnSmall.getWidth(), tnSmall.getHeight()), tnSmall.getProperty(Image.name));
			assertEquals("Mid Thumbnail name not auto-renamed as expected for image subclass", subclassTestImage.getThumbnailName(renamedImageName, tnMid.getWidth(), tnMid.getHeight()), tnMid.getProperty(Image.name));
			assertEquals("Custom Thumbnail name not auto-renamed as expected for image subclass", subclassTestImage.getThumbnailName(renamedImageName, tnCustom.getWidth(), tnCustom.getHeight()), tnCustom.getProperty(Image.name));

			tx.success();

		} catch (Exception ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}

	}

	@Test
	public void testFolderPath() {

		try (final Tx tx = app.tx()) {

			Folder test4 = FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), "/a/a");
			Folder test3 = FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), "/c/b/a");
			Folder test2 = FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), "/b/a");
			Folder test1 = FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), "/a/b/c");

			tx.success();

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			Folder a = (Folder) FileHelper.getFileByAbsolutePath(SecurityContext.getSuperUserInstance(), "/a");
			assertNotNull(a);
			assertEquals(FileHelper.getFolderPath(a), "/a");

			Folder b = (Folder) FileHelper.getFileByAbsolutePath(SecurityContext.getSuperUserInstance(), "/a/b");
			assertNotNull(b);
			assertEquals(FileHelper.getFolderPath(b), "/a/b");

			Folder c = (Folder) FileHelper.getFileByAbsolutePath(SecurityContext.getSuperUserInstance(), "/a/b/c");
			assertNotNull(c);
			assertEquals(FileHelper.getFolderPath(c), "/a/b/c");

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}

	}

	@Test
	public void testAllowedCharacters() {

		try (final Tx tx = app.tx()) {

			app.create(Folder.class, "/a/b");

			tx.success();

			fail("Folder with non-allowed characters were created.");

		} catch (FrameworkException ex) {}

		try (final Tx tx = app.tx()) {

			app.create(Folder.class, "a/b");

			tx.success();

			fail("Folder with non-allowed characters were created.");

		} catch (FrameworkException ex) {}

		try (final Tx tx = app.tx()) {

			app.create(Folder.class, "/");

			tx.success();

			fail("Folder with non-allowed characters were created.");

		} catch (FrameworkException ex) {}

		try (final Tx tx = app.tx()) {

			app.create(Folder.class, "c/");

			tx.success();

			fail("Folder with non-allowed characters were created.");

		} catch (FrameworkException ex) {}

		try (final Tx tx = app.tx()) {

			app.create(Folder.class, "abc\0");

			tx.success();

			fail("Folder with non-allowed characters were created.");

		} catch (FrameworkException ex) {}

		try (final Tx tx = app.tx()) {

			app.create(Folder.class, "\0abc");

			tx.success();

			fail("Folder with non-allowed characters were created.");

		} catch (FrameworkException ex) {}

		try (final Tx tx = app.tx()) {

			app.create(Folder.class, "a\0bc");

			tx.success();

			fail("Folder with non-allowed characters were created.");

		} catch (FrameworkException ex) {}
	}

	@Test
	public void testCreateFolder() {

		Folder folder1 = null;

		try (final Tx tx = app.tx()) {

			folder1 = FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), "/folder1");
			tx.success();

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			FileBase file1 = (FileBase) app.create(File.class, "file1");
			assertNotNull(file1);
			assertEquals(FileHelper.getFolderPath(file1), "/file1");

			file1.setProperties(file1.getSecurityContext(), new PropertyMap(File.parent, folder1));
			assertEquals(FileHelper.getFolderPath(file1), "/folder1/file1");

			tx.success();

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			Image image1 = (Image) app.create(Image.class, "image1");
			assertNotNull(image1);
			assertEquals(FileHelper.getFolderPath(image1), "/image1");

			image1.setProperties(image1.getSecurityContext(), new PropertyMap(File.parent, folder1));
			assertEquals(FileHelper.getFolderPath(image1), "/folder1/image1");

			tx.success();

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			assertEquals(2, folder1.getProperty(Folder.files).size());
			assertEquals(1, folder1.getProperty(Folder.images).size());

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}
	}

	@Test
	public void testCreateBase64File() {

		final String base64Data = "data:text/plain;base64,RGllcyBpc3QgZWluIFRlc3Q=";
		final String plaintext  = "Dies ist ein Test";
		FileBase file           = null;

		try (final Tx tx = app.tx()) {

			file = app.create(File.class,
				new NodeAttribute<>(AbstractNode.name, "test.txt"),
				new NodeAttribute<>(FileBase.base64Data, base64Data)
			);

			tx.success();

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			assertEquals("Invalid base64 encoded file content creation result", plaintext, IOUtils.toString(file.getInputStream()));

			tx.success();

		} catch (FrameworkException | IOException ex) {
			logger.error("", ex);
		}

	}

	@Test
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
						final FileBase file = FileHelper.createFile(securityContext, content, null, File.class, fileName);
						assertEquals("MIME type detection failed", mimeType, file.getContentType());

					} catch (IOException ioex) {

						logger.warn("", ioex);
						fail("Unexpected exception");
					}

				}
			}

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
		}


	}

	@Test
	public void testContentBasedMimeTypeDetection() {

		final Map<String, Map<String, byte[]>> testMap = new LinkedHashMap<>();

		try {

			// text-based formats will of course resolved into "text/plain"
			testMap.put("text/plain",               toMap(new Pair("test01", "<!DOCTYPE html><html><head><title>Test</title></head><body><h1>Test</h1></body></html>".getBytes())));
			testMap.put("text/plain",               toMap(new Pair("test02", "Hello world!".getBytes())));
			testMap.put("text/plain",               toMap(new Pair("test03", "body { background-color: #ffffff; }".getBytes())));

			// disabled because jmimemagic detects matlab..
			// testMap.put("text/plain",               toMap(new Pair("test04", "function test() { alert('Test'); return 'Hello world!'; }".getBytes())));

			testMap.put("application/zip",          toMap(new Pair("test05", IOUtils.toByteArray(UiTest.class.getResourceAsStream("/test/test.zip")))));
			testMap.put("image/jpeg",               toMap(new Pair("test06", IOUtils.toByteArray(UiTest.class.getResourceAsStream("/test/test.jpg")))));
			testMap.put("image/png",                toMap(new Pair("test07", IOUtils.toByteArray(UiTest.class.getResourceAsStream("/test/test.png")))));
			testMap.put("image/gif",                toMap(new Pair("test08", IOUtils.toByteArray(UiTest.class.getResourceAsStream("/test/test.gif")))));

			// disabled because jmimemagic v0.1.2 does not properly detect image/tiff cross-OS
			// testMap.put("image/tiff",               toMap(new Pair("test09", IOUtils.toByteArray(FileHelperTest.class.getResourceAsStream("/test/test.tiff")))));

			// disabled because jmimemagic v0.1.2 does not properly detect image/bmp cross-OS
			// testMap.put("image/bmp",                toMap(new Pair("test10", IOUtils.toByteArray(FileHelperTest.class.getResourceAsStream("/test/test.bmp")))));

			// disabled because jmimemagic v0.1.2 does not properly detect image/vnd.microsoft.icon cross-OS
			// testMap.put("image/vnd.microsoft.icon", toMap(new Pair("test11", IOUtils.toByteArray(FileHelperTest.class.getResourceAsStream("/test/test.ico")))));

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
						final FileBase file = FileHelper.createFile(securityContext, content, null, File.class, fileName);
						assertEquals("MIME type detection failed for " + fileName, mimeType, file.getContentType());

					} catch (IOException ioex) {

						logger.warn("", ioex);
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
