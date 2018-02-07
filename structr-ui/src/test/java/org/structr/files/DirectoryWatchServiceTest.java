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
package org.structr.files;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import org.apache.cxf.helpers.IOUtils;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.web.StructrUiTest;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;


/**
 */
public class DirectoryWatchServiceTest extends StructrUiTest {

	private static final Logger logger = LoggerFactory.getLogger(DirectoryWatchServiceTest.class.getName());

	@Test
	public void testMountedFolderInRoot() {

		final String dirName = "mountServiceTest1";
		final Path base      = Paths.get(basePath);
		Path testDir         = null;

		try {

			testDir = Files.createDirectory(base.resolve(dirName));

			createTestFile(testDir.resolve(Paths.get("test1.txt")), "test file content 1");
			createTestFile(testDir.resolve(Paths.get("test2.txt")), "test file content 2");
			createTestFile(testDir.resolve(Paths.get("test3.txt")), "test file content 3");

		} catch (IOException ioex) {
			fail("Unable to create test files.");
		}

		// mount directory
		try (final Tx tx = app.tx()) {

			app.create(Folder.class,
				new NodeAttribute<>(Folder.name, "mounted1"),
				new NodeAttribute<>(StructrApp.key(Folder.class, "mountWatchContents"), true),
				new NodeAttribute<>(StructrApp.key(Folder.class, "mountTarget"), testDir.toString())
			);

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}

		// wait for DirectoryWatchService to start and scan
		try { Thread.sleep(5000); } catch (InterruptedException ex) {}

		// verify mount point
		try (final Tx tx = app.tx()) {

			assertNotNull("Folder should have been created by import", app.nodeQuery(Folder.class).and(StructrApp.key(File.class, "path"), "/mounted1").getFirst());

			final File file1 = app.nodeQuery(File.class).and(StructrApp.key(File.class, "path"), "/mounted1/test1.txt").getFirst();
			final File file2 = app.nodeQuery(File.class).and(StructrApp.key(File.class, "path"), "/mounted1/test2.txt").getFirst();
			final File file3 = app.nodeQuery(File.class).and(StructrApp.key(File.class, "path"), "/mounted1/test3.txt").getFirst();

			assertNotNull("Test file should have been created by import", file1);
			assertNotNull("Test file should have been created by import", file2);
			assertNotNull("Test file should have been created by import", file3);

			assertEquals("Imported test file content does not match source", "test file content 1", getContent(file1));
			assertEquals("Imported test file content does not match source", "test file content 2", getContent(file2));
			assertEquals("Imported test file content does not match source", "test file content 3", getContent(file3));

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testNestedMountedFolder() {

		final String dirName = "mountServiceTest2";
		final Path base      = Paths.get(basePath);
		Path testDir         = null;

		try {

			testDir = Files.createDirectory(base.resolve(dirName));

			createTestFile(testDir.resolve(Paths.get("test1.txt")), "test file content 1");
			createTestFile(testDir.resolve(Paths.get("test2.txt")), "test file content 2");
			createTestFile(testDir.resolve(Paths.get("test3.txt")), "test file content 3");

		} catch (IOException ioex) {
			fail("Unable to create test files.");
		}

		// mount directory
		try (final Tx tx = app.tx()) {

			final Folder parent1 = app.create(Folder.class, "parent");

			final Folder parent2 = app.create(Folder.class,
				new NodeAttribute<>(Folder.name, "parent"),
				new NodeAttribute<>(StructrApp.key(Folder.class, "parent"), parent1)
			);

			app.create(Folder.class,
				new NodeAttribute<>(Folder.name, "mounted2"),
				new NodeAttribute<>(StructrApp.key(Folder.class, "parent"), parent2),
				new NodeAttribute<>(StructrApp.key(Folder.class, "mountWatchContents"), true),
				new NodeAttribute<>(StructrApp.key(Folder.class, "mountTarget"), testDir.toString())
			);

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}

		// wait for DirectoryWatchService to start and scan
		try { Thread.sleep(5000); } catch (InterruptedException ex) {}

		// verify mount point
		try (final Tx tx = app.tx()) {

			assertNotNull("Folder should have been created by import", app.nodeQuery(Folder.class).and(StructrApp.key(File.class, "path"), "/parent/parent/mounted2").getFirst());

			final File file1 = app.nodeQuery(File.class).and(StructrApp.key(File.class, "path"), "/parent/parent/mounted2/test1.txt").getFirst();
			final File file2 = app.nodeQuery(File.class).and(StructrApp.key(File.class, "path"), "/parent/parent/mounted2/test2.txt").getFirst();
			final File file3 = app.nodeQuery(File.class).and(StructrApp.key(File.class, "path"), "/parent/parent/mounted2/test3.txt").getFirst();

			assertNotNull("Test file should have been created by import", file1);
			assertNotNull("Test file should have been created by import", file2);
			assertNotNull("Test file should have been created by import", file3);

			assertEquals("Imported test file content does not match source", "test file content 1", getContent(file1));
			assertEquals("Imported test file content does not match source", "test file content 2", getContent(file2));
			assertEquals("Imported test file content does not match source", "test file content 3", getContent(file3));

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
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
					new NodeAttribute<>(Folder.name, "mounted3"),
				new NodeAttribute<>(StructrApp.key(Folder.class, "mountWatchContents"), true),
					new NodeAttribute<>(StructrApp.key(Folder.class, "mountTarget"), root.toString())
				);

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}


			// wait some time
			try { Thread.sleep(5000); } catch (Throwable t) {}

			// check that all files and folders exist
			try (final Tx tx = app.tx()) {

				logger.info("Checking directory..");

				final File check1 = app.nodeQuery(File.class).andName("test1.txt").getFirst();
				final File check2 = app.nodeQuery(File.class).andName("test2.txt").getFirst();
				final File check3 = app.nodeQuery(File.class).andName("test3.txt").getFirst();

				assertEquals("Invalid mount result", "/mounted3/parent1/child1/grandchild1/test1.txt", check1.getPath());
				assertEquals("Invalid mount result", "/mounted3/parent2/child1/grandchild1/test2.txt", check2.getPath());
				assertEquals("Invalid mount result", "/mounted3/parent3/child1/grandchild1/test3.txt", check3.getPath());

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

				final File check2 = app.nodeQuery(File.class).andName("test2.txt").getFirst();

				assertEquals("Invalid checksum of externally modified file", FileHelper.getChecksum(file2), check2.getChecksum());
				assertEquals("Invalid content of externally modified file", "test2 - AFTER change", readFile(check2.getFileOnDisk(false)));

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}

			// unmount folder
			try (final Tx tx = app.tx()) {

				logger.info("Unmounting directory..");

				final Folder mounted = app.nodeQuery(Folder.class).and(Folder.name, "mounted3").getFirst();

				mounted.setProperty(StructrApp.key(Folder.class, "mountTarget"), null);

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
						try {
							Files.delete(file);
						} catch (Throwable t) {
							t.printStackTrace();
						}
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
						try {
							Files.delete(dir);
						} catch (Throwable t) {
							t.printStackTrace();
						}
						return FileVisitResult.CONTINUE;
					}
				});

			} catch (Throwable ex) {
				ex.printStackTrace();
			}
		}

	}

	@Test
	public void testDisableWatchKeyRegistration() {

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
					new NodeAttribute<>(Folder.name, "mounted3"),
					new NodeAttribute<>(StructrApp.key(Folder.class, "mountTarget"), root.toString()),
					new NodeAttribute<>(StructrApp.key(Folder.class, "mountWatchContents"), false)
				);

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}


			// wait some time
			try { Thread.sleep(5000); } catch (Throwable t) {}

			// check that all files and folders exist
			try (final Tx tx = app.tx()) {

				logger.info("Checking directory..");

				final File check1 = app.nodeQuery(File.class).andName("test1.txt").getFirst();
				final File check2 = app.nodeQuery(File.class).andName("test2.txt").getFirst();
				final File check3 = app.nodeQuery(File.class).andName("test3.txt").getFirst();

				assertEquals("Invalid mount result", "/mounted3/parent1/child1/grandchild1/test1.txt", check1.getPath());
				assertEquals("Invalid mount result", "/mounted3/parent2/child1/grandchild1/test2.txt", check2.getPath());
				assertEquals("Invalid mount result", "/mounted3/parent3/child1/grandchild1/test3.txt", check3.getPath());

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}

			// test external changes to files
			writeFile(file2, "test2 - AFTER change");

			// wait some time
			try { Thread.sleep(1000); } catch (InterruptedException ignore) {}

			// check that external changes are NOT recorded
			try (final Tx tx = app.tx()) {

				logger.info("Checking directory..");

				final File check2 = app.nodeQuery(File.class).andName("test2.txt").getFirst();

				assertFalse("Invalid checksum of externally modified file", FileHelper.getMD5Checksum(file2).equals(check2.getMd5()));
				assertEquals("Invalid content of externally modified file", "test2 - AFTER change", readFile(check2.getFileOnDisk(false)));

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}

			// unmount folder
			try (final Tx tx = app.tx()) {

				logger.info("Unmounting directory..");

				final Folder mounted = app.nodeQuery(Folder.class).and(Folder.name, "mounted3").getFirst();

				mounted.setProperty(StructrApp.key(Folder.class, "mountTarget"), null);

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
						try {
							Files.delete(file);
						} catch (Throwable t) {
							t.printStackTrace();
						}
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
						try {
							Files.delete(dir);
						} catch (Throwable t) {
							t.printStackTrace();
						}
						return FileVisitResult.CONTINUE;
					}
				});

			} catch (Throwable ex) {
				ex.printStackTrace();
			}
		}

	}


	// ----- private methods -----
	private void createTestFile(final Path path, final String content) throws IOException {

		try (final FileWriter writer = new FileWriter(path.toFile())) {

			writer.append(content);
			writer.flush();
			writer.close();
		}
	}

	private String getContent(final File file) {

		try (final InputStream is = file.getInputStream()) {

			return IOUtils.toString(is, "utf-8");

		} catch (IOException ioex) {
			ioex.printStackTrace();
			fail("Unexpected exception.");
		}

		return null;
	}

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
