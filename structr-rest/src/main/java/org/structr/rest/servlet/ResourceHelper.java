/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.Value;
import org.structr.core.ViewTransformation;
import org.structr.core.app.StructrApp;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.NoResultsException;
import org.structr.rest.exception.NotFoundException;
import org.structr.rest.resource.Resource;
import org.structr.rest.resource.TransformationResource;
import org.structr.rest.resource.ViewFilterResource;

//~--- classes ----------------------------------------------------------------

/**
 * Helper class for parsing and optimizing the resource path
 *
 *  @author Axel Morgner
 */
public class ResourceHelper {

	private static final Logger logger = Logger.getLogger(ResourceHelper.class.getName());

	//~--- methods --------------------------------------------------------

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
		if (!StringUtils.isNotBlank(path)) {

			throw new NoResultsException();
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

								logger.log(Level.WARNING, "Error instantiating resource class", t);

							}

							if (resource != null) {

								// set security context
								resource.setSecurityContext(securityContext);

								if (resource.checkAndConfigure(part, securityContext, request)) {

									logger.log(Level.FINE, "{0} matched, adding resource of type {1} for part {2}", new Object[] { matcher.pattern(), type.getName(),
										part });

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

					throw new NotFoundException();
				}

			}
		}

		return resourceChain;

	}

	/**
	 * Optimize the resource chain by trying to combine two resources to a new one
	 *
	 * @param resourceChain
	 * @return finalResource
	 * @throws FrameworkException
	 */
	public static Resource optimizeNestedResourceChain(final List<Resource> resourceChain) throws FrameworkException {

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

				// ignore exceptions thrown here
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

		}

		throw new IllegalPathException();

	}

	/**
	 * Apply view transformation on final resource, if any
	 *
	 * @param request
	 * @param securityContext
	 * @param finalResource
	 * @param propertyView
	 * @return transformedResource
	 * @throws FrameworkException
	 */
	public static Resource applyViewTransformation(final HttpServletRequest request, final SecurityContext securityContext, final Resource finalResource, final Value<String> propertyView) throws FrameworkException {

		Resource transformedResource = finalResource;

		// add view transformation
		Class type = finalResource.getEntityClass();
		if (type != null) {

			ViewTransformation transformation = StructrApp.getConfiguration().getViewTransformation(type, propertyView.get(securityContext));
			if (transformation != null) {

				transformedResource = transformedResource.tryCombineWith(new TransformationResource(securityContext, transformation));
			}
		}

		return transformedResource;
	}

}
