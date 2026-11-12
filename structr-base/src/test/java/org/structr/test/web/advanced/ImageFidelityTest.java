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

import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.ImageHelper;
import org.structr.web.common.ImageHelper.ScaleOptions;
import org.structr.web.common.ImageHelper.Thumbnail;
import org.structr.web.entity.Image;
import org.testng.annotations.Test;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.IntBinaryOperator;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Pixel-level checks of scaling, cropping and re-encoding: colours, orientation and colour models.
 *
 * The reference pattern has four coloured quadrants (red, green, blue, yellow) above a grey gradient, so a channel swap, a
 * colour shift, a mirrored or rotated result or a distorted crop changes the colour at a known spot.
 */
public class ImageFidelityTest extends StructrUiTest {

	private static final int W      = 400;
	private static final int H      = 200;
	private static final Color RED    = new Color(255, 0, 0);
	private static final Color GREEN  = new Color(0, 255, 0);
	private static final Color BLUE   = new Color(0, 0, 255);
	private static final Color YELLOW = new Color(255, 255, 0);

	// colour tolerance per channel: lossless formats must be exact up to resampling, JPEG gets more room
	private static final int LOSSLESS = 3;
	private static final int LOSSY    = 16;

	// -DimageTest.dumpDir=<folder> writes every input and result as PNG for a visual check
	private static final String DUMP_DIR = System.getProperty("imageTest.dumpDir");

	@Test
	public void testScalingKeepsColours() throws Exception {

		final Image image = store("pattern.png", encode(pattern(W, H), "png"));

		try (final Tx tx = app.tx()) {

			for (final String format : new String[] { "png", "jpeg", "gif" }) {

				final int tolerance = "jpeg".equals(format) ? LOSSY : LOSSLESS;

				final BufferedImage half = scale(image, options(null, null, 0.5, format));
				assertSize(half, 200, 100);
				assertQuadrants(format + " scale 0.5", half, tolerance);
				assertSimilar(format + " scale 0.5", reference(pattern(W, H), 200, 100), half, "jpeg".equals(format) ? 6.0 : 4.0);

				final BufferedImage box = scale(image, options(100, 100, null, format));
				assertSize(box, 100, 50);
				assertQuadrants(format + " box 100", box, tolerance);
				assertSimilar(format + " box 100", reference(pattern(W, H), 100, 50), box, "jpeg".equals(format) ? 8.0 : 5.0);
			}

			tx.success();
		}
	}

	@Test
	public void testStrongDownscaleKeepsColours() throws Exception {

		final Image image = store("large.png", encode(pattern(4000, 2000), "png"));

		try (final Tx tx = app.tx()) {

			final BufferedImage small = scale(image, options(120, 120, null, "png"));

			assertSize(small, 120, 60);
			assertQuadrants("4000 to 120", small, LOSSLESS);
			assertSimilar("4000 to 120", reference(pattern(4000, 2000), 120, 60), small, 5.0);

			tx.success();
		}
	}

	@Test
	public void testCropKeepsColours() throws Exception {

		final Image image = store("pattern.png", encode(pattern(W, H), "png"));

		try (final Tx tx = app.tx()) {

			// a 100x50 cut-out from the middle of each quadrant of the unscaled original
			assertSolid("crop red",    crop(image, 50, 15),  RED,    LOSSLESS);
			assertSolid("crop green",  crop(image, 250, 15), GREEN,  LOSSLESS);
			assertSolid("crop blue",   crop(image, 50, 95),  BLUE,   LOSSLESS);
			assertSolid("crop yellow", crop(image, 250, 95), YELLOW, LOSSLESS);

			// crop to fit: covers the 100x100 box, the centered cut-out shows the inner edge of all four quadrants
			final BufferedImage fit = scale(image, cropToFit(100, 100));

			assertSize(fit, 100, 100);
			assertColor("crop to fit, top left",     fit.getRGB(5, 5),   RED,    LOSSLESS);
			assertColor("crop to fit, top right",    fit.getRGB(94, 5),  GREEN,  LOSSLESS);
			assertColor("crop to fit, bottom left",  fit.getRGB(5, 70),  BLUE,   LOSSLESS);
			assertColor("crop to fit, bottom right", fit.getRGB(94, 70), YELLOW, LOSSLESS);

			tx.success();
		}
	}

