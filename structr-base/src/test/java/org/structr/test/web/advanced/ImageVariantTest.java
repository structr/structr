/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.test.web.advanced;

import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.script.Scripting;
import org.structr.core.traits.StructrTraits;
import org.structr.schema.action.ActionContext;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.ImageHelper;
import org.structr.web.common.ImageHelper.Thumbnail;
import org.structr.web.entity.File;
import org.structr.web.entity.Image;
import org.testng.annotations.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/** Scaling, cropping and re-encoding of images: createImageVariant(), cropped images and thumbnails. */
public class ImageVariantTest extends StructrUiTest {

	private String original = null;

	@Test
	public void testScaleJpegIntoBoundingBox() throws Exception {

		createImage("photo.jpg", "jpeg", 400, 200, false);

		try (final Tx tx = app.tx()) {

			final Image variant = variant("{ maxWidth: 100, maxHeight: 100 }");

			assertEquals("image/jpeg", variant.getContentType());
			assertEquals("photo_100x50.jpg", variant.getName());
			assertDimensions(variant, 100, 50);
			assertEquals("jpeg", formatOf(variant));

			tx.success();
		}
	}

	@Test
	public void testScaleFactorAndNoUpscaling() throws Exception {

		createImage("photo.jpg", "jpeg", 400, 200, false);

		try (final Tx tx = app.tx()) {

			assertDimensions(variant("{ scale: 0.5 }"), 200, 100);
			assertDimensions(variant("{ scale: 2 }"), 400, 200);
			assertDimensions(variant("{ maxWidth: 1000, maxHeight: 1000 }"), 400, 200);
			assertDimensions(variant("{ maxWidth: 200 }"), 200, 100);

			tx.success();
		}
	}

	@Test
	public void testCropCoversTheBox() throws Exception {

		createImage("photo.jpg", "jpeg", 400, 200, false);

		try (final Tx tx = app.tx()) {

			// scaled to 200x100 to cover the box, then the box is cut out of the center
			assertDimensions(variant("{ maxWidth: 100, maxHeight: 100, crop: true }"), 100, 100);

			tx.success();
		}
	}

	@Test
	public void testCropWithOffsetCutsTheRightRegion() throws Exception {

		// left half red, right half blue
		createImage("halves.png", "png", 400, 200, false);

		try (final Tx tx = app.tx()) {

			final BufferedImage left  = read(variant("{ scale: 1, crop: true, maxWidth: 100, maxHeight: 100, offsetX: 0, offsetY: 50 }"));
			final BufferedImage right = read(variant("{ scale: 1, crop: true, maxWidth: 100, maxHeight: 100, offsetX: 300, offsetY: 50 }"));

			assertEquals(100, left.getWidth());
			assertEquals(100, right.getWidth());
			assertEquals(Color.RED.getRGB(), left.getRGB(50, 50));
			assertEquals(Color.BLUE.getRGB(), right.getRGB(50, 50));

			tx.success();
		}
	}

	@Test
	public void testCroppedImageOfJpegIsNotEmpty() throws Exception {

		createImage("halves.jpg", "jpeg", 400, 200, false);

		try (final Tx tx = app.tx()) {

			final Image original = image("halves.jpg");
			final Thumbnail tn   = ImageHelper.createCroppedImage(original, 100, 80, 300, 20, null);

			assertNotNull(tn);
			assertEquals("image/jpeg", tn.getContentType());
			assertEquals(100, tn.getWidth());
			assertEquals(80, tn.getHeight());

			final BufferedImage img = ImageIO.read(new ByteArrayInputStream(tn.getBytes()));

			assertEquals(100, img.getWidth());
			assertEquals(80, img.getHeight());
			assertTrue("cropped region must be blue", isBluish(img.getRGB(50, 40)));

			tx.success();
		}
	}

