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

/**
 * A rectangle on a page, in points from the top left corner of the page's crop box.
 */
record Box(double x0, double y0, double x1, double y1) {

	static Box of(final double xa, final double ya, final double xb, final double yb) {

		return new Box(Math.min(xa, xb), Math.min(ya, yb), Math.max(xa, xb), Math.max(ya, yb));
	}

	double width() {

		return x1 - x0;
	}

	double height() {

		return y1 - y0;
	}

	double area() {

		return width() * height();
	}

	Box union(final Box other) {

		return new Box(Math.min(x0, other.x0), Math.min(y0, other.y0), Math.max(x1, other.x1), Math.max(y1, other.y1));
	}

	Box expand(final double margin) {

		return new Box(x0 - margin, y0 - margin, x1 + margin, y1 + margin);
	}

	Box clip(final double width, final double height) {

		return new Box(Math.max(0, x0), Math.max(0, y0), Math.min(width, x1), Math.min(height, y1));
	}

	boolean intersects(final Box other) {

		return x0 <= other.x1 && other.x0 <= x1 && y0 <= other.y1 && other.y0 <= y1;
	}

	/**
	 * The part of this box's area that lies inside the other box, from 0 to 1.
	 */
	double fractionInside(final Box other) {

		final double w = Math.min(x1, other.x1) - Math.max(x0, other.x0);
		final double h = Math.min(y1, other.y1) - Math.max(y0, other.y0);

		if (w <= 0 || h <= 0) {

			// a line without height (e.g. a rule) is inside if it overlaps

			return area() == 0 && intersects(other) ? 1 : 0;
		}

		return area() > 0 ? (w * h) / area() : 1;
	}

	/**
	 * How much the horizontal extents overlap, relative to the narrower box, from 0 to 1.
	 */
	double horizontalOverlap(final Box other) {

		final double overlap = Math.min(x1, other.x1) - Math.max(x0, other.x0);
		final double narrow  = Math.min(width(), other.width());

		if (overlap <= 0) {

			return 0;
		}

		return narrow > 0 ? Math.min(1, overlap / narrow) : 1;
	}

	double[] toArray() {

		return new double[] { round(x0), round(y0), round(width()), round(height()) };
	}

	private static double round(final double value) {

		return Math.round(value * 10) / 10.0;
	}
}
