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
package org.structr.web.advanced;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import org.apache.commons.io.IOUtils;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.dynamic.File;
import org.structr.web.StructrUiTest;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.FileBase;
import org.structr.web.entity.Folder;
import org.structr.web.entity.Image;

public class FilesystemTest extends StructrUiTest {

	private static final Logger logger = LoggerFactory.getLogger(FilesystemTest.class.getName());

	private static final String Base64ImageData =
		"iVBORw0KGgoAAAANSUhEUgAAAGQAAAA7CAYAAACNOi92AAAGcklEQVR42u2ca0/bVhjH+/X2eu/2FfYNJu3FJm3Spk2rqq1Sq95ooS1sXYGiQEnKnYQ2BCiEkBDIxXZsx87NuZAr/JfnSGZh0MQOYSTlPNJfcY7PObbP7xyf25PcArdLmWEYqNfrPcvv1k0uzEKhgHw+fyltbGwgFotdOp9qtdpbILlcbiBr99HR0SeVzWbPxaHvmUwGqVTqNEyW5bb5FIvFtufNOOeAECmqNXZFN+7xeJBMJrtKTw/Zj0BmZ2eZHj58iDdv3uDevXt49eoVhoaGWPjo6CgeP36MO3fusOPh4WE4nU52/tGjR3j58iUmJydx//59aJpmHwgVbLtEVDMo49baQS2DwkulUtc1hK57HUb33u6+qIISNFMUn0THdI6O3W430un06aunNb4Zjz47lUFXQKiWLC4uMvovXryAw+FgtYdqDYXNzMywz/HxcUxMTODp06d49uwZS9ePQGq1WsdXSSdNrT3AQWzv0vmcnJzYB2K+U+mTagUdU2uh77qun75bzZpEcSjMSssbVHMmfoZRU65mlFWpVLqiS83yr4U/zrzK7KhcLnMgvR72Tgnf3sihMwfCgXAgHAgHwoF8VkDYePzgBzY57OVCW78aPSM9K2k69mN/AaGbo4WxXDXBvtNNXpXRsNwsCJK5IGfFaC7Umva/srMW1/qMzsRPKNZT/QWEZrzboQ/NAqpcCITmGe0Kw2rBmHnT9RonNWhGvFk7ZUsqFA3IqgQ9lUQkdoi4EIUkC5ASAuJiFLlC2nJe6bzSvH4VESkAh/gdSvV0/wGpn1SarURCykigUEuekW5IKFdK7OElWUSiKVESoCZlaHoSSV2FoiaahaXBKGTOpc+VVbbWQzP6o3oO2/seFOoa3OIwltS7lqQbYrPSeLG2sYjltXdYeT8Ht3ceS24XYkoQQsZvOa/d5Lvm9XVkchrmldv9B4RWbNnrpFzAtjzbvOnfz2hFeAJR24dr0YHl9y54fAtYWHnLjilsfXsVq945+PxuSJm9c+nnY3eRrYoMLL0aj4+Pbb8eO8W1k5f5vCSn+Gt/ASGjAjJlLpJ1+7AXxaVBA1lAXsJhav10lZZappWOl+LSWtunREs+dpZvKE9z68Ap/NJ/QDpZo9Gw3Ie0A3KYX8Fu1mEJcGscWmtrZ1SJzGtYGVG2VrrJ8PeoHZcHC0ivgPo1J7zKn9jT5xmYdoqlt1CsZeAPbiJtJKHocWz51+Hd9MC3tYZgeAfBg21sbntZv7SlTHXMk/RRcaDcMCDIB8g3+7dIytfTZx2oPfV9Yx53A1/gY3oc0YKnrRLZEKrHRVQaBWQNHbliCnuhHUSFcBOGH6GDAA6iQRZWbRQR0lc65kna11ZZvqmcykZavR7mDxSQUG4O77Uhyx0v9TEkGp2ZxxeJOmerfQi9smj+Y6Y1d/puJJBgzoWt9N+WBxrUsVuV3ZGlqYsGMTcGSCA7g43U6Ge9LDNQQMYiX+PJ/lccSL/YnrSGj7E5DqSXw1da5DOdIOyKPAQjkUjX6UntJpM3Dog5Kmnn8ODz+U49VUzvFvIFo2MKTyQSEEURkiSxczTLpmM6R7NuK04VHIgNIOT9R35eIyMjcLlc7Pvz588xNjaGqakp5udFvmGvX79m4eRBSOHkQUh+YDTc5UBsDBepVpu1365UVYWiKF2np1ZEcw4OpEfmj6/Ad/iWd+r9YpG8uzkXmeZAOBAOhAPhQDgQDmSQgdCuX7agMY+P6/o9CQfSYrQRRJtNako4sylkd3vYVL/ORwYKyAkaiKmBM0C63bG7Soe+GwOEjFqJlAlgTv4N8/JteMQRhIUd5kq0G/ZhJ+TFh61lvFtywL0+j4XVt5h2TmBueRprviX4dlaxF9lEUFti6UkR3YdiqXDqTcKBWDBaB/vXrafEXG9I6bzKCpNEzndSQkQqrTHPxHzRYM55qqpAViTmiCdKcWRzaeapSOnJDZT1S1UB9Ubt2lvOwP9xAC0WdtOHtLoGsT6lVkQ4usuB9E//dIx8TeVA+gVIvphlPle99iLhQLo08rWinxbwURYHwoFwIANgO5lJPAh92VPHaQ7kEpaqRDER/Ya3kP/DaC+ffHfbKR6PM+cJ868+2okDuaRRIZq/ePqUzD8n6xTvqpdWOJCmyLcrHA5DEATm88WBXDMQmhi2/tUUB3LFRnsmvA8ZQDN/3ctHWdw4EA6EmyX7B3U7uu30qAOXAAAAAElFTkSuQmCC";

