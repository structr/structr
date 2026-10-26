/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.extraction.parser.figures;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Encoding, decoding and hashing of figure images. Figures are always stored as PNG.
 */
final class FigureImages {

	static final String MEDIA_TYPE = "image/png";

	/** Images are scaled down to at most this many pixels on their long side. */
	private static final int MAX_SIDE = 4096;

	private FigureImages() {
	}

	static byte[] png(final BufferedImage image) throws IOException {

		try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {

			if (!ImageIO.write(image, "png", out)) {

				throw new IOException("No PNG encoder available.");
			}

			return out.toByteArray();
		}
	}

	/**
	 * Decodes an image, scaled down if it is very large, or returns null if the format cannot be read
	 * (e.g. EMF and WMF in office documents).
	 */
	static BufferedImage decode(final byte[] data) {

		try {

			final BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
			if (image == null) {

				return null;
			}

			return limit(toRgb(image));

		} catch (final IOException | RuntimeException ex) {

			return null;
		}
	}

	static String sha256(final byte[] data) {

		try {

			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));

		} catch (final NoSuchAlgorithmException ex) {

			// every Java runtime is required to provide SHA-256
			throw new IllegalStateException(ex);
		}
	}

	// ----- private methods -----
	/**
	 * Transparent images are drawn on white, as they appear in a document.
	 */
	private static BufferedImage toRgb(final BufferedImage image) {

		if (image.getType() == BufferedImage.TYPE_INT_RGB) {

			return image;
		}

		final BufferedImage rgb   = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
		final Graphics2D graphics = rgb.createGraphics();

		try {

			graphics.setColor(java.awt.Color.WHITE);
			graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
			graphics.drawImage(image, 0, 0, null);

		} finally {

			graphics.dispose();
		}

		return rgb;
	}

	private static BufferedImage limit(final BufferedImage image) {

		final int side = Math.max(image.getWidth(), image.getHeight());
		if (side <= MAX_SIDE) {

			return image;
		}

		final double scale        = (double) MAX_SIDE / side;
		final int width           = Math.max(1, (int) Math.round(image.getWidth() * scale));
		final int height          = Math.max(1, (int) Math.round(image.getHeight() * scale));
		final BufferedImage small = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		final Graphics2D graphics = small.createGraphics();

		try {

			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			graphics.drawImage(image, 0, 0, width, height, null);

		} finally {

			graphics.dispose();
		}

		return small;
	}
}
