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

import java.lang.reflect.Method;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalMethodException;
import org.structr.rest.exception.IllegalPathException;
import org.structr.schema.action.Actions;

/**
 *
 */
public class SchemaMethodResource extends SortableResource {

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
		this.method     = StructrApp.getConfiguration().getExportedMethodsForType(type).get(methodName);

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

		final App app           = StructrApp.getInstance(securityContext);
		RestMethodResult result = null;

		try (final Tx tx = app.tx()) {

			final SchemaNode schemaNode = app.nodeQuery(SchemaNode.class).andName(method.getDeclaringClass().getSimpleName()).getFirst();
			if (schemaNode != null) {

				final SchemaMethod schemaMethod = app.nodeQuery(SchemaMethod.class)
					.and(SchemaMethod.name, method.getName())
					.and(SchemaMethod.schemaNode, schemaNode)
					.getFirst();

				if (schemaMethod != null) {

					final String source = schemaMethod.getProperty(SchemaMethod.source);
					if (StringUtils.isNotBlank(source)) {

						final Object obj = Actions.execute(securityContext, null, "${" + source + "}", propertySet);
						if (obj instanceof RestMethodResult) {

							result = (RestMethodResult)obj;

						} else {

							result = new RestMethodResult(200);

							// unwrap nested object(s)
							StaticRelationshipResource.unwrapTo(obj, result);
						}
					}
				}
			}

			tx.success();
		}

		if (result != null) {

			return result;

		} else {

			throw new IllegalPathException("Type and method name do not match the given path.");
		}
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
