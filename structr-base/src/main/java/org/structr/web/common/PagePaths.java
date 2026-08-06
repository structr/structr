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
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.path.PagePath;
import org.structr.web.servlet.HtmlServlet;
import org.structr.web.traits.definitions.PagePathTraitDefinition;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class PagePaths {

	public static Page findPageAndResolveParameters(final RenderContext renderContext, final String fullPath) throws FrameworkException {

		// we need to split the path ourselves because we need to be able to detect "empty" parts (//)
		// we also need to decode the parts ourselves because we want to be able to distinguish between "/" and "%2f" and not have it treated as a "/" which is why the fullPath is not already url decoded
		final String[] requestParts           = Arrays.stream(StringUtils.splitPreserveAllTokens(StringUtils.substringAfter(fullPath, "/"), "/")).map(PagePaths::decodePathSegment).toArray(String[]::new);

		// we fetch PagePaths privileged so they are always available for users and then the linked page is checked for visibility
		final App app                         = StructrApp.getInstance();
		final int requestLength               = requestParts.length;
		final SecurityContext securityContext = renderContext.getSecurityContext();
		final Map<String, Boolean> processPageVariablesMap = new HashMap<>();

		if (requestLength > 0) {

			for (final NodeInterface node : app.nodeQuery(StructrTraits.PAGE_PATH).sort(Traits.key(StructrTraits.PAGE_PATH, PagePathTraitDefinition.PRIORITY_PROPERTY), false).getResultStream()) {

				final PagePath pathCandidate = node.as(PagePath.class);
				final Page resolvedPage      = pathCandidate.getPage();

				if (resolvedPage != null) {

					// only process path variables, if:
					// a) we already have stored knowledge (in processPageVariablesMap) that the page is visible for the user AND site
					// b) we check that:
					//    - the resolved page is visible for the current user (or public)
					//    - the resolved page belongs to the site the request belongs to
					final boolean processPathVariables;

					if (processPageVariablesMap.containsKey(resolvedPage.getUuid())) {

						processPathVariables = processPageVariablesMap.get(resolvedPage.getUuid());

					} else {

						processPathVariables = (securityContext.isReadable(resolvedPage, false, false) && HtmlServlet.isVisibleForSite(securityContext.getRequest(), resolvedPage));
					}

					processPageVariablesMap.put(resolvedPage.getUuid(), processPathVariables);

					if (processPathVariables) {

						final Map<String, Object> values = pathCandidate.tryResolvePath(securityContext, requestParts);
						if (values != null) {

							final ContextStore contextStore = securityContext.getContextStore();

							// handle values
							for (final Entry<String, Object> entry : values.entrySet()) {

								contextStore.setConstant(entry.getKey(), entry.getValue());
							}

							return resolvedPage;
						}
					}
				}
			}
		}

		return null;
	}

	static String decodePathSegment(String segment) {

		return URI.create("/" + segment).getPath().substring(1);
	}
}
