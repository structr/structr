/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.rest.resource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalMethodException;
import org.structr.rest.exception.IllegalPathException;

/**
 *
 */
public class SchemaMethodResource extends SortableResource {

	private static final Logger logger  = LoggerFactory.getLogger(SchemaMethodResource.class.getName());

	private TypeResource typeResource   = null;
	private TypeResource methodResource = null;
	private String methodName           = null;
	private Method method               = null;
	private Class type                  = null;

	public SchemaMethodResource(final SecurityContext securityContext, final TypeResource typeResource, final TypeResource methodResource) throws IllegalPathException {

		this.typeResource    = typeResource;
		this.methodResource  = methodResource;
		this.securityContext = securityContext;

		// check if the given type has a method of the given name here
		this.methodName = methodResource.getRawType();
		this.type       = typeResource.getEntityClass();

		try {
			this.method = type.getMethod(methodName, Map.class);

		} catch (NoSuchMethodException nsex) {
			throw new IllegalPathException("Type and method name do not match the given path.");
		}

		if (method == null) {
			throw new IllegalPathException("Type and method name do not match the given path.");
		}
	}

	@Override
	public boolean checkAndConfigure(final String part, final SecurityContext securityContext, final HttpServletRequest request) throws FrameworkException {

		// never return this method in a URL path evaluation
		return false;
	}

	@Override
	public String getResourceSignature() {
		return typeResource.getResourceSignature() + "/" + methodResource.getResourceSignature();
	}

	@Override
	public Result doGet(final PropertyKey sortKey, final boolean sortDescending, final int pageSize, final int page, final String offsetId) throws FrameworkException {
		throw new IllegalMethodException("GET not allowed on " + getResourceSignature());
	}

	@Override
	public RestMethodResult doPost(final Map<String, Object> propertySet) throws FrameworkException {

		final App app = StructrApp.getInstance(securityContext);

		try (final Tx tx = app.tx()) {

			if (type != null && method != null) {

				try {

					final Object instance = instantiate(type);
					final Object obj      = method.invoke(instance, propertySet);

					if (obj instanceof RestMethodResult) {

						return (RestMethodResult)obj;

					} else {

						final RestMethodResult result = new RestMethodResult(200);

						// unwrap nested object(s)
						StaticRelationshipResource.unwrapTo(obj, result);

						return result;
					}

				} catch (InstantiationException | InvocationTargetException | IllegalAccessException ex) {
					logger.warn("Exception while executing {}.{}: {}", type.getSimpleName(), methodName, ex.getMessage());

				}
			}

			tx.success();
		}

		throw new IllegalPathException("Type and method name do not match the given path.");
	}

	@Override
	public RestMethodResult doPut(final Map<String, Object> propertySet) throws FrameworkException {
		throw new IllegalMethodException("PUT not allowed on " + getResourceSignature());
	}

	// ----- private methods -----
	private Object instantiate(final Class type) throws InstantiationException, IllegalAccessException {

		final Object instance = type.newInstance();
		if (instance != null) {

			if (instance instanceof AbstractNode) {

				((AbstractNode)instance).init(securityContext, null, type, false);

			} else if (instance instanceof AbstractRelationship) {

				((AbstractRelationship)instance).init(securityContext, null, type);
			}
		}

		return instance;
	}
}
