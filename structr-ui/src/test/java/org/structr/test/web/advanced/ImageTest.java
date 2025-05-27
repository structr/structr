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
package org.structr.test.web.advanced;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.AccessControllable;
import org.structr.common.AccessMode;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.FileHelper;
import org.structr.web.common.ImageHelper;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;
import org.structr.web.traits.definitions.ImageTraitDefinition;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

public class ImageTest extends StructrUiTest {

	private static final Logger logger = LoggerFactory.getLogger(ImageTest.class.getName());

	private static final String Base64ImageData =
		"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";


	@Test
	public void testThumbnailGeneration() {

		final PropertyKey passwordKey = Traits.of(StructrTraits.PRINCIPAL).key(PrincipalTraitDefinition.PASSWORD_PROPERTY);
		Principal tester1             = null;
		Principal tester2             = null;
		Principal tester3             = null;

		try (final Tx tx = app.tx()) {

			tester1 = app.create(StructrTraits.USER, new NodeAttribute<>(Traits.of(StructrTraits.PRINCIPAL).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "tester1"), new NodeAttribute<>(passwordKey, "test")).as(Principal.class);
			tester2 = app.create(StructrTraits.USER, new NodeAttribute<>(Traits.of(StructrTraits.PRINCIPAL).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "tester2"), new NodeAttribute<>(passwordKey, "test")).as(Principal.class);
			tester3 = app.create(StructrTraits.USER, new NodeAttribute<>(Traits.of(StructrTraits.PRINCIPAL).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "tester3"), new NodeAttribute<>(passwordKey, "test")).as(Principal.class);

			final AccessControllable folder1 = FileHelper.createFolderPath(securityContext, "/Test1").as(AccessControllable.class);
			folder1.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true);
			folder1.grant(Permission.write, tester1);
			folder1.grant(Permission.write, tester2);
			folder1.grant(Permission.write, tester3);

			final AccessControllable folder2 = FileHelper.createFolderPath(securityContext, "/Test1/Subtest2").as(AccessControllable.class);
			folder2.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true);
			folder2.grant(Permission.write, tester1);
			folder2.grant(Permission.write, tester2);
			folder2.grant(Permission.write, tester3);

