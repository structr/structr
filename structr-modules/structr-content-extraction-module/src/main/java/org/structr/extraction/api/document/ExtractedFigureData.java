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

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A figure found in a document, with its rendered image.
 *
 * @param page        the 1-based page, or null for formats without pages
 * @param position    reading order within the document, 0-based
 * @param label       the label of the caption, e.g. "Figure 3", or null
 * @param caption     the caption including its label, or null
 * @param source      where the figure comes from
 * @param bbox        x, y, width and height on the page in points from the top left corner, or null
 * @param width       width of the image in pixels
 * @param height      height of the image in pixels
 * @param mediaType   the media type of the image, always image/png
 * @param image       the image
 * @param checksum    SHA-256 of the image, hex encoded
 * @param mentions    sentences of the text that refer to the label
 * @param text        what the figure says in words: the caption and the text inside the figure (axis labels,
 *                    legends, table cells), one line each; empty if there is none
 */
public record ExtractedFigureData(Integer page, int position, String label, String caption, FigureSource source, double[] bbox, int width, int height, String mediaType,
	byte[] image, String checksum, List<String> mentions, String text) {

	public ExtractedFigureData {

		mentions = List.copyOf(mentions);
		text     = text != null ? text : "";
	}

	/**
	 * The figure with more text, e.g. what OCR read in its image, added on a line of its own.
	 */
	public ExtractedFigureData withMoreText(final String more) {

		if (more == null || more.isBlank()) {

			return this;
		}

		return new ExtractedFigureData(page, position, label, caption, source, bbox, width, height, mediaType, image, checksum, mentions, text.isEmpty() ? more.strip() : text + "\n" + more.strip());
	}

	/**
	 * The figure as a map. The image is {@code { data: <base64>, mediaType }}, the form an image is passed
	 * to an LLM in, so it can be handed on unchanged. The keys are the property names of DocumentFigure, so
	 * the map can be stored as one; the checksum is {@code imageHash}, since File has a checksum of its own.
	 *
	 * @param includeImage whether to include the image
	 */
	public Map<String, Object> toMap(final boolean includeImage) {

		final Map<String, Object> map = new LinkedHashMap<>();

		map.put("page",      page);
		map.put("position",  position);
		map.put("label",     label);
		map.put("caption",   caption);
		map.put("source",    source.getValue());
		map.put("bbox",      bbox != null ? List.of(bbox[0], bbox[1], bbox[2], bbox[3]) : null);
		map.put("width",     width);
		map.put("height",    height);
		map.put("imageHash", checksum);
		map.put("mentions",  mentions);
		map.put("text",      text);

		if (includeImage) {

			final Map<String, Object> data = new LinkedHashMap<>();

			data.put("data",      Base64.getEncoder().encodeToString(image));
			data.put("mediaType", mediaType);

			map.put("image", data);
		}

		return map;
	}
}
