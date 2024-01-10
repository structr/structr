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
package org.structr.rest.servlet;


import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.Value;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.NoResultsException;
import org.structr.rest.exception.NotFoundException;
import org.structr.rest.resource.Resource;
import org.structr.rest.resource.ViewFilterResource;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for parsing and optimizing the resource path.
 */
public class ResourceHelper {

	private static final Logger logger = LoggerFactory.getLogger(ResourceHelper.class.getName());

	/**
	 * Parse the request path and match with possible resource patterns
	 *
	 * @param securityContext
	 * @param request
	 * @param resourceMap
	 * @param propertyView
	 * @return resourceChain
	 * @throws FrameworkException
	 */
	public static List<Resource> parsePath(final SecurityContext securityContext, final HttpServletRequest request, final Map<Pattern, Class<? extends Resource>> resourceMap, final Value<String> propertyView) throws FrameworkException {

		final String path = request.getPathInfo();

		// intercept empty path and send 204 No Content
		if (StringUtils.isBlank(path)) {

			throw new NoResultsException("No content");
		}

		// 1.: split request path into URI parts
		final String[] pathParts = path.split("[/]+");

		// 2.: create container for resource constraints
		final Set<String> propertyViews    = Services.getInstance().getConfigurationProvider().getPropertyViews();
		final List<Resource> resourceChain = new ArrayList<>(pathParts.length);

		// 3.: try to assign resource constraints for each URI part
		for (int i = 0; i < pathParts.length; i++) {

			// eliminate empty strings
			final String part = pathParts[i].trim();

			if (part.length() > 0) {

				boolean found = false;

				// check views first
				if (propertyViews.contains(part)) {

					Resource resource = new ViewFilterResource();
					resource.checkAndConfigure(part, securityContext, request);
					resource.configurePropertyView(propertyView);

					resourceChain.add(resource);

					// mark this part as successfully parsed
					found = true;

				} else {

					// look for matching pattern
					for (Map.Entry<Pattern, Class<? extends Resource>> entry : resourceMap.entrySet()) {

						Pattern pattern = entry.getKey();
						Matcher matcher = pattern.matcher(pathParts[i]);

						if (matcher.matches()) {

							Class<? extends Resource> type = entry.getValue();
							Resource resource              = null;

							try {

								// instantiate resource constraint
								resource = type.newInstance();
							} catch (Throwable t) {

								logger.warn("Error instantiating resource class", t);

							}

							if (resource != null) {

								// set security context
								resource.setSecurityContext(securityContext);

								if (resource.checkAndConfigure(part, securityContext, request)) {

									logger.debug("{} matched, adding resource of type {} for part {}", new Object[] { matcher.pattern(), type.getName(), part });

									// allow constraint to modify context
									resource.configurePropertyView(propertyView);

									// add constraint and go on
									resourceChain.add(resource);

									found = true;

									// first match wins, so choose priority wisely ;)
									break;

								}
							}
						}
					}
				}

				if (!found) {

					throw new NotFoundException("Cannot resolve URL path");
				}
			}
		}

		return resourceChain;
	}

	/**
	 * Optimize the resource chain by trying to combine two resources to a new one
	 *
	 * @param securityContext
	 * @param request
	 * @param resourceMap
	 * @param propertyView
	 * @return finalResource
	 * @throws FrameworkException
	 */
	public static Resource optimizeNestedResourceChain(final SecurityContext securityContext, final HttpServletRequest request, final Map<Pattern, Class<? extends Resource>> resourceMap, final Value<String> propertyView) throws FrameworkException {

		final List<Resource> resourceChain = ResourceHelper.parsePath(securityContext, request, resourceMap, propertyView);

		ViewFilterResource view = null;
		int num                 = resourceChain.size();
		boolean found           = false;

		do {

			for (Iterator<Resource> it = resourceChain.iterator(); it.hasNext(); ) {

				Resource constr = it.next();

				if (constr instanceof ViewFilterResource) {

					view = (ViewFilterResource) constr;

					it.remove();
				}
			}

			found = false;

			try {

				for (int i = 0; i < num; i++) {

					Resource firstElement       = resourceChain.get(i);
					Resource secondElement      = resourceChain.get(i + 1);
					Resource combinedConstraint = firstElement.tryCombineWith(secondElement);

					if (combinedConstraint != null) {

						// remove source constraints
						resourceChain.remove(firstElement);
						resourceChain.remove(secondElement);

						// add combined constraint
						resourceChain.add(i, combinedConstraint);

						// signal success
						found = true;
					}
				}

			} catch (Throwable t) {

				// ignore exceptions thrown here but make it possible to set a breakpoint
				final boolean test = false;
			}

		} while (found);

		if (resourceChain.size() == 1) {

			Resource finalResource = resourceChain.get(0);

			if (view != null) {

				finalResource = finalResource.tryCombineWith(view);
			}

			if (finalResource == null) {
				// fall back to original resource
				finalResource = resourceChain.get(0);
			}

			return finalResource;

		} else {

			logger.warn("Resource chain evaluation for path {} resulted in {} entries, returning status code 400.", new Object[] { request.getPathInfo(), resourceChain.size() });
		}

		throw new IllegalPathException("Cannot resolve URL path");

	}
}
