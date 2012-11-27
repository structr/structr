/*
 *  Copyright (C) 2012 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.rest.servlet;

import org.apache.commons.lang.StringUtils;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Value;
import org.structr.core.property.PropertyKey;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.NoResultsException;
import org.structr.rest.exception.NotFoundException;
import org.structr.rest.resource.RelationshipFollowingResource;
import org.structr.rest.resource.Resource;
import org.structr.rest.resource.SchemaResource;
import org.structr.rest.resource.ViewFilterResource;

//~--- JDK imports ------------------------------------------------------------

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import org.structr.core.EntityContext;
import org.structr.core.ViewTransformation;
import org.structr.rest.resource.TransformationResource;

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
	 * @param defaultIdProperty
	 * @return
	 * @throws FrameworkException 
	 */
	protected static List<Resource> parsePath(final SecurityContext securityContext, final HttpServletRequest request, final Map<Pattern, Class<? extends Resource>> resourceMap,
		final Value<String> propertyView, final PropertyKey defaultIdProperty)
		throws FrameworkException {

		String path = request.getPathInfo();

		// intercept empty path and send 204 No Content
		if (!StringUtils.isNotBlank(path)) {

			throw new NoResultsException();
		}

		// 1.: split request path into URI parts
		String[] pathParts = path.split("[/]+");

		// 2.: create container for resource constraints
		List<Resource> constraintChain = new ArrayList<Resource>(pathParts.length);

		// 3.: try to assign resource constraints for each URI part
		for (int i = 0; i < pathParts.length; i++) {

			// eliminate empty strings
			String part = pathParts[i].trim();

			if (part.length() > 0) {

				boolean found = false;

				// Special resource: _schema contains schema information
				if ("_schema".equals(part)) {

					SchemaResource resource = new SchemaResource();

					resource.checkAndConfigure("_schema", securityContext, request);
					constraintChain.add(resource);

					found = true;

					break;

				}

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

							logger.log(Level.WARNING, "Error instantiating constraint class", t);

						}

						if (resource != null) {

							// set security context
							resource.setSecurityContext(securityContext);

							if (resource.checkAndConfigure(part, securityContext, request)) {

								logger.log(Level.FINE, "{0} matched, adding constraint of type {1} for part {2}", new Object[] { matcher.pattern(), type.getName(),
									part });

								// allow constraint to modify context
								resource.configurePropertyView(propertyView);
								resource.configureIdProperty(defaultIdProperty);

								// add constraint and go on
								constraintChain.add(resource);

								found = true;

								// first match wins, so choose priority wisely ;)
								break;

							}
						}

					}

				}

				if (!found) {

					throw new NotFoundException();
				}

			}
		}

		return constraintChain;

	}

	/**
	 * Optimize the resource chain by trying to combine two resources to a new one
	 * 
	 * @param constraintChain
	 * @param defaultIdProperty
	 * @return
	 * @throws FrameworkException 
	 */
	protected static Resource optimizeConstraintChain(final List<Resource> constraintChain, final PropertyKey defaultIdProperty) throws FrameworkException {

		ViewFilterResource view = null;
		int num                 = constraintChain.size();
		boolean found           = false;
		int iterations          = 0;

		do {

			StringBuilder chain = new StringBuilder();

			for (Iterator<Resource> it = constraintChain.iterator(); it.hasNext(); ) {

				Resource constr = it.next();

				chain.append(constr.getClass().getSimpleName());
				chain.append(", ");

				if (constr instanceof ViewFilterResource) {

					view = (ViewFilterResource) constr;

					it.remove();

				}

			}

			logger.log(Level.FINE, "########## Constraint chain after iteration {0}: {1}", new Object[] { iterations, chain.toString() });

			found = false;

			try {

				for (int i = 0; i < num; i++) {

					Resource firstElement       = constraintChain.get(i);
					Resource secondElement      = constraintChain.get(i + 1);
					Resource combinedConstraint = firstElement.tryCombineWith(secondElement);

					if (combinedConstraint != null) {

						logger.log(Level.FINE, "Combined constraint {0} and {1} to {2}", new Object[] { firstElement.getClass().getSimpleName(),
							secondElement.getClass().getSimpleName(), combinedConstraint.getClass().getSimpleName() });

						// remove source constraints
						constraintChain.remove(firstElement);
						constraintChain.remove(secondElement);

						// add combined constraint
						constraintChain.add(i, combinedConstraint);

						// signal success
						found = true;

						if (combinedConstraint instanceof RelationshipFollowingResource) {

							break;
						}

					}

				}

			} catch (Throwable t) {

				// ignore exceptions thrown here
			}

			iterations++;

		} while (found);

		StringBuilder chain = new StringBuilder();

		for (Resource constr : constraintChain) {

			chain.append(constr.getClass().getSimpleName());
			chain.append(", ");

		}

		logger.log(Level.FINE, "Final constraint chain {0}", chain.toString());

		if (constraintChain.size() == 1) {

			Resource finalConstraint = constraintChain.get(0);

			if (view != null) {

				finalConstraint = finalConstraint.tryCombineWith(view);
			}

			// inform final constraint about the configured ID property
			finalConstraint.configureIdProperty(defaultIdProperty);

			return finalConstraint;

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
	 * @return
	 * @throws FrameworkException 
	 */
	protected static Resource applyViewTransformation(final HttpServletRequest request, final SecurityContext securityContext, final Resource finalResource, final Value<String> propertyView) throws FrameworkException {

		Resource transformedResource = finalResource;

		// add view transformation
		Class type = finalResource.getEntityClass();
		if (type != null) {
			
			ViewTransformation transformation = EntityContext.getViewTransformation(type, propertyView.get(securityContext));
			if (transformation != null) {
				transformedResource = transformedResource.tryCombineWith(new TransformationResource(securityContext, transformation));
			}
		}

		return transformedResource;
	}
	
}
