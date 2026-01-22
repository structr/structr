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
package org.structr.web.common;

import org.apache.commons.lang3.StringUtils;
import org.structr.common.ContextStore;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.path.PagePath;

import java.util.Map;
import java.util.Map.Entry;

/**
 */
public class PagePaths {

	public static Page findPageAndResolveParameters(final RenderContext renderContext, final String fullPath) throws FrameworkException {

		// we need to split the path ourselves because we need to be able to detect "empty" parts (//)
		final String[] requestParts = StringUtils.splitPreserveAllTokens(StringUtils.substringAfter(fullPath, "/"), "/");
		final App app               = StructrApp.getInstance();
		final int requestLength     = requestParts.length;

		if (requestLength > 0) {

			for (final NodeInterface node : app.nodeQuery(StructrTraits.PAGE_PATH).getResultStream()) {

				final PagePath pathCandidate     = node.as(PagePath.class);
				final Map<String, Object> values = pathCandidate.tryResolvePath(requestParts);
				if (values != null) {

					final ContextStore contextStore = renderContext.getSecurityContext().getContextStore();

					// handle values
					for (final Entry<String, Object> entry : values.entrySet()) {

						contextStore.setConstant(entry.getKey(), entry.getValue());
					}

					final Page resolvedPage = pathCandidate.getPage();
					if (resolvedPage != null) {

						// only return resolved page if it is visible for the current user (or public)
						if (renderContext.getSecurityContext().isReadable(resolvedPage, false, false)) {

							return resolvedPage;
						}
					}
				}
			}
		}

		return null;
	}
}
