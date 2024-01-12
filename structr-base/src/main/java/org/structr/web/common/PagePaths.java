/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.web.common;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.path.PagePath;
import org.structr.web.entity.path.PagePathParameter;

/**
 */
public class PagePaths {

	private static final Map<String, Pattern> patterns = new LinkedHashMap<>();

	public static PagePath resolve(final String fullPath) throws FrameworkException {

		final App app  = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			for (final PagePath path : app.nodeQuery(PagePath.class).getResultStream()) {

				final String uuid = path.getUuid();

				Pattern pattern = patterns.get(uuid);
				if (pattern == null) {

					pattern = Pattern.compile(getPattern(path));
					patterns.put(uuid, pattern);
				}

				final Matcher matcher = pattern.matcher(fullPath);
				if (matcher.matches()) {

					return path;
				}
			}

			tx.success();
		}

		return null;
	}

	private static String getPattern(final PagePath path) {

		final StringBuilder buf = new StringBuilder();

		final Page page = path.getPage();
		if (page != null) {

			buf.append("/");
			buf.append(page.getName());

			for (final PagePathParameter parameter : path.getParameters()) {

				buf.append("/");
				buf.append("(.*)");
			}
		}

		return buf.toString();
	}
}
