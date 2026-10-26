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
package org.structr.extraction.api.document;

/**
 * Where a figure comes from.
 */
public enum FigureSource {

	/** A region of a PDF page, rendered: charts drawn as vector graphics are found this way, too. */
	REGION("region"),

	/** A whole PDF page, for a caption whose figure could not be told apart from the rest of the page. */
	PAGE("page"),

	/** An image embedded in the document, e.g. a picture in a Word document or an e-mail. */
	EMBEDDED("embedded"),

	/** The file itself, which is an image. */
	FILE("file");

	private final String value;

	FigureSource(final String value) {

		this.value = value;
	}

	public String getValue() {

		return value;
	}
}
