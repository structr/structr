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
package org.structr.test.files;

import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.Permission;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.ResourceAccess;
import org.structr.core.traits.definitions.ResourceAccessTraitDefinition;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.schema.export.StructrSchema;
import org.structr.storage.providers.local.LocalFSStorageProvider;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.entity.Image;
import org.structr.web.entity.User;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import org.structr.storage.StorageProviderFactory;
import org.structr.web.entity.StorageConfiguration;

import static org.testng.AssertJUnit.*;


/**
 */
public class DirectoryWatchServiceTest extends StructrUiTest {

	private static final Logger logger = LoggerFactory.getLogger(DirectoryWatchServiceTest.class.getName());

	@Parameters("testDatabaseConnection")
	@BeforeClass(alwaysRun = true)
	@Override
	public void setup(@Optional String testDatabaseConnection) {

		Settings.Services.setValue("NodeService SchemaService HttpService DirectoryWatchService");

		super.setup(testDatabaseConnection);
	}

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

			// create folder to mount
			final StorageConfiguration testMount = StorageProviderFactory.createConfig("testMount", LocalFSStorageProvider.class, Map.of("mountTarget", testDir.toString()));

			app.create("Folder",
				new NodeAttribute<>(Folder.name, "mounted1"),
				new NodeAttribute<>(StructrApp.key(Folder.class, "mountWatchContents"), true),
				new NodeAttribute<>(StructrApp.key(Folder.class, "storageConfiguration"), testMount)
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

			assertNotNull("Folder should have been created by import", app.nodeQuery("Folder").and(StructrApp.key(File.class, "path"), "/mounted1").getFirst());

			final File file1 = app.nodeQuery("File").and(StructrApp.key(File.class, "path"), "/mounted1/test1.txt").getFirst();
			final File file2 = app.nodeQuery("File").and(StructrApp.key(File.class, "path"), "/mounted1/test2.txt").getFirst();
			final File file3 = app.nodeQuery("File").and(StructrApp.key(File.class, "path"), "/mounted1/test3.txt").getFirst();

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

			final Folder parent1 = app.create("Folder", "parent");

			final Folder parent2 = app.create("Folder",
				new NodeAttribute<>(Folder.name, "parent"),
				new NodeAttribute<>(StructrApp.key(Folder.class, "parent"), parent1)
			);

			// create folder to mount
			final StorageConfiguration testMount = StorageProviderFactory.createConfig("testMount", LocalFSStorageProvider.class, Map.of("mountTarget", testDir.toString()));

			app.create("Folder",
				new NodeAttribute<>(Folder.name, "mounted2"),
				new NodeAttribute<>(StructrApp.key(Folder.class, "parent"), parent2),
				new NodeAttribute<>(StructrApp.key(Folder.class, "mountWatchContents"), true),
				new NodeAttribute<>(StructrApp.key(Folder.class, "storageConfiguration"), testMount)
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

			assertNotNull("Folder should have been created by import", app.nodeQuery("Folder").and(StructrApp.key(File.class, "path"), "/parent/parent/mounted2").getFirst());

			final File file1 = app.nodeQuery("File").and(StructrApp.key(File.class, "path"), "/parent/parent/mounted2/test1.txt").getFirst();
			final File file2 = app.nodeQuery("File").and(StructrApp.key(File.class, "path"), "/parent/parent/mounted2/test2.txt").getFirst();
			final File file3 = app.nodeQuery("File").and(StructrApp.key(File.class, "path"), "/parent/parent/mounted2/test3.txt").getFirst();

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

				// create folder to mount
				final StorageConfiguration testMount = StorageProviderFactory.createConfig("testMount", LocalFSStorageProvider.class, Map.of("mountTarget", root.toString()));

				app.create("Folder",
					new NodeAttribute<>(Folder.name, "mounted3"),
					new NodeAttribute<>(StructrApp.key(Folder.class, "mountWatchContents"),   true),
					new NodeAttribute<>(StructrApp.key(Folder.class, "storageConfiguration"), testMount),
					new NodeAttribute<>(StructrApp.key(Folder.class, "mountScanInterval"),    2)
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

				final File check1 = app.nodeQuery("File").andName("test1.txt").getFirst();
				final File check2 = app.nodeQuery("File").andName("test2.txt").getFirst();
				final File check3 = app.nodeQuery("File").andName("test3.txt").getFirst();

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
			try { Thread.sleep(4000); } catch (InterruptedException ignore) {}

			// check that external changes are present in the file
			try (final Tx tx = app.tx()) {

				logger.info("Checking directory..");

				final File check2 = app.nodeQuery("File").andName("test2.txt").getFirst();

				assertEquals("Invalid checksum of externally modified file", FileHelper.getChecksum(file2), check2.getChecksum());
				assertEquals("Invalid content of externally modified file", "test2 - AFTER change", readFile(check2));

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}

			// unmount folder
			try (final Tx tx = app.tx()) {

				logger.info("Unmounting directory..");

				final Folder mounted = app.nodeQuery("Folder").and(Folder.name, "mounted3").getFirst();

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

				final StorageConfiguration testMount = StorageProviderFactory.createConfig("testMount", LocalFSStorageProvider.class, Map.of("mountTarget", root.toString()));

				app.create("Folder",
					new NodeAttribute<>(Folder.name, "mounted3"),
					new NodeAttribute<>(StructrApp.key(Folder.class, "storageConfiguration"), testMount),
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

				final File check1 = app.nodeQuery("File").andName("test1.txt").getFirst();
				final File check2 = app.nodeQuery("File").andName("test2.txt").getFirst();
				final File check3 = app.nodeQuery("File").andName("test3.txt").getFirst();

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

				final File check2 = app.nodeQuery("File").andName("test2.txt").getFirst();

				assertFalse("Invalid checksum of externally modified file", FileHelper.getMD5Checksum(file2).equals(check2.getMd5()));
				assertEquals("Invalid content of externally modified file", "test2 - AFTER change", readFile(check2));

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}

			// unmount folder
			try (final Tx tx = app.tx()) {

				logger.info("Unmounting directory..");

				final Folder mounted = app.nodeQuery("Folder").and(Folder.name, "mounted3").getFirst();

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
	public void testImageDataUploadToMountedFolder() {

		final String base64Data = "iVBORw0KGgoAAAANSUhEUgAAAFYAAAAXCAYAAAHmVYioAAAAAXNSR0IArs4c6QAAAAlwSFlzAAALEwAACxMBAJqcGAAAAAd0SU1FB9sEBxYRJKsLwDYAAAAZdEVYdENvbW1lbnQAQ3JlYXRlZCB3aXRoIEdJTVBXgQ4XAAAD00lEQVRYw+1Y3XXyMAy9zuE1XSALhAHsBVigXsAL4AUyAQOQBeoF0gHIANECZIB6gTCAvhecY0wClNLv0HO4L4Cw/CNL8pWElBIAUFUVbzYbgUvouo632y0zM0spsd1uWUqJruv44+ODpZRYr9fcdR0vrLWCiDgoO+fG2eu6FgBARAIAi7CNOTjn2BgjACAzxjAAKKW4bVsOvwHAGMNlWY7fs7AsEYn9fp/OKuLvZ9sgIlZKXbZKgimdTGvNVVXxlEJd1xy2m8rmoJRiAMiGYYD3/kQhnggADocDjDFsjGHv/TgmtVusc/VGriHPcz4cDqdmwA+htT6TZUTExhherVYc26ZpmvGIwRHjz9gEQSfIF6l7HD0VRVGcyCccVQR7Bp0gz5RSQmvNbdsyHoTMGMN5niPPcwCAtZbzPD9bIL3hcPzgHd+6/XsC47swxrC1FlfXkVLi6+uLY3Rdx1JKSCkRy4Ms1QnpLB2TzhXGpHOm8jD+/f39RL6o65qLorh4qvQ/ImIigtb6Wxa/Zr30v6ZpYK0dnT2z1oqjgImItdaMJ0UWwlQpJZRSwnuP+AF6Jiw2mw2vVqsTYVVV8aMwbj6+kql8s1wuOT5o3/ew1oqpueKrT3XnXOViNlBKsdYaVVX9ejbo+37WEDe9MXVdj3nyN2Gtvd1n/wzmcl+aY2N5rDMMA4ex6/X6LM/O5WZm5kA1p2RSSux2uxP5wlqLtm2Rcr44SOq6Prs25xyGYbjZKE3TcFEUJ3MR0YnL9X2PQLy01rxarcbxRITFMfpnHXsYhknHDzz6VhRFASKaZTBpgIVYiceHPMtFUfCzu2ymlBJEhKZp8Eja9WsvmLVWKKXE8conqdzTbDbi5WKOnz/Fc5sS3BjeeyilRhJ8id6HLLHb7RgAlsvlxblChPd9L2Jd7z3atp1+bpmZ06gMxW+ccuI3e4qQl2XJzrkzjhEvHM8V0pVzThRFwU3TjAew1oopQv6jOjGqxk4Iy19EMM41wnZXfL3wwFzwiEne3t7GnkF4oEOM/0UcW2bjefb7PdLuzVXDHptct4T9bLiXZXlGy5RSs6F2qay9NSQvpaFra6V581olcBx30iZzziHOwen4RVwo3tt1uSXHhs3+j3z807Wu5Vit9WjUubHZVEv1hQeQQ2utCG4e2g+hVf+sjPZPGDZUfaHJZa2F9x6h3CUids5xWZYvI/+EbhGRMMaMhm6aBmVZwjmHS9XEC9/ksW3bvqx0B0TXdZxSo0uvofde3EJr0tfy1irtGpWZqJjOOPM9dCvPc/78/Jxs7KZ7voUS/gORqW62f9bxTgAAAABJRU5ErkJggg==";
		final String dirName    = "mountServiceTest";
		final Path base         = Paths.get(basePath);
		Path testDir            = null;

		try {

			testDir = Files.createDirectory(base.resolve(dirName));

		} catch (IOException ioex) {
			fail("Unable to create test files.");
		}

		// mount directory
		try (final Tx tx = app.tx()) {

			// create test user
			final User tester = app.create("User",
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "tester"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "tester")
			);

			// create folder to mount
			final StorageConfiguration testMount = StorageProviderFactory.createConfig("testMount", LocalFSStorageProvider.class, Map.of("mountTarget", testDir.toString()));

			final Folder folder = app.create("Folder",
				new NodeAttribute<>(Folder.name, "mounted"),
				new NodeAttribute<>(StructrApp.key(Folder.class, "mountWatchContents"), false),
				new NodeAttribute<>(StructrApp.key(Folder.class, "storageConfiguration"), testMount)
			);

			// make folder writable for user
			folder.grant(Permission.read, tester);
			folder.grant(Permission.write, tester);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			// set resource access flags to be able to POS to /Image
			final ResourceAccess grant = app.nodeQuery("ResourceAccessTraitDefinition").and(ResourceAccessTraitDefinition.signature, "Image").getFirst();
			if (grant != null) {

				grant.setProperty(ResourceAccessTraitDefinition.flags, 4L);
				grant.setProperty(ResourceAccessDefinition.visibleToAuthenticatedUsers, true);

			} else {

				app.create("ResourceAccessDefinition",
					new NodeAttribute<>(ResourceAccessDefinition.signature,                   "Image"),
					new NodeAttribute<>(ResourceAccessDefinition.flags,                       4L),
					new NodeAttribute<>(ResourceAccessDefinition.visibleToAuthenticatedUsers, true)
				);
			}

			// add onCreate method that sets the parent of an uploaded image
			final JsonSchema schema  = StructrSchema.createFromDatabase(app);
			final JsonType imageType = schema.getType("Image");
			imageType.addMethod("onCreation", "set(this, 'parent', first(find('Folder', 'name', 'mounted')))");
			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
				.headers("X-User", "tester" , "X-Password", "tester")
				.body("{ name: 'test.png', imageData: '" + base64Data + "' }")

			.expect()

				.statusCode(201)

			.when()
				.post("/Image");

		try { Thread.sleep(5000); } catch (Throwable t) {}

		try (final Tx tx = app.tx()) {

			final List<Image> images = app.nodeQuery("Image").getAsList();

			assertEquals("Only one image should exist", 1, images.size());

			final Image image = images.get(0);

			assertNotNull(image);
			assertEquals("Invalid name of uploaded image", "test.png", image.getName());
			assertEquals("Invalid binary data of uploaded image", base64Data, image.getProperty(StructrApp.key(Image.class, "imageData")));

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
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

	private String readFile(final File file) throws IOException {

		try (final InputStream is = file.getInputStream()) {

			final String content = IOUtils.toString(is, "utf-8");
			return content;
		}
	}
}
