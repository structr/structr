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

import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.util.Matrix;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Collects what is drawn on a PDF page besides text: images, and paths that are stroked or filled. Charts
 * drawn as vector graphics consist of many such paths (axes, bars, lines, markers), so they are found as
 * well as embedded pictures.
 *
 * Paths that only clip, fills in white (backgrounds) and fills that cover most of the page are left out.
 * Text is not collected here, also not text drawn with Type 3 fonts.
 */
final class PdfPageGraphics extends PDFGraphicsStreamEngine {

	/** A drawn element: its box, and whether it is an image. */
	record Element(Box box, boolean image) {
	}

	private final List<Element> elements = new ArrayList<>();
	private final PDRectangle cropBox;
	private final double pageArea;
	private final int rotation;

	private final Point2D.Float current = new Point2D.Float();
	private double minX = Double.MAX_VALUE;
	private double minY = Double.MAX_VALUE;
	private double maxX = -Double.MAX_VALUE;
	private double maxY = -Double.MAX_VALUE;

	PdfPageGraphics(final PDPage page) {

		super(page);

		this.cropBox  = page.getCropBox();
		this.pageArea = cropBox.getWidth() * cropBox.getHeight();
		this.rotation = Math.floorMod(page.getRotation(), 360);
	}

	static List<Element> collect(final PDPage page) throws IOException {

		final PdfPageGraphics graphics = new PdfPageGraphics(page);

		graphics.processPage(page);

		return graphics.elements;
	}

	@Override
	public void appendRectangle(final Point2D p0, final Point2D p1, final Point2D p2, final Point2D p3) {

		add(p0);
		add(p1);
		add(p2);
		add(p3);

		current.setLocation(p0);
	}

	@Override
	public void drawImage(final PDImage image) {

		final Matrix ctm = getGraphicsState().getCurrentTransformationMatrix();
		final Box box    = toPage(unitSquare(ctm));

		if (box.area() < pageArea * 0.85) {

			elements.add(new Element(box, true));
		}
	}

	@Override
	public void clip(final int windingRule) {

		// the path in progress is a clipping path; endPath() follows and discards it
	}

	@Override
	public void moveTo(final float x, final float y) {

		add(x, y);
		current.setLocation(x, y);
	}

	@Override
	public void lineTo(final float x, final float y) {

		add(x, y);
		current.setLocation(x, y);
	}

	@Override
	public void curveTo(final float x1, final float y1, final float x2, final float y2, final float x3, final float y3) {

		// the control points enclose the curve, so their box is a slightly larger box of the curve
		add(x1, y1);
		add(x2, y2);
		add(x3, y3);
		current.setLocation(x3, y3);
	}

	@Override
	public Point2D getCurrentPoint() {

		return current;
	}

	@Override
	public void closePath() {
	}

	@Override
	public void endPath() {

		reset();
	}

	@Override
	public void strokePath() {

		finish(false);
	}

	@Override
	public void fillPath(final int windingRule) {

		finish(isWhite(getGraphicsState().getNonStrokingColor()));
	}

	@Override
	public void fillAndStrokePath(final int windingRule) {

		finish(false);
	}

	@Override
	public void shadingFill(final COSName shadingName) {
	}

	// ----- private methods -----
	private void finish(final boolean invisible) {

		if (minX <= maxX && !invisible) {

			final Box box = toPage(new Box(minX, minY, maxX, maxY));
			if (box.area() < pageArea * 0.85) {

				elements.add(new Element(box, false));
			}
		}

		reset();
	}

	private void reset() {

		minX = Double.MAX_VALUE;
		minY = Double.MAX_VALUE;
		maxX = -Double.MAX_VALUE;
		maxY = -Double.MAX_VALUE;
	}

	private void add(final Point2D point) {

		add(point.getX(), point.getY());
	}

	private void add(final double x, final double y) {

		minX = Math.min(minX, x);
		minY = Math.min(minY, y);
		maxX = Math.max(maxX, x);
		maxY = Math.max(maxY, y);
	}

	/**
	 * A box in PDF user space (origin bottom left) as a box on the page as it is displayed: origin top
	 * left of the crop box, turned by the page's rotation, like the rendered page and the text positions.
	 */
	private Box toPage(final Box userSpace) {

		final double x0 = userSpace.x0() - cropBox.getLowerLeftX();
		final double y0 = userSpace.y0() - cropBox.getLowerLeftY();
		final double x1 = userSpace.x1() - cropBox.getLowerLeftX();
		final double y1 = userSpace.y1() - cropBox.getLowerLeftY();
		final double w  = cropBox.getWidth();
		final double h  = cropBox.getHeight();

		// the page is turned clockwise

		return switch (rotation) {

			case 90  -> Box.of(y0, x0, y1, x1);
			case 180 -> Box.of(w - x0, y0, w - x1, y1);
			case 270 -> Box.of(h - y0, w - x0, h - y1, w - x1);
			default  -> Box.of(x0, h - y0, x1, h - y1);
		};
	}

	/**
	 * The box an image covers: images are drawn into the unit square of the current transformation.
	 */
	private static Box unitSquare(final Matrix ctm) {

		final double[][] corners = { { 0, 0 }, { 1, 0 }, { 0, 1 }, { 1, 1 } };
		double x0                = Double.MAX_VALUE;
		double y0                = Double.MAX_VALUE;
		double x1                = -Double.MAX_VALUE;
		double y1                = -Double.MAX_VALUE;

		for (final double[] corner : corners) {

			final Point2D.Float point = ctm.transformPoint((float) corner[0], (float) corner[1]);

			x0 = Math.min(x0, point.x);
			y0 = Math.min(y0, point.y);
			x1 = Math.max(x1, point.x);
			y1 = Math.max(y1, point.y);
		}

		return new Box(x0, y0, x1, y1);
	}

	private static boolean isWhite(final PDColor color) {

		try {

			return color != null && (color.toRGB() & 0xFFFFFF) == 0xFFFFFF;

		} catch (final IOException | RuntimeException ex) {

			return false;
		}
	}
}
