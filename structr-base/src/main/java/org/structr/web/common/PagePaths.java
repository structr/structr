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
import org.structr.api.config.Settings;
import org.structr.common.ContextStore;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.path.PagePath;
import org.structr.web.entity.path.PagePathParameter;
import org.structr.web.servlet.HtmlServlet;
import org.structr.web.traits.definitions.PagePathParameterTraitDefinition;
import org.structr.web.traits.definitions.PagePathTraitDefinition;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class PagePaths {

	public static Page findIndexPageRoute(final RenderContext renderContext) throws FrameworkException {

		// find URL Routes with name = "/"
		final App app                         = StructrApp.getInstance();
		final SecurityContext securityContext = renderContext.getSecurityContext();

		for (final NodeInterface node : app.nodeQuery(StructrTraits.PAGE_PATH).name("/").sort(Traits.key(StructrTraits.PAGE_PATH, PagePathTraitDefinition.PRIORITY_PROPERTY), false).getResultStream()) {

			final PagePath pathCandidate = node.as(PagePath.class);
			final Page pageForPath       = pathCandidate.getPage();

			if (pageForPath != null) {

				if (securityContext.isReadable(pageForPath, false, false) && HtmlServlet.isVisibleForSite(securityContext.getRequest(), pageForPath)) {

					return pageForPath;
				}
			}
		}

		return null;
	}

	public static Page findPageAndResolveParameters(final RenderContext renderContext, final String fullPath) throws FrameworkException {

		final Match match = findMatch(renderContext, fullPath);
		if (match != null) {

			apply(renderContext, match, true);

			return match.page();
		}

		return null;
	}

	/** A partial reload carries its page's path: only a route of the partial's own page counts, and a current object from the reload URL is kept. */
	public static void resolveParametersForPartial(final RenderContext renderContext, final String fullPath, final DOMNode partial) throws FrameworkException {

		final Match match = findMatch(renderContext, fullPath);
		if (match != null && partial.getOwnerDocument() != null && partial.getOwnerDocument().getUuid().equals(match.page().getUuid())) {

			apply(renderContext, match, renderContext.getDetailsDataObject() == null);
		}
	}

	// ----- private methods -----
	private record Match(Page page, PagePath path, Map<String, Object> values) {}

	private static Match findMatch(final RenderContext renderContext, final String fullPath) throws FrameworkException {

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
				final Page pageForPath       = pathCandidate.getPage();

				if (pageForPath != null) {

					// only process path variables, if:
					// a) we already have stored knowledge (in processPageVariablesMap) that the page is visible for the user AND site
					// b) we check that:
					//    - the resolved page is visible for the current user (or public)
					//    - the resolved page belongs to the site the request belongs to
					final boolean processPathVariables;

					if (processPageVariablesMap.containsKey(pageForPath.getUuid())) {

						processPathVariables = processPageVariablesMap.get(pageForPath.getUuid());

					} else {

						processPathVariables = (securityContext.isReadable(pageForPath, false, false) && HtmlServlet.isVisibleForSite(securityContext.getRequest(), pageForPath));
					}

					processPageVariablesMap.put(pageForPath.getUuid(), processPathVariables);

					if (processPathVariables) {

						final Map<String, Object> values = pathCandidate.tryResolvePath(securityContext, requestParts);
						if (values != null) {

							return new Match(pageForPath, pathCandidate, values);
						}
					}
				}
			}
		}

		return null;
	}

	private static void apply(final RenderContext renderContext, final Match match, final boolean setCurrentObject) throws FrameworkException {

		final SecurityContext securityContext = renderContext.getSecurityContext();
		final ContextStore contextStore       = securityContext.getContextStore();

		for (final Entry<String, Object> entry : match.values().entrySet()) {

			contextStore.setConstant(entry.getKey(), entry.getValue());
		}

		if (setCurrentObject) {

			final GraphObject current = resolveCurrentObject(securityContext, match.path(), match.values());
			if (current != null) {

				renderContext.setDetailsDataObject(current);
			}
		}
	}

	/** The parameter marked as the current object, or else the only Node parameter of the route; a value that is not an object yet is looked up as an id. */
	static GraphObject resolveCurrentObject(final SecurityContext securityContext, final PagePath path, final Map<String, Object> values) throws FrameworkException {

		PagePathParameter chosen   = null;
		PagePathParameter onlyNode = null;
		int nodeParameters         = 0;

		for (final PagePathParameter parameter : path.getParameters()) {

			if (parameter.getIsCurrentObject()) {

				chosen = parameter;
				break;
			}

			if (PagePathParameterTraitDefinition.PathParameterValueType.Node.name().equals(parameter.getValueType())) {

				onlyNode = parameter;
				nodeParameters++;
			}
		}

		if (chosen == null && nodeParameters == 1) {

			chosen = onlyNode;
		}

		if (chosen == null) {

			return null;
		}

		final Object value = values.get(chosen.getName());

		if (value instanceof GraphObject graphObject) {

			return graphObject;
		}

		if (value != null && Settings.isValidUuid(value.toString())) {

			return StructrApp.getInstance(securityContext).nodeQuery().and().uuid(value.toString()).getFirst();
		}

		return null;
	}

	static String decodePathSegment(String segment) {

		return URI.create("/" + segment).getPath().substring(1);
	}
}
