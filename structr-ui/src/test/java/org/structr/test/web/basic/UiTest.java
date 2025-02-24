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
package org.structr.test.web.basic;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.api.util.Iterables;
import org.structr.common.AccessControllable;
import org.structr.common.AccessMode;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.schema.export.StructrSchema;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.FileHelper;
import org.structr.web.common.ImageHelper;
import org.structr.web.common.ImageHelper.Thumbnail;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.entity.Image;
import org.structr.web.entity.User;
import org.structr.web.property.ThumbnailProperty;
import org.testng.annotations.Test;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static org.testng.Assert.assertNotEquals;
import static org.testng.AssertJUnit.*;


public class UiTest extends StructrUiTest {

	private static String base64Image  = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAWAAAABUCAYAAAC8/e1DAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAA2ZpVFh0WE1MOmNvbS5hZG9iZS54bXAAAAAAADw/eHBhY2tldCBiZWdpbj0i77u/IiBpZD0iVzVNME1wQ2VoaUh6cmVTek5UY3prYzlkIj8+IDx4OnhtcG1ldGEgeG1sbnM6eD0iYWRvYmU6bnM6bWV0YS8iIHg6eG1wdGs9IkFkb2JlIFhNUCBDb3JlIDUuMy1jMDExIDY2LjE0NTY2MSwgMjAxMi8wMi8wNi0xNDo1NjoyNyAgICAgICAgIj4gPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4gPHJkZjpEZXNjcmlwdGlvbiByZGY6YWJvdXQ9IiIgeG1sbnM6eG1wTU09Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9tbS8iIHhtbG5zOnN0UmVmPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvc1R5cGUvUmVzb3VyY2VSZWYjIiB4bWxuczp4bXA9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC8iIHhtcE1NOk9yaWdpbmFsRG9jdW1lbnRJRD0ieG1wLmRpZDo2RjYyNjlFMUNFMTNFMjExQTQ2N0ZGMDI2MEZEQ0Q3NSIgeG1wTU06RG9jdW1lbnRJRD0ieG1wLmRpZDo2MDcwOEExQzEzRDMxMUUyQTMyQzlEQjBGNTBBQUUwMSIgeG1wTU06SW5zdGFuY2VJRD0ieG1wLmlpZDo2MDcwOEExQjEzRDMxMUUyQTMyQzlEQjBGNTBBQUUwMSIgeG1wOkNyZWF0b3JUb29sPSJBZG9iZSBQaG90b3Nob3AgQ1M2IChXaW5kb3dzKSI+IDx4bXBNTTpEZXJpdmVkRnJvbSBzdFJlZjppbnN0YW5jZUlEPSJ4bXAuaWlkOkYzODhBQzYwRDIxM0UyMTFBNDY3RkYwMjYwRkRDRDc1IiBzdFJlZjpkb2N1bWVudElEPSJ4bXAuZGlkOjZGNjI2OUUxQ0UxM0UyMTFBNDY3RkYwMjYwRkRDRDc1Ii8+IDwvcmRmOkRlc2NyaXB0aW9uPiA8L3JkZjpSREY+IDwveDp4bXBtZXRhPiA8P3hwYWNrZXQgZW5kPSJyIj8+xNxK9AAAAt9JREFUeNrs3TFOKlEUgGGuIWhGSwmVLoMlWFi8ddiZuA41sTMuw9LlYGWBEqOE4DC8c4mNhSUzhvt9yQ2813GCf46DOmk8Hu9dXl7+q6rqPqU06gGwNev1+mU+n1/c3d099uPfB+Eh/nMYx3QAtmuUmxuPTznAh+IL0Kphbm8O8GC1WhkHQLsGOcCpaRqjAGhXygHu2YAB2ifAAF0GuK5rkwCwAQPYgAEQYIAdDbBLEAA2YAABBqCFALsEASDAAGUF2CUIABswQFkB9tfQAGzAAGUF2DVggI4CvFgsTALABgxQUIDdkBOgfXtGACDAAAIMgAADCDAAAgwgwAAIMIAAAyDAAH/a5leRU0omAWADBrABA2ADBtjNAK/Dm1EAtCOaO8sPOcDL19fXm6Zp3o0FYLuitbNo7nVubxqPx1U8OY1zEucojgvC3egbAYV81516Py9/5jtC5DsD19/Pd3r5jfMR5znOJH/R5xvCTeJM4wwEuNM3JpQg/RKmpoDXnl/nMs5nbm//+Pi4d3V1dVZV1X1KaeS9AbDFAq/XL/P5/OL29vYxb8AH+/v7D03TDI0GYOtGubnx+JQDfBhFHroxJ0Br8sJ7mAM8iO3XOADaNcgBTgIM0Lq0+dGn1WplFAAt2wTYBgzQUYC/vr5MAqCLALsEASDAAGUF2CUIgI4C7EM4gI4CXNe1SQAIMEBBAfYhHIAAA5QVYJcgAGzAAGUF2N8CBugowC5BAAgwQFkBXiwWJgFgAwYoKMA+hANo354RAAgwgAADIMAAAgyAAAMIMAACDCDAAAgwwJ+2+VXklJJJANiAAWzAANiAAXYzwOvwZhQA7YjmzvJDDvByOp3eNE3zbiwA2xWtnUVzr3N70/n5eRVPTuOcxDmK44JwN/pGQCHfdafez8uf+Y4Qqzj19/OdXn7jfMR5jjPJX/T5hnCTONM4AwHu9I0JJUi/hKkp4LXn17mM85nb+1+AAQDuVAgNv/BqVwAAAABJRU5ErkJggg==";
	private static final Logger logger = LoggerFactory.getLogger(UiTest.class.getName());