	@Test
	public void testExifOrientationIsApplied() throws Exception {

		final BufferedImage upright = pattern(W, H);

		for (int orientation = 1; orientation <= 8; orientation++) {

			// stored so that applying the orientation gives the upright pattern
			final BufferedImage stored = transform(upright, inverse(orientation));
			final Image image          = store("orientation-" + orientation + ".jpg", withExifOrientation(encodeJpeg(stored, 0.98f), orientation));

			try (final Tx tx = app.tx()) {

				final BufferedImage result = scale(image, options(null, null, null, "png"));

				assertSize(result, W, H);
				assertQuadrants("orientation " + orientation, result, LOSSY);

				tx.success();
			}
		}
	}

	@Test
	public void testGrayscalePng() throws Exception {

		final BufferedImage gray = new BufferedImage(256, 64, BufferedImage.TYPE_BYTE_GRAY);
		final WritableRaster raster = gray.getRaster();

		for (int x = 0; x < 256; x++) {
			for (int y = 0; y < 64; y++) {
				raster.setSample(x, y, 0, x);
			}
		}

		final Image image = store("gray.png", encode(gray, "png"));

		try (final Tx tx = app.tx()) {

			for (final String format : new String[] { "png", "jpeg" }) {

				final BufferedImage result = scale(image, options(null, null, null, format));

				assertSize(result, 256, 64);

				for (final int x : new int[] { 10, 64, 128, 200, 245 }) {
					assertColor("gray " + format + " at x=" + x, result.getRGB(x, 32), new Color(x, x, x), "jpeg".equals(format) ? 6 : LOSSLESS);
				}
			}

			tx.success();
		}
	}

	@Test
	public void testIndexedPngAndGif() throws Exception {

		for (final String format : new String[] { "png", "gif" }) {

			final Image image = store("indexed." + format, encode(indexedPattern(W, H), format));

			try (final Tx tx = app.tx()) {

				final BufferedImage same = scale(image, options(null, null, null, format));
				assertSize(same, W, H);
				assertQuadrants("indexed " + format + " unscaled", same, LOSSLESS);

				final BufferedImage half = scale(image, options(null, null, 0.5, format));
				assertSize(half, 200, 100);
				assertQuadrants("indexed " + format + " scale 0.5", half, LOSSLESS);

				tx.success();
			}
		}
	}

	@Test
	public void testSixteenBitPng() throws Exception {

		final BufferedImage deep = new BufferedImage(256, 64, BufferedImage.TYPE_USHORT_GRAY);
		final WritableRaster raster = deep.getRaster();

		for (int x = 0; x < 256; x++) {
			for (int y = 0; y < 64; y++) {
				raster.setSample(x, y, 0, x * 257);
			}
		}

		final Image image = store("deep.png", encode(deep, "png"));

		try (final Tx tx = app.tx()) {

			final BufferedImage result = scale(image, options(null, null, 0.5, "png"));

			assertSize(result, 128, 32);

			for (final int x : new int[] { 5, 32, 64, 100, 122 }) {
				final int expected = x * 2;
				assertColor("16 bit at x=" + x, result.getRGB(x, 16), new Color(expected, expected, expected), 4);
			}

			tx.success();
		}
	}

	@Test
	public void testIndexedPaletteWithUncommonColours() throws Exception {

		// colours that are not in Java's default palette, so a lost palette shifts them
		final Color[] colors = { new Color(201, 97, 43), new Color(37, 163, 91), new Color(88, 61, 173), new Color(240, 214, 137) };
		final byte[] r = new byte[4], g = new byte[4], b = new byte[4];

		for (int i = 0; i < 4; i++) {
			r[i] = (byte)colors[i].getRed();
			g[i] = (byte)colors[i].getGreen();
			b[i] = (byte)colors[i].getBlue();
		}

		final BufferedImage img = new BufferedImage(200, 100, BufferedImage.TYPE_BYTE_BINARY, new IndexColorModel(2, 4, r, g, b));

		for (int x = 0; x < 200; x++) {
			for (int y = 0; y < 100; y++) {
				img.setRGB(x, y, colors[(x < 100 ? 0 : 1) + (y < 50 ? 0 : 2)].getRGB());
			}
		}

		for (final String format : new String[] { "png", "gif" }) {

			final Image image = store("palette." + format, encode(img, format));

			try (final Tx tx = app.tx()) {

				for (final String target : new String[] { format, "jpeg" }) {

					final int tolerance       = "jpeg".equals(target) ? LOSSY : LOSSLESS;
					final BufferedImage result = scale(image, options(null, null, 0.5, target));

					assertSize(result, 100, 50);
					assertColor(format + " to " + target + ", top left",     result.getRGB(25, 12), colors[0], tolerance);
					assertColor(format + " to " + target + ", top right",    result.getRGB(75, 12), colors[1], tolerance);
					assertColor(format + " to " + target + ", bottom left",  result.getRGB(25, 37), colors[2], tolerance);
					assertColor(format + " to " + target + ", bottom right", result.getRGB(75, 37), colors[3], tolerance);
				}

				tx.success();
			}
		}
	}