	@Test
	public void test01ImageUploadBase64() {

		try (final Tx tx = app.tx()) {

			app.create(Image.class,
				new NodeAttribute<>(Image.name, "test01.png"),
				new NodeAttribute<>(Image.imageData, Base64ImageData)
			);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final List<Image> images = app.nodeQuery(Image.class).getAsList();

			assertEquals("There should be exactly one image", 1, images.size());

			final Image image = images.get(0);

			assertEquals("File size of the image does not match",    Long.valueOf(1707),   image.getProperty(Image.size));
			assertEquals("Width of the image does not match",        Integer.valueOf(100), image.getProperty(Image.width));
			assertEquals("Height of the image does not match",       Integer.valueOf(59),  image.getProperty(Image.height));
			assertEquals("Content type of the image does not match", "image/png",          image.getProperty(Image.contentType));

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}
	}


	@Test
	public void test02ImageUploadBase64WithContentType() {

		try (final Tx tx = app.tx()) {

			app.create(Image.class,
				new NodeAttribute<>(Image.name, "test01.png"),
				new NodeAttribute<>(Image.imageData, "data:image/jpeg;base64," + Base64ImageData)
			);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final List<Image> images = app.nodeQuery(Image.class).getAsList();

			assertEquals("There should be exactly one image", 1, images.size());

			final Image image = images.get(0);

			assertEquals("File size of the image does not match",    Long.valueOf(1707),   image.getProperty(Image.size));
			assertEquals("Width of the image does not match",        Integer.valueOf(100), image.getProperty(Image.width));
			assertEquals("Height of the image does not match",       Integer.valueOf(59),  image.getProperty(Image.height));
			assertEquals("Content type of the image does not match", "image/jpeg",         image.getProperty(Image.contentType));

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testExternalChangesOfMountedDirectory() {

		Path root          = null;
		java.io.File file1 = null;
		java.io.File file2 = null;
		java.io.File file3 = null;

		try {

			logger.info("Creating directory to mount..");

			// create some files and folders on disk
			root = Files.createTempDirectory("structr-mount-test");

			root.resolve("parent1/child1/grandchild1").toFile().mkdirs();
			root.resolve("parent2/child1/grandchild1").toFile().mkdirs();
			root.resolve("parent3/child1/grandchild1").toFile().mkdirs();

			logger.info("Creating files to mount..");

			file1 = root.resolve("parent1/child1/grandchild1/test1.txt").toFile();
			file2 = root.resolve("parent2/child1/grandchild1/test2.txt").toFile();
			file3 = root.resolve("parent3/child1/grandchild1/test3.txt").toFile();

			writeFile(file1, "test1 - before change");
			writeFile(file2, "test2 - before change");
			writeFile(file3, "test3 - before change");

			// mount folder
			try (final Tx tx = app.tx()) {

				logger.info("Mounting directory..");

				app.create(Folder.class,
					new NodeAttribute<>(Folder.name, "mounted"),
					new NodeAttribute<>(Folder.mountTarget, root.toString())
				);

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}

			// check that all files and folders exist
			try (final Tx tx = app.tx()) {

				logger.info("Checking directory..");

				final FileBase check1 = app.nodeQuery(File.class).andName("test1.txt").getFirst();
				final FileBase check2 = app.nodeQuery(File.class).andName("test2.txt").getFirst();
				final FileBase check3 = app.nodeQuery(File.class).andName("test3.txt").getFirst();

				assertEquals("Invalid mount result", "/mounted/parent1/child1/grandchild1/test1.txt", check1.getProperty(File.path));
				assertEquals("Invalid mount result", "/mounted/parent2/child1/grandchild1/test2.txt", check2.getProperty(File.path));
				assertEquals("Invalid mount result", "/mounted/parent3/child1/grandchild1/test3.txt", check3.getProperty(File.path));

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}

			// test external changes to files
			writeFile(file2, "test2 - AFTER change");

			// wait some time
			try { Thread.sleep(1000); } catch (InterruptedException ignore) {}

			// check that external changes are present in the file
			try (final Tx tx = app.tx()) {

				logger.info("Checking directory..");

				final FileBase check2 = app.nodeQuery(File.class).andName("test2.txt").getFirst();

				assertEquals("Invalid checksum of externally modified file", FileHelper.getChecksum(file2), check2.getChecksum());
				assertEquals("Invalid content of externally modified file", "test2 - AFTER change", readFile(check2.getFileOnDisk(false)));

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}

			// unmount folder
			try (final Tx tx = app.tx()) {

				logger.info("Unmounting directory..");

				final Folder mounted = app.nodeQuery(Folder.class).and(Folder.name, "mounted").getFirst();

				mounted.setProperty(Folder.mountTarget, null);

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}

			// check that files and folders have been deleted
			try (final Tx tx = app.tx()) {

				logger.info("Verifying that files have been deleted..");

				assertEquals("No files should remain after unmounting", 0, app.nodeQuery(File.class).getAsList().size());
				assertEquals("Only one directory should remain after unmounting", 1, app.nodeQuery(Folder.class).getAsList().size());

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}

		} catch (IOException ioex) {

			fail("Unexpected exception.");

		} finally {

			try {

				// cleanup
				Files.walkFileTree(root, new FileVisitor<Path>() {

					@Override
					public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						Files.delete(file);
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
						Files.delete(dir);
						return FileVisitResult.CONTINUE;
					}
				});
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

	}

	// ----- private methods -----
	private void writeFile(final java.io.File file, final String content) throws IOException {

		try (final FileOutputStream os = new FileOutputStream(file)) {

			os.write(content.getBytes("utf-8"));
			os.flush();
		}
	}

	private String readFile(final java.io.File file) throws IOException {

		try (final FileInputStream fis = new FileInputStream(file)) {

			return IOUtils.toString(fis, "utf-8");
		}
	}
}









