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
package org.structr.web.datasource;

import org.apache.commons.lang3.StringUtils;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.RenderContext;

import java.util.*;
import org.structr.web.entity.dom.DOMNode;

public class TagWithCSSInfo {

	private final List<String> classes = new LinkedList<>();
	private final String source;
	private String id = null;
	private String tag = null;

	public TagWithCSSInfo(final String source) {

		this.source = source;

		// we only support simple CSS selectors with id and class for now
		for (final String part : splitCssSelector(source)) {

			if (part.startsWith(".")) {

				classes.add(part.substring(1));

			} else if (part.startsWith("#")) {

				id = part.substring(1);

			} else {

				tag = part;
			}
		}
	}

	public void formatStartTag(final AsyncBuffer buffer) {

		formatStartTag(buffer, null, null);
	}

	public void formatStartTag(final AsyncBuffer buffer, final Map<String, String> additionalValues, final Set<String> additionalClasses) {

		final Set<String> mergedClasses = new LinkedHashSet<>(classes);

		if (additionalClasses != null) {

			mergedClasses.addAll(additionalClasses);
		}

		// apply default column width
		if (!containsAny(mergedClasses, "col-span-1", "col-span-2", "col-span-3", "col-span-4", "col-span-5", "col-span-6")) {

			mergedClasses.add("col-span-6");
		}

		if (StringUtils.isNotBlank(tag)) {

			buffer.append("<");
			buffer.append(tag);

			if (id != null) {

				buffer.append(" id=\"" + DOMNode.escapeForHtmlAttributes(id) + "\"");
			}

			if (!mergedClasses.isEmpty()) {

				buffer.append(" class=\"");
				buffer.append(StringUtils.join(mergedClasses, " "));
				buffer.append("\"");
			}

			if (additionalValues != null) {

				for (final String key : additionalValues.keySet()) {

					// attribute name and value can carry request-derived data (e.g. pagination key / channel values),
					// so restrict the name to [A-Za-z0-9-] and escape the value for an HTML attribute context (#1579)
					final String sanitizedKey = DOMNode.sanitizeAttributeName(key);
					if (sanitizedKey.isEmpty()) {

						continue;
					}

					buffer.append(" " + sanitizedKey + "=\"" + DOMNode.escapeForHtmlAttributes(additionalValues.get(key)) + "\"");
				}
			}

			buffer.append(">");
		}

	}

	public void formatEndTag(final AsyncBuffer buffer) {

		if (StringUtils.isNotBlank(tag)) {

			buffer.append("</" + tag + ">");
		}
	}

	public boolean matches(final List<String> selectors) {

		for (final String selector : selectors) {

			// match CSS class only
			if (selector.startsWith(".") && classes.contains(selector.substring(1))) {

				return true;
			}

			// match CSS class only
			if (selector.startsWith("#") && selector.substring(1).equals(id)) {

				return true;
			}

			// tag match
			if (selector.equals(source)) {

				return true;
			}
		}

		return false;
	}

	private List<String> splitCssSelector(final String source) {

		final Set<Character> separators = Set.of('.', '#');
		final List<String> parts     = new LinkedList<>();
		final StringBuilder current  = new StringBuilder();

		for (final Character c : source.toCharArray()) {

			// separator creates new part
			if (separators.contains(c)) {

				parts.add(current.toString());
				current.setLength(0);
			}

			current.append(c);
		}

		parts.add(current.toString());

		return parts;
	}

	private boolean containsAny(final Set<String> set, final String... values) {

		for (final String value : values) {

			if (set.contains(value)) {

				return true;
			}
		}

		return false;
	}
}