	@Test
	public void testPngKeepsAlpha() throws Exception {

		createImage("transparent.png", "png", 300, 300, true);

		try (final Tx tx = app.tx()) {

			final Image variant     = variant("{ maxWidth: 100, maxHeight: 100 }");
			final BufferedImage img = read(variant);

			assertEquals("image/png", variant.getContentType());
			assertTrue(img.getColorModel().hasAlpha());
			assertEquals(0, img.getRGB(1, 1) >>> 24);

			tx.success();
		}
	}

	@Test
	public void testPngToJpegFillsTransparencyWithWhite() throws Exception {

		createImage("transparent.png", "png", 300, 300, true);

		try (final Tx tx = app.tx()) {

			final Image variant     = variant("{ format: 'jpeg' }");
			final BufferedImage img = read(variant);

			assertEquals("image/jpeg", variant.getContentType());
			assertEquals("transparent_300x300.jpg", variant.getName());
			assertTrue("transparent pixels must become white", (img.getRGB(1, 1) & 0xffffff) > 0xf0f0f0);

			tx.success();
		}
	}

	@Test
	public void testQualityChangesSize() throws Exception {

		createImage("noise.png", "png", 300, 300, false);

		try (final Tx tx = app.tx()) {

			final long low  = variant("{ format: 'jpeg', quality: 0.1 }").getSize();
			final long high = variant("{ format: 'jpeg', quality: 0.95 }").getSize();

			assertTrue("quality 0.1 (" + low + " bytes) must be smaller than quality 0.95 (" + high + " bytes)", low < high);

			tx.success();
		}
	}

	@Test
	public void testNameAndParentOptions() throws Exception {

		createImage("photo.jpg", "jpeg", 400, 200, false);

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.FOLDER, "downloads");
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final Image variant = variant("{ maxWidth: 100, name: 'small.jpg', parent: $.first($.find('Folder', 'name', 'downloads')) }");

			assertEquals("small.jpg", variant.getName());
			assertEquals("/downloads/small.jpg", variant.getPath());