	@Test
	public void testGrayscaleWithAlphaPng() throws Exception {

		final ComponentColorModel model = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
		final WritableRaster raster     = model.createCompatibleWritableRaster(200, 100);

		for (int x = 0; x < 200; x++) {
			for (int y = 0; y < 100; y++) {
				raster.setSample(x, y, 0, 128);
				raster.setSample(x, y, 1, x < 100 ? 255 : 0);
			}
		}

		final BufferedImage img = new BufferedImage(model, raster, false, null);
		final Image image       = store("gray-alpha.png", encode(img, "png"));

		try (final Tx tx = app.tx()) {

			final BufferedImage result = scale(image, options(null, null, 0.5, "png"));

			assertSize(result, 100, 50);
			assertTrue("result keeps transparency", result.getColorModel().hasAlpha());
			assertEquals("left half opaque", 255, result.getRGB(25, 25) >>> 24);
			assertEquals("right half transparent", 0, result.getRGB(75, 25) >>> 24);

			final Color c = new Color(result.getRGB(25, 25));

			// grey 128 in a linear grey colour space can come back gamma-corrected, so only require a neutral mid grey
			assertTrue("left half is a neutral grey, was " + c, Math.abs(c.getRed() - c.getGreen()) <= 2 && Math.abs(c.getGreen() - c.getBlue()) <= 2 && c.getRed() > 60 && c.getRed() < 200);

			tx.success();
		}
	}

