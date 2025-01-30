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
package org.structr.test.web.advanced;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.Traits;
import org.structr.storage.StorageProviderFactory;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.Image;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

public class ImageUploadTest extends StructrUiTest {

	private static final Logger logger = LoggerFactory.getLogger(ImageUploadTest.class.getName());

	private static final String Base64ImageData =
		"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";

	@Test
	public void test01ImageUploadBase64() {

		try (final Tx tx = app.tx()) {

			app.create("Image",
				new NodeAttribute<>(Traits.of("Image").key("name"),      "test01.png"),
				new NodeAttribute<>(Traits.of("Image").key("imageData"), Base64ImageData)
			);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final List<NodeInterface> images = app.nodeQuery("Image").getAsList();

			assertEquals("There should be exactly one image", 1, images.size());

			final Image image = images.get(0).as(Image.class);

			assertEquals("File size of the image does not match", Optional.of(Long.valueOf(1707)), Optional.of(StorageProviderFactory.getStorageProvider(image).size()));
			assertEquals("Width of the image does not match",        Integer.valueOf(100), image.getWidth());
			assertEquals("Height of the image does not match",       Integer.valueOf(59),  image.getHeight());
			assertEquals("Content type of the image does not match", "image/png",          image.getContentType());

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}
	}


	@Test
	public void test02ImageUploadBase64WithContentType() {

		try (final Tx tx = app.tx()) {

			app.create("Image",
				new NodeAttribute<>(Traits.of("Image").key("name"),      "test01.png"),
				new NodeAttribute<>(Traits.of("Image").key("imageData"), "data:image/jpeg;base64," + Base64ImageData)
			);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final List<NodeInterface> images = app.nodeQuery("Image").getAsList();

			assertEquals("There should be exactly one image", 1, images.size());

			final Image image = images.get(0).as(Image.class);

			assertEquals("File size of the image does not match",    java.util.Optional.ofNullable(Long.valueOf(1707)),   java.util.Optional.of(StorageProviderFactory.getStorageProvider(image).size()));
			assertEquals("Width of the image does not match",        Integer.valueOf(100), image.getWidth());
			assertEquals("Height of the image does not match",       Integer.valueOf(59),  image.getHeight());
			assertEquals("Content type of the image does not match", "image/jpeg",   image.getContentType());

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}
	}
}