			final AccessControllable folder3 = FileHelper.createFolderPath(securityContext, "/Test1/Subtest3").as(AccessControllable.class);
			folder3.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true);
			folder3.grant(Permission.write, tester1);
			folder3.grant(Permission.write, tester2);
			folder3.grant(Permission.write, tester3);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}


		final SecurityContext ctx1 = SecurityContext.getInstance(tester1, AccessMode.Backend);
		final SecurityContext ctx2 = SecurityContext.getInstance(tester2, AccessMode.Backend);
		final SecurityContext ctx3 = SecurityContext.getInstance(tester3, AccessMode.Backend);

		final App app1             = StructrApp.getInstance(ctx1);
		final App app2             = StructrApp.getInstance(ctx2);
		final App app3             = StructrApp.getInstance(ctx3);

		try (final Tx tx = app1.tx()) {

			createImage(ctx1, "tester1 - image01.png", "/");
			createImage(ctx1, "tester1 - image02.png", "/");
			createImage(ctx1, "tester1 - image03.png", "/Test1");
			createImage(ctx1, "tester1 - image04.png", "/Test1/Subtest2");
			createImage(ctx1, "tester1 - image05.png", "/Test1/Subtest3");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		try (final Tx tx = app2.tx()) {

			createImage(ctx2, "tester2 - image01.png", "/");
			createImage(ctx2, "tester2 - image02.png", "/");
			createImage(ctx2, "tester2 - image03.png", "/Test1");
			createImage(ctx2, "tester2 - image04.png", "/Test1/Subtest2");
			createImage(ctx2, "tester2 - image05.png", "/Test1/Subtest3");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		try (final Tx tx = app3.tx()) {

			createImage(ctx3, "tester3 - image01.png", "/");
			createImage(ctx3, "tester3 - image02.png", "/");
			createImage(ctx3, "tester3 - image03.png", "/Test1");
			createImage(ctx3, "tester3 - image04.png", "/Test1/Subtest2");
			createImage(ctx3, "tester3 - image05.png", "/Test1/Subtest3");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// Wait for asynchronous thumbnail generation
		tryWithTimeout(() -> {

			boolean allThumbnailsAvailable = true;
			try (final Tx tx = app.tx()) {

				final List<NodeInterface> images = app.nodeQuery(StructrTraits.IMAGE).key(Traits.of(StructrTraits.IMAGE).key(ImageTraitDefinition.IS_THUMBNAIL_PROPERTY), false).getAsList();
				for (NodeInterface img : images) {

					allThumbnailsAvailable &= img.getProperty(Traits.of(StructrTraits.IMAGE).key(ImageTraitDefinition.TN_MID_PROPERTY)) != null;
				}

				tx.success();

			} catch (FrameworkException ex) {

				ex.printStackTrace();
				fail("Unexpected exception");
			}

			return allThumbnailsAvailable;

		}, () -> fail("Exceeded timeout while waiting for thumbnails to be available."), 30000, 1000);

		try (final Tx tx = app.tx()) {

			System.out.println("############# Folders:");

			final List<NodeInterface> folders = app.nodeQuery(StructrTraits.FOLDER).sort(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.PATH_PROPERTY)).getAsList();

			folders.stream().forEach(f -> {
				System.out.println(f.as(Folder.class).getPath());
			});

			assertEquals("Invalid number of folders after thumbnail creation", 7, folders.size());

			assertEquals("Invalid folder path", "/._structr_thumbnails",                folders.get(0).as(Folder.class).getPath());
			assertEquals("Invalid folder path", "/._structr_thumbnails/Test1",          folders.get(1).as(Folder.class).getPath());
			assertEquals("Invalid folder path", "/._structr_thumbnails/Test1/Subtest2", folders.get(2).as(Folder.class).getPath());
			assertEquals("Invalid folder path", "/._structr_thumbnails/Test1/Subtest3", folders.get(3).as(Folder.class).getPath());
			assertEquals("Invalid folder path", "/Test1",                               folders.get(4).as(Folder.class).getPath());
			assertEquals("Invalid folder path", "/Test1/Subtest2",                      folders.get(5).as(Folder.class).getPath());
			assertEquals("Invalid folder path", "/Test1/Subtest3",                      folders.get(6).as(Folder.class).getPath());

			tx.success();

		} catch (Exception ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	/*
	 * Test is disabled because we cannot prevent users from creating identical
	   folder paths. If that happens in a production system, the administrator
	   should consider enabling the application.filesystem.enabled flag.

	@Test
	public void testFolderPaths() {

		final PropertyKey passwordKey = Traits.of(StructrTraits.PRINCIPAL).key("password");
		Principal tester1             = null;
		Principal tester2             = null;
		Principal tester3             = null;

		try (final Tx tx = app.tx()) {

			tester1 = app.create(StructrTraits.PRINCIPAL, new NodeAttribute<>(Traits.of(StructrTraits.PRINCIPAL).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "tester1"), new NodeAttribute<>(passwordKey, "test"));
			tester2 = app.create(StructrTraits.PRINCIPAL, new NodeAttribute<>(Traits.of(StructrTraits.PRINCIPAL).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "tester2"), new NodeAttribute<>(passwordKey, "test"));
			tester3 = app.create(StructrTraits.PRINCIPAL, new NodeAttribute<>(Traits.of(StructrTraits.PRINCIPAL).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "tester3"), new NodeAttribute<>(passwordKey, "test"));

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}


		final SecurityContext ctx1 = SecurityContext.getInstance(tester1, AccessMode.Backend);
		final SecurityContext ctx2 = SecurityContext.getInstance(tester2, AccessMode.Backend);
		final SecurityContext ctx3 = SecurityContext.getInstance(tester3, AccessMode.Backend);

		final App app1             = StructrApp.getInstance(ctx1);
		final App app2             = StructrApp.getInstance(ctx2);
		final App app3             = StructrApp.getInstance(ctx3);

		try (final Tx tx = app1.tx()) {

			FileHelper.createFolderPath(ctx1, "/Test1/data");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		try (final Tx tx = app2.tx()) {

			FileHelper.createFolderPath(ctx2, "/Test1/data");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		try (final Tx tx = app3.tx()) {

			FileHelper.createFolderPath(ctx3, "/Test1/data");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			System.out.println("############# Folders:");

			app.nodeQuery(StructrTraits.FOLDER).getAsList().stream().forEach(f -> {
				System.out.println(f.getPath());
			});

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}
	}
	*/

	private void createImage(final SecurityContext securityContext, final String name, final String folderPath) throws FrameworkException {

		try( final InputStream is = ImageTest.class.getResourceAsStream("/test/thumbtest.png")) {

			final NodeInterface image = ImageHelper.createImage(securityContext, is, "image/png", StructrTraits.IMAGE, name, false);
			final NodeInterface path  = FileHelper.createFolderPath(securityContext, folderPath);

			// set path
			if (path != null) {

				image.as(File.class).setParent(path.as(Folder.class));
			}

			// request thumbnail creation
			image.getProperty(Traits.of(StructrTraits.IMAGE).key(ImageTraitDefinition.TN_MID_PROPERTY));

		} catch (IOException ioex) {
			ioex.printStackTrace();
		}
	}
}