	@Test
	public void test01CreateThumbnail() {

		final String imageType = createTestImageType();
		NodeInterface img      = null;

		try (final Tx tx = app.tx()) {

			img = ImageHelper.createFileBase64(securityContext, base64Image, imageType);
			img.setProperties(img.getSecurityContext(), new PropertyMap(Traits.of(StructrTraits.IMAGE).key("name"), "test-image.png"));

			assertNotNull(img);
			assertTrue(img.is(StructrTraits.IMAGE));

			Image tn = img.getProperty(Traits.of(imageType).key("thumbnail"));

			tx.success();
		} catch (Exception ex) {

			ex.printStackTrace();
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface immutableImage = img;

			tryWithTimeout(
				() -> {
					// Thumbnail creation happens in the background, in a different thread,
					// so we need to allow this thread to break the transaction isolation..
					immutableImage.getNode().invalidate();

					return immutableImage.getProperty(Traits.of(imageType).key("thumbnail")) != null;
				},
				()->fail("Exceeded timeout while waiting for thumbnail creation."),
				30000, 1000);

			Image tn = img.getProperty(Traits.of(imageType).key("thumbnail"));

			assertNotNull(tn);
			assertEquals(Integer.valueOf(200), tn.getWidth());
			assertEquals(Integer.valueOf(48), tn.getHeight());  // cropToFit = false

			assertEquals("image/" + Thumbnail.Format.jpeg, tn.getContentType());

			tx.success();

		} catch (Exception ex) {
			ex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void test01AutoRenameThumbnail() {

		final String initialImageName = "initial_image_name.png";
		final String renamedImageName = "image_name_after_rename.png";
		NodeInterface testImage = null;

		try (final Tx tx = app.tx()) {

			testImage = ImageHelper.createFileBase64(securityContext, base64Image, StructrTraits.IMAGE);

			testImage.setProperties(testImage.getSecurityContext(), new PropertyMap(Traits.of(StructrTraits.IMAGE).key("name"), initialImageName));

			assertNotNull(testImage);
			assertTrue(testImage.is(StructrTraits.IMAGE));

			// Retrieve tn properties to force their generation
			final Image tnSmall = testImage.getProperty(Traits.of(StructrTraits.IMAGE).key("tnSmall"));
			final Image tnMid = testImage.getProperty(Traits.of(StructrTraits.IMAGE).key("tnMid"));

			tx.success();
		} catch (Exception ex) {

			ex.printStackTrace();
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface immutableImage = testImage;

			tryWithTimeout(
				() -> {
					// Thumbnail creation happens in the background, in a different thread,
					// so we need to allow this thread to break the transaction isolation..
					immutableImage.getNode().invalidate();

					return immutableImage.getProperty(Traits.of(StructrTraits.IMAGE).key("tnSmall")) != null && immutableImage.getProperty(Traits.of(StructrTraits.IMAGE).key("tnMid")) != null;
				},
				()->fail("Exceeded timeout while waiting for thumbnail creation."),
				30000, 1000);
			final Image tnSmall = testImage.getProperty(Traits.of(StructrTraits.IMAGE).key("tnSmall"));
			final Image tnMid = testImage.getProperty(Traits.of(StructrTraits.IMAGE).key("tnMid"));

			assertEquals("Initial small thumbnail name not as expected", ImageHelper.getThumbnailName(initialImageName, tnSmall.getWidth(), tnSmall.getHeight()), tnSmall.getProperty(Traits.of(StructrTraits.IMAGE).key("name")));
			assertEquals("Initial mid thumbnail name not as expected", ImageHelper.getThumbnailName(initialImageName, tnMid.getWidth(), tnMid.getHeight()), tnMid.getProperty(Traits.of(StructrTraits.IMAGE).key("name")));

			tx.success();

		} catch (Exception ex) {

			ex.printStackTrace();
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			testImage.setProperties(testImage.getSecurityContext(), new PropertyMap(Traits.of(StructrTraits.IMAGE).key("name"), renamedImageName));
			tx.success();

		} catch (Exception ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			final Image tnSmall = testImage.getProperty(Traits.of(StructrTraits.IMAGE).key("tnSmall"));
			final Image tnMid   = testImage.getProperty(Traits.of(StructrTraits.IMAGE).key("tnMid"));

			assertEquals("Small Thumbnail name not auto-renamed as expected", ImageHelper.getThumbnailName(renamedImageName, tnSmall.getWidth(), tnSmall.getHeight()), tnSmall.getProperty(Traits.of(StructrTraits.IMAGE).key("name")));
			assertEquals("Mid Thumbnail name not auto-renamed as expected", ImageHelper.getThumbnailName(renamedImageName, tnMid.getWidth(), tnMid.getHeight()), tnMid.getProperty(Traits.of(StructrTraits.IMAGE).key("name")));

			tx.success();

		} catch (Exception ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}

	}

	@Test
	public void test01AutoRenameThumbnailForImageSubclass() {

		final String testImageType = createTestImageType();

		NodeInterface subclassTestImage = null;
		final String initialImageName = "initial_image_name.png";
		final String renamedImageName = "image_name_after_rename.png";

		try (final Tx tx = app.tx()) {

			subclassTestImage = ImageHelper.createFileBase64(securityContext, base64Image, testImageType);

			subclassTestImage.setProperties(subclassTestImage.getSecurityContext(), new PropertyMap(Traits.of(StructrTraits.IMAGE).key("name"), initialImageName));

			assertNotNull(subclassTestImage);
			assertTrue(subclassTestImage.is(StructrTraits.IMAGE));

			final Image tnSmall  = subclassTestImage.getProperty(Traits.of(testImageType).key("tnSmall"));
			final Image tnMid    = subclassTestImage.getProperty(Traits.of(testImageType).key("tnMid"));
			final Image tnCustom = subclassTestImage.getProperty(Traits.of(testImageType).key("thumbnail"));

			tx.success();

		} catch (Exception ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}


		try (final Tx tx = app.tx()) {
			final NodeInterface immutableImage = subclassTestImage;
			tryWithTimeout(
					() -> {
							// allow inner node to be updated
							immutableImage.getNode().invalidate();

							return immutableImage.getProperty(Traits.of(testImageType).key("tnSmall")) != null &&
							immutableImage.getProperty(Traits.of(testImageType).key("tnMid")) != null &&
							immutableImage.getProperty(Traits.of(testImageType).key("thumbnail")) != null;
					},
					()->fail("Exceeded timeout while waiting for thumbnail creation."),
					60000,
					5000
			);
			final Image tnSmall  = subclassTestImage.getProperty(Traits.of(testImageType).key("tnSmall"));
			final Image tnMid    = subclassTestImage.getProperty(Traits.of(testImageType).key("tnMid"));
			final Image tnCustom = subclassTestImage.getProperty(Traits.of(testImageType).key("thumbnail"));

			assertEquals("Initial small thumbnail name not as expected", ImageHelper.getThumbnailName(initialImageName, tnSmall.getWidth(), tnSmall.getHeight()), tnSmall.getProperty(Traits.of(StructrTraits.IMAGE).key("name")));
			assertEquals("Initial mid thumbnail name not as expected", ImageHelper.getThumbnailName(initialImageName, tnMid.getWidth(), tnMid.getHeight()), tnMid.getProperty(Traits.of(StructrTraits.IMAGE).key("name")));
			assertEquals("Initial custom thumbnail name not as expected", ImageHelper.getThumbnailName(initialImageName, tnCustom.getWidth(), tnCustom.getHeight()), tnCustom.getProperty(Traits.of(StructrTraits.IMAGE).key("name")));

			tx.success();

		} catch (Exception ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			subclassTestImage.setProperties(subclassTestImage.getSecurityContext(), new PropertyMap(Traits.of(StructrTraits.IMAGE).key("name"), renamedImageName));
			tx.success();

		} catch (Exception ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			final Image tnSmall  = subclassTestImage.getProperty(Traits.of(testImageType).key("tnSmall"));
			final Image tnMid    = subclassTestImage.getProperty(Traits.of(testImageType).key("tnMid"));
			final Image tnCustom = subclassTestImage.getProperty(Traits.of(testImageType).key("thumbnail"));

			assertEquals("Small Thumbnail name not auto-renamed as expected for image subclass", ImageHelper.getThumbnailName(renamedImageName, tnSmall.getWidth(), tnSmall.getHeight()), tnSmall.getProperty(Traits.of(StructrTraits.IMAGE).key("name")));
			assertEquals("Mid Thumbnail name not auto-renamed as expected for image subclass", ImageHelper.getThumbnailName(renamedImageName, tnMid.getWidth(), tnMid.getHeight()), tnMid.getProperty(Traits.of(StructrTraits.IMAGE).key("name")));
			assertEquals("Custom Thumbnail name not auto-renamed as expected for image subclass", ImageHelper.getThumbnailName(renamedImageName, tnCustom.getWidth(), tnCustom.getHeight()), tnCustom.getProperty(Traits.of(StructrTraits.IMAGE).key("name")));

			tx.success();

		} catch (Exception ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}

	}

	@Test
	public void testFolderPath() {

		try (final Tx tx = app.tx()) {

			FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), "/a/a");
			FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), "/c/b/a");
			FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), "/b/a");
			FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), "/a/b/c");

			tx.success();

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			NodeInterface a = FileHelper.getFileByAbsolutePath(SecurityContext.getSuperUserInstance(), "/a");
			assertNotNull(a);
			assertEquals(a.as(Folder.class).getPath(), "/a");

			NodeInterface b = FileHelper.getFileByAbsolutePath(SecurityContext.getSuperUserInstance(), "/a/b");
			assertNotNull(b);
			assertEquals(b.as(Folder.class).getPath(), "/a/b");

			NodeInterface c = FileHelper.getFileByAbsolutePath(SecurityContext.getSuperUserInstance(), "/a/b/c");
			assertNotNull(c);
			assertEquals(c.as(Folder.class).getPath(), "/a/b/c");

			tx.success();

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}

	}

	@Test
	public void testAllowedCharacters() {

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.FOLDER, "/a/b");

			tx.success();

			fail("Folder with non-allowed characters were created.");

		} catch (FrameworkException ex) {}

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.FOLDER, "a/b");

			tx.success();

			fail("Folder with non-allowed characters were created.");

		} catch (FrameworkException ex) {}

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.FOLDER, "/");

			tx.success();

			fail("Folder with non-allowed characters were created.");

		} catch (FrameworkException ex) {}

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.FOLDER, "c/");

			tx.success();

			fail("Folder with non-allowed characters were created.");

		} catch (FrameworkException ex) {}

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.FOLDER, "abc\0");

			tx.success();

			fail("Folder with non-allowed characters were created.");

		} catch (FrameworkException ex) {}

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.FOLDER, "\0abc");

			tx.success();

			fail("Folder with non-allowed characters were created.");

		} catch (FrameworkException ex) {}

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.FOLDER, "a\0bc");

			tx.success();

			fail("Folder with non-allowed characters were created.");

		} catch (FrameworkException ex) {}
	}

	@Test
	public void testCreateFolder() {

		Folder folder1 = null;

		try (final Tx tx = app.tx()) {

			folder1 = FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), "/folder1").as(Folder.class);
			tx.success();

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			File file1 = app.create(StructrTraits.FILE, "file1").as(File.class);
			assertNotNull(file1);
			assertEquals(file1.getPath(), "/file1");

			file1.setProperty(Traits.of(StructrTraits.FILE).key("parent"), folder1);
			assertEquals(file1.getPath(), "/folder1/file1");

			tx.success();

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			Image image1 = app.create(StructrTraits.IMAGE, "image1").as(Image.class);
			assertNotNull(image1);
			assertEquals(image1.getPath(), "/image1");

			image1.setProperty(Traits.of(StructrTraits.FILE).key("parent"), folder1);
			assertEquals(image1.getPath(), "/folder1/image1");

			tx.success();

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			assertEquals(2, Iterables.toList(folder1.getFiles()).size());
			assertEquals(1, Iterables.toList(folder1.getImages()).size());

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}
	}

	@Test
	public void testCreateBase64File() {

		final String base64Data = "data:text/plain;base64,RGllcyBpc3QgZWluIFRlc3Q=";
		final String plaintext  = "Dies ist ein Test";
		File file               = null;

		try (final Tx tx = app.tx()) {

			file = app.create(StructrTraits.FILE,
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key("name"),      "test.txt"),
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key("base64Data"), base64Data)
			).as(File.class);

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
	public void testAutoRenameFileWithIdenticalPathInRootFolder() {

		Settings.UniquePaths.setValue(Boolean.TRUE);

		NodeInterface rootFile1 = null;
		NodeInterface rootFile2 = null;

		try (final Tx tx = app.tx()) {

			rootFile1 = app.create(StructrTraits.FILE, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key("name"), "test.txt"));
			assertNotNull(rootFile1);

			tx.success();

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}


		try (final Tx tx = app.tx()) {

			rootFile2 = app.create(StructrTraits.FILE, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key("name"), "test.txt"));
			assertNotNull(rootFile2);

			tx.success();

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			// property access must now be done in a transaction context
			assertNotEquals(rootFile1.getName(), rootFile2.getName());

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
		}
	}

	@Test
	public void testAutoRenameFileWithIdenticalPathInRootFolderWithDifferentInsertionPoints() {

		Settings.UniquePaths.setValue(Boolean.TRUE);

		final String fileName = "test.file.txt";

		NodeInterface rootFile1 = null;
		NodeInterface rootFile2 = null;
		NodeInterface rootFile3 = null;
		NodeInterface rootFile4 = null;

		String file1Name = null;
		String file2Name = null;
		String file3Name = null;
		String file4Name = null;

		try (final Tx tx = app.tx()) {

			rootFile1 = app.create(StructrTraits.FILE, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key("name"), fileName));
			assertNotNull(rootFile1);

			file1Name = rootFile1.getName();

			tx.success();

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			file1Name = rootFile1.getName();
			tx.success();

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}


		Settings.UniquePathsInsertionPosition.setValue("end");
		try (final Tx tx = app.tx()) {

			rootFile2 = app.create(StructrTraits.FILE, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key("name"), fileName));
			assertNotNull(rootFile2);

			file2Name = rootFile2.getName();

			tx.success();

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			file2Name = rootFile2.getName();
			tx.success();

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}


		assertNotEquals(file1Name, file2Name);
		assertEquals("underscore+timestamp should be after filename for insertion position 'end'", file2Name.charAt(fileName.length()), '_');

		Settings.UniquePathsInsertionPosition.setValue("start");
		try (final Tx tx = app.tx()) {

			rootFile3 = app.create(StructrTraits.FILE, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key("name"), fileName));
			assertNotNull(rootFile3);

			file3Name = rootFile3.getName();

			tx.success();

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			file3Name = rootFile3.getName();
			tx.success();

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}


		assertNotEquals(file1Name, file3Name);
		assertNotEquals(file2Name, file3Name);
		assertEquals("timestamp+underscore should be before filename for insertion position 'start'", file3Name.charAt(file3Name.length() - fileName.length() - 1), '_');

		Settings.UniquePathsInsertionPosition.setValue("beforeextension");
		try (final Tx tx = app.tx()) {

			rootFile4 = app.create(StructrTraits.FILE, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key("name"), fileName));
			assertNotNull(rootFile4);

			file4Name = rootFile4.getName();

			tx.success();

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			file4Name = rootFile4.getName();
			tx.success();

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}


		assertNotEquals(file1Name, file4Name);
		assertNotEquals(file2Name, file4Name);
		assertNotEquals(file3Name, file4Name);
		assertEquals("underscore+timestamp should be before extension for insertion position 'beforeextension'", file4Name.charAt(fileName.length() - 4), '_');
	}

	@Test
	public void testAutoRenameFileWithIdenticalPathInSubFolder() {

		Settings.UniquePaths.setValue(Boolean.TRUE);

		Folder folder = null;
		File file1 = null;
		File file2 = null;

		try (final Tx tx = app.tx()) {

			folder = FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), "/my/test/folder").as(Folder.class);

			assertNotNull(folder);
			assertEquals(folder.getPath(), "/my/test/folder");

			tx.success();

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			file1 = app.create(StructrTraits.FILE,
				new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key("name"), "test.txt"),
				new NodeAttribute<>(Traits.of(StructrTraits.ABSTRACT_FILE).key("parent"), folder)
			).as(File.class);

			assertNotNull(file1);
			assertEquals("Testfolder should have exactly one child", 1, Iterables.count(folder.getChildren()));

			tx.success();

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			file2 = app.create(StructrTraits.FILE,
				new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key("name"), "test.txt"),
				new NodeAttribute<>(Traits.of(StructrTraits.ABSTRACT_FILE).key("parent"), folder)
			).as(File.class);

			assertNotNull(file2);
			assertEquals("Testfolder should have exactly two children", 2, Iterables.count(folder.getChildren()));

			tx.success();

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			// property access must now be done in a transaction context
			assertNotEquals(file1.getName(), file2.getName());

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
		}
	}

	@Test
	public void testAutoRenameFileWhenMovingToFolderWhereIdenticalFilenameExists() {

		Settings.UniquePaths.setValue(Boolean.TRUE);

		Folder folder1 = null;
		Folder folder2 = null;
		File file1 = null;
		File file2 = null;

		try (final Tx tx = app.tx()) {

			folder1 = FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), "/my/test/folder").as(Folder.class);
			assertNotNull(folder1);
			assertEquals(folder1.getPath(), "/my/test/folder");

			folder2 = FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), "/another/directory").as(Folder.class);
			assertNotNull(folder2);
			assertEquals(folder2.getPath(), "/another/directory");

			tx.success();

			file1 = app.create(StructrTraits.FILE,
				new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key("name"), "test.txt"),
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key("parent"), folder1)
			).as(File.class);

			assertNotNull(file1);
			assertEquals("Testfolder 1 should have exactly one child", 1, Iterables.count(folder1.getChildren()));

			file2 = app.create(StructrTraits.FILE,
				new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key("name"), "test.txt"),
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key("parent"), folder2)
			).as(File.class);

			assertNotNull(file2);
			assertEquals("Testfolder 2 should have exactly one child", 1, Iterables.count(folder2.getChildren()));

			tx.success();

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			file2.setParent(folder1);

			assertEquals("Testfolder 1 should have exactly two children", 2, Iterables.count(folder1.getChildren()));
			assertEquals("Testfolder 2 should have no children", 0, Iterables.count(folder2.getChildren()));

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
		}

		try (final Tx tx = app.tx()) {

			// property access must now be done in a transaction context
			assertNotEquals(file1.getName(), file2.getName());

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
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
						final File file = FileHelper.createFile(securityContext, content, null, StructrTraits.FILE, fileName, true).as(File.class);
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
						final File file = FileHelper.createFile(securityContext, content, null, StructrTraits.FILE, fileName, true).as(File.class);
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

	@Test
	public void testImageAndThumbnailDelete() {

		User tester = null;
		String uuid = null;
		NodeInterface image = null;

		try (final Tx tx = app.tx()) {

			image = ImageHelper.createFileBase64(securityContext, base64Image, StructrTraits.IMAGE);
			tester = app.create(StructrTraits.USER, "tester").as(User.class);

			image.setProperty(Traits.of(StructrTraits.IMAGE).key("name"), "test.png");

			// allow non-admin user to delete the image
			image.as(AccessControllable.class).grant(Permission.delete, tester);
			image.as(AccessControllable.class).grant(Permission.read, tester);

			image.getProperty(Traits.of(StructrTraits.IMAGE).key("tnSmall"));
			image.getProperty(Traits.of(StructrTraits.IMAGE).key("tnMid"));

			tx.success();
		} catch (IOException | FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface immutableImage = image;
			tryWithTimeout(
					() -> {
						// Thumbnail creation happens in the background, in a different thread,
						// so we need to allow this thread to break the transaction isolation..
						immutableImage.getNode().invalidate();

						immutableImage.getProperty(Traits.of(StructrTraits.IMAGE).key("tnSmall"));
						immutableImage.getProperty(Traits.of(StructrTraits.IMAGE).key("tnMid"));
						return (Iterables.count(immutableImage.as(Image.class).getThumbnails()) == 2);
					},
					()->fail("Exceeded timeout while waiting for thumbnail generation"),
					30000,
					1000
			);
			assertEquals("Image should have two thumbnails", 2, Iterables.count(image.as(Image.class).getThumbnails()));

			uuid = image.getUuid();

			tx.success();

		} catch (Exception fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final SecurityContext ctx = SecurityContext.getInstance(tester, AccessMode.Backend);
		final App testerApp       = StructrApp.getInstance(ctx);

		try (final Tx tx = testerApp.tx()) {

			final NodeInterface deleteMe = testerApp.getNodeById(StructrTraits.IMAGE, uuid);

			assertNotNull("Image should be visible to test user", deleteMe);

			testerApp.delete(deleteMe);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}

		try (final Tx tx = testerApp.tx()) {

			final List<NodeInterface> images = testerApp.nodeQuery(StructrTraits.IMAGE).getAsList();

			if (!images.isEmpty()) {

				final SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.S");

				for (final NodeInterface im : images) {

					System.out.println("Found Image " + im.getName() + " (" + im.getUuid() + "), created " + df.format(im.getCreatedDate()) + " which should have been deleted.");
				}
			}

			assertEquals("No images should be visible to test user", 0, images.size());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}
	}

	// ----- private methods -----
	private String createTestImageType() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.addType("TestImage");

			type.addTrait(StructrTraits.IMAGE);

			type.addCustomProperty("thumbnail", ThumbnailProperty.class.getName()).setFormat("200, 100, false");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
		}

		return "TestImage";
	}
}