			tx.success();
		}
	}

	@Test
	public void testStructrScriptKeyValueOptions() throws Exception {

		createImage("photo.jpg", "jpeg", 400, 200, false);

		try (final Tx tx = app.tx()) {

			final Object result = Scripting.evaluate(new ActionContext(securityContext), null, "${createImageVariant(first(find('Image', 'name', 'photo.jpg')), 'scale', 0.25, 'format', 'png')}", "test");

			assertTrue(result instanceof NodeInterface);

			final Image variant = ((NodeInterface) result).as(Image.class);

			assertEquals("image/png", variant.getContentType());
			assertDimensions(variant, 100, 50);

			tx.success();
		}
	}

	@Test
	public void testErrors() throws Exception {

		createImage("photo.jpg", "jpeg", 400, 200, false);

		try (final Tx tx = app.tx()) {

			assertError("${{ $.createImageVariant($.first($.find('Image', 'name', 'photo.jpg')), { format: 'webp' }); }}", "Unsupported image format");
			assertError("${{ $.createImageVariant($.first($.find('Image', 'name', 'photo.jpg')), { format: 'jpeg', quality: 2 }); }}", "Invalid quality");
			assertError("${{ $.createImageVariant($.first($.find('Image', 'name', 'photo.jpg')), { scale: 0 }); }}", "Invalid scale");

			tx.success();
		}
	}

	@Test
	public void testRollbackLeavesNoVariant() throws Exception {

		createImage("photo.jpg", "jpeg", 400, 200, false);

		try (final Tx tx = app.tx()) {

			variant("{ maxWidth: 100 }");

			// no tx.success()
		}

		try (final Tx tx = app.tx()) {

			assertNull(app.nodeQuery(StructrTraits.IMAGE).name("photo_100x50.jpg").getFirst());

			tx.success();
		}
	}

	@Test
	public void testThumbnailFormatSetting() throws Exception {

		createImage("photo.jpg", "jpeg", 400, 200, false);

		try (final Tx tx = app.tx()) {

			final Image original = image("photo.jpg");

			assertEquals("image/png", ImageHelper.createThumbnail(original, 100, 100).getContentType());

			Settings.ThumbnailFormat.setValue("original");

			final Thumbnail tn = ImageHelper.createThumbnail(original, 100, 100);

			assertEquals("image/jpeg", tn.getContentType());
			assertEquals("jpeg", ImageIO.getImageReaders(ImageIO.createImageInputStream(new ByteArrayInputStream(tn.getBytes()))).next().getFormatName().toLowerCase());
			assertEquals(100, tn.getWidth());
			assertEquals(50, tn.getHeight());

			tx.success();

		} finally {

			Settings.ThumbnailFormat.setValue("png");
		}
	}

	// ----- private methods -----
	private Image variant(final String options) throws FrameworkException {

		final Object result = Scripting.evaluate(new ActionContext(securityContext), null, "${{ $.createImageVariant($.first($.find('Image', 'name', '" + original + "')), " + options + "); }}", "test");

		assertTrue("createImageVariant must return a node, got " + result, result instanceof NodeInterface);

		return ((NodeInterface) result).as(Image.class);
	}

	private Image image(final String name) throws FrameworkException {

		return app.nodeQuery(StructrTraits.IMAGE).name(name).getFirst().as(Image.class);
	}

	private void assertError(final String script, final String expected) {

		try {

			Scripting.evaluate(new ActionContext(securityContext), null, script, "test");
			fail("Expected an error containing '" + expected + "'");

		} catch (FrameworkException fex) {

			assertTrue("Expected '" + expected + "' in '" + fex.getMessage() + "'", String.valueOf(fex.getMessage()).contains(expected) || fex.toString().contains(expected));
		}
	}

	private void assertDimensions(final Image image, final int width, final int height) throws IOException {

		final BufferedImage img = read(image);

		assertEquals(width, img.getWidth());
		assertEquals(height, img.getHeight());
	}

	private BufferedImage read(final Image image) throws IOException {

		try (final InputStream is = image.as(File.class).getInputStream()) {

			final BufferedImage img = ImageIO.read(is);

			assertNotNull("variant must be a readable, non-empty image", img);

			return img;
		}
	}

	private String formatOf(final Image image) throws IOException {

		try (final InputStream is = image.as(File.class).getInputStream()) {

			return ImageIO.getImageReaders(ImageIO.createImageInputStream(is)).next().getFormatName().toLowerCase();
		}
	}

	private boolean isBluish(final int rgb) {

		final Color c = new Color(rgb);

		return c.getBlue() > 200 && c.getRed() < 60 && c.getGreen() < 60;
	}

	private void createImage(final String name, final String format, final int width, final int height, final boolean transparent) throws Exception {

		final BufferedImage img = new BufferedImage(width, height, transparent ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
		final Graphics2D g      = img.createGraphics();

		if (transparent) {

			// transparent border, opaque green center
			g.setColor(Color.GREEN);
			g.fillRect(width / 4, height / 4, width / 2, height / 2);

		} else if (name.startsWith("noise")) {

			final Random random = new Random(42);

			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					img.setRGB(x, y, random.nextInt(0xffffff));
				}
			}

		} else {

			g.setColor(Color.RED);
			g.fillRect(0, 0, width / 2, height);
			g.setColor(Color.BLUE);
			g.fillRect(width / 2, 0, width - width / 2, height);
		}

		g.dispose();

		this.original = name;

		final ByteArrayOutputStream out = new ByteArrayOutputStream();

		assertTrue(ImageIO.write(img, format, out));

		try (final Tx tx = app.tx()) {

			ImageHelper.createImage(securityContext, new ByteArrayInputStream(out.toByteArray()), "image/" + format, StructrTraits.IMAGE, name, false);

			tx.success();
		}
	}
}
