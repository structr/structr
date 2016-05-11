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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.asciidoctor.internal.IOUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.dynamic.File;
import org.structr.web.entity.FileBase;
import org.structr.web.entity.Folder;
import org.structr.web.entity.Image;

/**
 *
 *
 */
public class FilesTest extends StructrTest {

	public void testCreateFolder() {

		Folder folder1 = null;

		try (final Tx tx = app.tx()) {

			folder1 = FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), "/folder1");
			tx.success();

		} catch (FrameworkException ex) {
			Logger.getLogger(FilesTest.class.getName()).log(Level.SEVERE, null, ex);
		}

		try (final Tx tx = app.tx()) {

			File file1 = (File) app.create(File.class, "file1");
			assertNotNull(file1);
			assertEquals(FileHelper.getFolderPath(file1), "/file1");

			file1.setProperty(File.parent, folder1);
			assertEquals(FileHelper.getFolderPath(file1), "/folder1/file1");

			tx.success();

		} catch (FrameworkException ex) {
			Logger.getLogger(FilesTest.class.getName()).log(Level.SEVERE, null, ex);
		}

		try (final Tx tx = app.tx()) {

			Image image1 = (Image) app.create(Image.class, "image1");
			assertNotNull(image1);
			assertEquals(FileHelper.getFolderPath(image1), "/image1");

			image1.setProperty(File.parent, folder1);
			assertEquals(FileHelper.getFolderPath(image1), "/folder1/image1");

			tx.success();

		} catch (FrameworkException ex) {
			Logger.getLogger(FilesTest.class.getName()).log(Level.SEVERE, null, ex);
		}

		try (final Tx tx = app.tx()) {

			assertEquals(2, folder1.getProperty(Folder.files).size());
			assertEquals(1, folder1.getProperty(Folder.images).size());

		} catch (FrameworkException ex) {
			Logger.getLogger(FilesTest.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void testCreateBase64File() {

		final String base64Data = "data:text/plain;base64,RGllcyBpc3QgZWluIFRlc3Q=";
		final String plaintext  = "Dies ist ein Test";
		File file               = null;

		try (final Tx tx = app.tx()) {

			file = app.create(File.class,
				new NodeAttribute<>(AbstractNode.name, "test.txt"),
				new NodeAttribute<>(FileBase.base64Data, base64Data)
			);

			tx.success();

		} catch (FrameworkException ex) {
			Logger.getLogger(FilesTest.class.getName()).log(Level.SEVERE, null, ex);
		}

		try (final Tx tx = app.tx()) {

			assertEquals("Invalid base64 encoded file content creation result", plaintext, IOUtils.readFull(file.getInputStream()));

			tx.success();

		} catch (FrameworkException ex) {
			Logger.getLogger(FilesTest.class.getName()).log(Level.SEVERE, null, ex);
		}

	}

}