	@Test
	public void testSixteenBitRgbPng() throws Exception {

		final ComponentColorModel model = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), false, false, Transparency.OPAQUE, DataBuffer.TYPE_USHORT);
		final WritableRaster raster     = model.createCompatibleWritableRaster(W, H);
		final BufferedImage src         = pattern(W, H);

		for (int x = 0; x < W; x++) {
			for (int y = 0; y < H; y++) {

				final Color c = new Color(src.getRGB(x, y));

				raster.setSample(x, y, 0, c.getRed() * 257);
				raster.setSample(x, y, 1, c.getGreen() * 257);
				raster.setSample(x, y, 2, c.getBlue() * 257);
			}
		}

		final Image image = store("deep-rgb.png", encode(new BufferedImage(model, raster, false, null), "png"));

		try (final Tx tx = app.tx()) {

			final BufferedImage result = scale(image, options(null, null, 0.5, "png"));

			assertSize(result, 200, 100);
			assertQuadrants("16 bit RGB", result, LOSSLESS);

			tx.success();
		}
	}

	@Test
	public void testRotateAndFlip() throws Exception {

		final BufferedImage upright = pattern(W, H);
		final Image image           = store("pattern.png", encode(upright, "png"));

		try (final Tx tx = app.tx()) {

			for (final int rotate : new int[] { 0, 90, 180, 270, -90 }) {

				for (final boolean flipX : new boolean[] { false, true }) {

					for (final boolean flipY : new boolean[] { false, true }) {

						final ScaleOptions options = options(null, null, null, "png");

						options.rotate = rotate;
						options.flipX  = flipX;
						options.flipY  = flipY;

						// rotation in EXIF terms: 6 is 90 degrees clockwise, 3 is 180, 8 is 270; 2 mirrors left to right, 4 top to bottom
						final int normalized   = ((rotate % 360) + 360) % 360;
						BufferedImage expected = transform(upright, normalized == 90 ? 6 : normalized == 180 ? 3 : normalized == 270 ? 8 : 1);

						if (flipX) expected = transform(expected, 2);
						if (flipY) expected = transform(expected, 4);

						final String label         = "rotate " + rotate + (flipX ? " flipX" : "") + (flipY ? " flipY" : "");
						final BufferedImage result = scale(image, options);

						assertSize(result, expected.getWidth(), expected.getHeight());
						assertSimilar(label, expected, result, 0.0);
					}
				}
			}

			tx.success();
		}
	}

	@Test
	public void testRegionOfRotatedImage() throws Exception {

		final Image image = store("pattern.png", encode(pattern(W, H), "png"));

		try (final Tx tx = app.tx()) {

			// rotated 90 degrees clockwise, the 200 x 400 image has the gradient on the left, then blue over yellow, then red over green on the right
			final ScaleOptions options = options(null, null, null, "png");

			options.rotate       = 90;
			options.regionX      = 140;
			options.regionY      = 20;
			options.regionWidth  = 50;
			options.regionHeight = 60;

			final BufferedImage red = scale(image, options);

			assertSize(red, 50, 60);
			assertSolid("region of rotated image", red, RED, LOSSLESS);

			// the region is cut out before scaling, so a box then scales the region, not the whole image
			options.regionX      = 45;
			options.regionY      = 250;
			options.regionWidth  = 70;
			options.regionHeight = 70;
			options.maxWidth     = 20;
			options.maxHeight    = 20;

			final BufferedImage yellow = scale(image, options);

			assertSize(yellow, 20, 20);
			assertSolid("scaled region of rotated image", yellow, YELLOW, LOSSLESS);

			tx.success();
		}
	}

	@Test
	public void testCmykJpeg() throws Exception {

		final byte[] cmyk = encodeCmykJpeg(pattern(W, H));

		// sanity check of the generated file, independent of Structr
		final BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(cmyk));
		assertNotNull("CMYK test image must be readable", decoded);
		assertQuadrants("CMYK test image itself", decoded, 40);

		final Image image = store("cmyk.jpg", cmyk);

		try (final Tx tx = app.tx()) {

			final BufferedImage result = scale(image, options(null, null, 0.5, "jpeg"));

			assertSize(result, 200, 100);
			assertQuadrants("CMYK to JPEG", result, 40);

			tx.success();
		}
	}

	// ----- assertions -----
	private void assertQuadrants(final String label, final BufferedImage img, final int tolerance) {

		final int w = img.getWidth();
		final int h = img.getHeight();

		assertColor(label + ", red quadrant",    img.getRGB(w / 4,     h / 5),     RED,    tolerance);
		assertColor(label + ", green quadrant",  img.getRGB(3 * w / 4, h / 5),     GREEN,  tolerance);
		assertColor(label + ", blue quadrant",   img.getRGB(w / 4,     3 * h / 5), BLUE,   tolerance);
		assertColor(label + ", yellow quadrant", img.getRGB(3 * w / 4, 3 * h / 5), YELLOW, tolerance);

		// the gradient runs from black on the left to white on the right
		final Color left  = new Color(img.getRGB(w / 20, 9 * h / 10));
		final Color right = new Color(img.getRGB(19 * w / 20, 9 * h / 10));

		final int dark  = 64 + tolerance;
		final int light = 192 - tolerance;

		assertTrue(label + ": gradient must be dark on the left, was " + left, left.getRed() < dark && left.getGreen() < dark && left.getBlue() < dark);
		assertTrue(label + ": gradient must be light on the right, was " + right, right.getRed() > light && right.getGreen() > light && right.getBlue() > light);
	}

	private void assertSolid(final String label, final BufferedImage img, final Color expected, final int tolerance) {

		for (int x = 0; x < img.getWidth(); x += 7) {
			for (int y = 0; y < img.getHeight(); y += 7) {
				assertColor(label + " at " + x + "," + y, img.getRGB(x, y), expected, tolerance);
			}
		}
	}

	private void assertColor(final String label, final int rgb, final Color expected, final int tolerance) {

		final Color actual = new Color(rgb);
		final int diff     = Math.max(Math.abs(actual.getRed() - expected.getRed()), Math.max(Math.abs(actual.getGreen() - expected.getGreen()), Math.abs(actual.getBlue() - expected.getBlue())));

		assertTrue(label + ": expected " + expected + " (tolerance " + tolerance + "), was " + actual, diff <= tolerance);
	}

	/** Mean absolute difference per channel against a reference, over the whole image. */
	private void assertSimilar(final String label, final BufferedImage expected, final BufferedImage actual, final double maxMean) {

		assertSize(actual, expected.getWidth(), expected.getHeight());

		long sum = 0;

		for (int x = 0; x < expected.getWidth(); x++) {
			for (int y = 0; y < expected.getHeight(); y++) {

				final Color e = new Color(expected.getRGB(x, y));
				final Color a = new Color(actual.getRGB(x, y));

				sum += Math.abs(e.getRed() - a.getRed()) + Math.abs(e.getGreen() - a.getGreen()) + Math.abs(e.getBlue() - a.getBlue());
			}
		}

		final double mean = sum / (3.0 * expected.getWidth() * expected.getHeight());

		assertTrue(label + ": mean channel difference to the reference is " + mean + ", allowed " + maxMean, mean <= maxMean);
	}

	private void assertSize(final BufferedImage img, final int width, final int height) {

		assertEquals("width", width, img.getWidth());
		assertEquals("height", height, img.getHeight());
	}

	// ----- Structr calls -----
	private BufferedImage scale(final Image image, final ScaleOptions options) throws Exception {

		final Thumbnail tn = ImageHelper.scaleImage(image, options, false);

		assertNotNull(tn);
		assertTrue("result must not be empty", tn.getBytes().length > 0);

		final BufferedImage img = ImageIO.read(new ByteArrayInputStream(tn.getBytes()));

		assertNotNull("result must be a readable image", img);

		dump(image.getName() + "__" + describe(options), img);
		assertEquals("decoded width matches the reported width", tn.getWidth(), img.getWidth());
		assertEquals("decoded height matches the reported height", tn.getHeight(), img.getHeight());

		return img;
	}

	private BufferedImage crop(final Image image, final int x, final int y) throws Exception {

		final Thumbnail tn = ImageHelper.createCroppedImage(image, 100, 50, x, y, "png");

		assertNotNull(tn);

		final BufferedImage img = ImageIO.read(new ByteArrayInputStream(tn.getBytes()));

		dump(image.getName() + "__crop-" + x + "-" + y, img);

		return img;
	}

	private ScaleOptions options(final Integer maxWidth, final Integer maxHeight, final Double scale, final String format) {

		final ScaleOptions options = new ScaleOptions();

		options.maxWidth  = maxWidth;
		options.maxHeight = maxHeight;
		options.scale     = scale;
		options.format    = format;
		options.quality   = 0.95f;

		return options;
	}

	private ScaleOptions cropToFit(final int width, final int height) {

		final ScaleOptions options = options(width, height, null, "png");

		options.crop = true;

		return options;
	}

	private Image store(final String name, final byte[] data) throws Exception {

		dump(name + "__input", ImageIO.read(new ByteArrayInputStream(data)));

		try (final Tx tx = app.tx()) {

			final String contentType = name.endsWith(".png") ? "image/png" : name.endsWith(".gif") ? "image/gif" : "image/jpeg";
			final Image image        = ImageHelper.createImage(securityContext, new ByteArrayInputStream(data), contentType, StructrTraits.IMAGE, name, false).as(Image.class);

			tx.success();

			return image;
		}
	}

	private static String describe(final ScaleOptions o) {

		final StringBuilder buf = new StringBuilder(o.format);

		if (o.scale != null)     buf.append("-scale").append(o.scale);
		if (o.maxWidth != null)  buf.append("-w").append(o.maxWidth);
		if (o.maxHeight != null) buf.append("-h").append(o.maxHeight);
		if (o.crop)              buf.append("-crop");

		return buf.toString();
	}

	private static void dump(final String name, final BufferedImage img) throws IOException {

		if (DUMP_DIR == null || img == null) {
			return;
		}

		final java.io.File dir = new java.io.File(DUMP_DIR);

		dir.mkdirs();

		// PNG keeps the decoded pixels exactly; images ImageIO cannot write as PNG directly are drawn into RGB(A) first
		if (!ImageIO.write(img, "png", new java.io.File(dir, name.replace('/', '_') + ".png"))) {

			final BufferedImage rgb = new BufferedImage(img.getWidth(), img.getHeight(), img.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
			rgb.createGraphics().drawImage(img, 0, 0, null);

			ImageIO.write(rgb, "png", new java.io.File(dir, name.replace('/', '_') + ".png"));
		}
	}

	// ----- test images -----
	private static BufferedImage pattern(final int w, final int h) {

		final BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		final int split         = 4 * h / 5;

		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {

				final int rgb;

				if (y < split) {

					final boolean left = x < w / 2;
					final boolean top  = y < split / 2;

					rgb = (top ? (left ? RED : GREEN) : (left ? BLUE : YELLOW)).getRGB();

				} else {

					final int v = (int)Math.round(255.0 * x / (w - 1));
					rgb = new Color(v, v, v).getRGB();
				}

				img.setRGB(x, y, rgb);
			}
		}

		return img;
	}

	/** The pattern with a palette that holds the four colours and 32 grey levels exactly. */
	private static BufferedImage indexedPattern(final int w, final int h) {

		final byte[] r = new byte[36];
		final byte[] g = new byte[36];
		final byte[] b = new byte[36];
		final Color[] colors = { RED, GREEN, BLUE, YELLOW };

		for (int i = 0; i < 4; i++) {
			r[i] = (byte)colors[i].getRed();
			g[i] = (byte)colors[i].getGreen();
			b[i] = (byte)colors[i].getBlue();
		}

		for (int i = 0; i < 32; i++) {
			final int v = Math.round(255f * i / 31);
			r[4 + i] = g[4 + i] = b[4 + i] = (byte)v;
		}

		final BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_INDEXED, new IndexColorModel(8, 36, r, g, b));
		final BufferedImage src = pattern(w, h);

		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
				img.setRGB(x, y, src.getRGB(x, y));
			}
		}

		return img;
	}

	/** Area-averaged reference downscale. */
	private static BufferedImage reference(final BufferedImage src, final int w, final int h) {

		final BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		final double fx         = (double)src.getWidth() / w;
		final double fy         = (double)src.getHeight() / h;

		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {

				long r = 0, g = 0, b = 0, n = 0;

				for (int sx = (int)(x * fx); sx < (int)((x + 1) * fx); sx++) {
					for (int sy = (int)(y * fy); sy < (int)((y + 1) * fy); sy++) {

						final int rgb = src.getRGB(sx, sy);

						r += (rgb >> 16) & 0xff;
						g += (rgb >> 8) & 0xff;
						b += rgb & 0xff;
						n++;
					}
				}

				out.setRGB(x, y, new Color((int)(r / n), (int)(g / n), (int)(b / n)).getRGB());
			}
		}

		return out;
	}

	// ----- EXIF orientation -----
	private static int inverse(final int orientation) {

		return orientation == 6 ? 8 : orientation == 8 ? 6 : orientation;
	}

	/** Applies the display transform of an EXIF orientation. */
	private static BufferedImage transform(final BufferedImage in, final int orientation) {

		final int w        = in.getWidth();
		final int h        = in.getHeight();
		final boolean swap = orientation >= 5;
		final BufferedImage out = new BufferedImage(swap ? h : w, swap ? w : h, BufferedImage.TYPE_INT_RGB);

		final IntBinaryOperator sourceX;
		final IntBinaryOperator sourceY;

		switch (orientation) {
			case 2  -> { sourceX = (x, y) -> w - 1 - x; sourceY = (x, y) -> y; }
			case 3  -> { sourceX = (x, y) -> w - 1 - x; sourceY = (x, y) -> h - 1 - y; }
			case 4  -> { sourceX = (x, y) -> x;         sourceY = (x, y) -> h - 1 - y; }
			case 5  -> { sourceX = (x, y) -> y;         sourceY = (x, y) -> x; }
			case 6  -> { sourceX = (x, y) -> y;         sourceY = (x, y) -> h - 1 - x; }
			case 7  -> { sourceX = (x, y) -> w - 1 - y; sourceY = (x, y) -> h - 1 - x; }
			case 8  -> { sourceX = (x, y) -> w - 1 - y; sourceY = (x, y) -> x; }
			default -> { sourceX = (x, y) -> x;         sourceY = (x, y) -> y; }
		}

		for (int x = 0; x < out.getWidth(); x++) {
			for (int y = 0; y < out.getHeight(); y++) {
				out.setRGB(x, y, in.getRGB(sourceX.applyAsInt(x, y), sourceY.applyAsInt(x, y)));
			}
		}

		return out;
	}

	/** Inserts a minimal EXIF segment with the given orientation right after the JPEG start marker. */
	private static byte[] withExifOrientation(final byte[] jpeg, final int orientation) {

		final byte[] tiff = {
			'I', 'I', 0x2a, 0x00, 0x08, 0x00, 0x00, 0x00,          // little endian header, IFD0 at offset 8
			0x01, 0x00,                                              // one entry
			0x12, 0x01, 0x03, 0x00, 0x01, 0x00, 0x00, 0x00,          // tag 0x0112 (orientation), SHORT, count 1
			(byte)orientation, 0x00, 0x00, 0x00,                     // value
			0x00, 0x00, 0x00, 0x00                                   // no next IFD
		};

		final byte[] header = { 'E', 'x', 'i', 'f', 0x00, 0x00 };
		final int length    = 2 + header.length + tiff.length;
		final ByteArrayOutputStream out = new ByteArrayOutputStream();

		out.write(0xff);
		out.write(0xd8);
		out.write(0xff);
		out.write(0xe1);
		out.write((length >> 8) & 0xff);
		out.write(length & 0xff);
		out.writeBytes(header);
		out.writeBytes(tiff);
		out.write(jpeg, 2, jpeg.length - 2);

		return out.toByteArray();
	}

	// ----- encoding -----
	private static byte[] encode(final BufferedImage img, final String format) throws IOException {

		final ByteArrayOutputStream out = new ByteArrayOutputStream();

		assertTrue("test image must be writable as " + format, ImageIO.write(img, format, out));

		return out.toByteArray();
	}

	private static byte[] encodeJpeg(final BufferedImage img, final float quality) throws IOException {

		final ImageWriter writer        = ImageIO.getImageWritersByFormatName("jpeg").next();
		final ByteArrayOutputStream out = new ByteArrayOutputStream();

		try (final ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {

			final ImageWriteParam param = writer.getDefaultWriteParam();

			param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			param.setCompressionQuality(quality);

			writer.setOutput(ios);
			writer.write(null, new IIOImage(img, null, null), param);

		} finally {

			writer.dispose();
		}

		return out.toByteArray();
	}

	/** Writes the image as a four-channel CMYK JPEG (Adobe convention, inverted values). */
	private static byte[] encodeCmykJpeg(final BufferedImage rgb) throws IOException {

		final int w                  = rgb.getWidth();
		final int h                  = rgb.getHeight();
		final WritableRaster raster  = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, w, h, 4, null);

		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {

				final Color c  = new Color(rgb.getRGB(x, y));
				final double r = c.getRed() / 255.0, g = c.getGreen() / 255.0, b = c.getBlue() / 255.0;
				final double k = 1.0 - Math.max(r, Math.max(g, b));
				final double cy = k < 1.0 ? (1.0 - r - k) / (1.0 - k) : 0;
				final double m  = k < 1.0 ? (1.0 - g - k) / (1.0 - k) : 0;
				final double ye = k < 1.0 ? (1.0 - b - k) / (1.0 - k) : 0;

				// Adobe CMYK JPEGs store inverted values
				raster.setSample(x, y, 0, 255 - (int)Math.round(cy * 255));
				raster.setSample(x, y, 1, 255 - (int)Math.round(m * 255));
				raster.setSample(x, y, 2, 255 - (int)Math.round(ye * 255));
				raster.setSample(x, y, 3, 255 - (int)Math.round(k * 255));
			}
		}

		final ImageWriter writer        = ImageIO.getImageWritersByFormatName("jpeg").next();
		final ByteArrayOutputStream out = new ByteArrayOutputStream();

		try (final ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {

			final ImageWriteParam param = writer.getDefaultWriteParam();

			param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			param.setCompressionQuality(0.98f);

			writer.setOutput(ios);
			writer.write(null, new IIOImage(raster, null, null), param);

		} finally {

			writer.dispose();
		}

		return out.toByteArray();
	}
}
