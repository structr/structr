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

import com.google.gson.GsonBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The payload frontend.js sends for a trigger element: the element's data-* attributes under their dataset keys
 * (data-structr-id-expression becomes structrIdExpression), without empty values, so a test posts to the event
 * endpoint what a browser would post for the element.
 */
final class BrowserEventPayload {

	private BrowserEventPayload() {}

	/**
	 * @return the dataset of the first element in the page that carries the given data-structr-id
	 */
	static Map<String, Object> of(final String html, final String elementUuid) {

		final Element element = Jsoup.parse(html).selectFirst("[data-structr-id=\"" + elementUuid + "\"]");
		if (element == null) {

			throw new IllegalStateException("No element with data-structr-id " + elementUuid + " in the rendered page");
		}

		return dataset(element);
	}

	static Map<String, Object> dataset(final Element element) {

		final Map<String, Object> dataset = new LinkedHashMap<>();

		for (final Attribute attribute : element.attributes()) {

			final String name  = attribute.getKey();
			final String value = attribute.getValue();

			// frontend.js skips empty values
			if (name.startsWith("data-") && !value.isEmpty()) {

				dataset.put(datasetKey(name.substring(5)), value);
			}
		}

		return dataset;
	}

	static String toJson(final Map<String, Object> payload) {

		return new GsonBuilder().disableHtmlEscaping().create().toJson(payload);
	}

	// the HTML rule for dataset names: a hyphen followed by a lowercase ASCII letter becomes that letter in upper case
	private static String datasetKey(final String attributeName) {

		final StringBuilder key = new StringBuilder();

		for (int i = 0; i < attributeName.length(); i++) {

			final char c = attributeName.charAt(i);

			if (c == '-' && i + 1 < attributeName.length() && attributeName.charAt(i + 1) >= 'a' && attributeName.charAt(i + 1) <= 'z') {

				key.append(Character.toUpperCase(attributeName.charAt(++i)));

			} else {

				key.append(c);
			}
		}

		return key.toString();
	}
}
