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
import java.util.Collection;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
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

	private static final Logger logger   = LoggerFactory.getLogger(SchemaMethodResource.class);
	private TypeResource typeResource   = null;
	private TypeResource methodResource = null;
	private String source               = null;

	public SchemaMethodResource(final SecurityContext securityContext, final TypeResource typeResource, final TypeResource methodResource) throws IllegalPathException {

		this.typeResource    = typeResource;
		this.methodResource  = methodResource;
		this.securityContext = securityContext;
		this.source          = findMethodSource(typeResource.getEntityClass(), methodResource.getRawType());
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

		if (source != null) {

			try (final Tx tx = app.tx()) {

				result = SchemaMethodResource.invoke(securityContext, null, source, propertySet);
				tx.success();
			}
		}

		if (result == null) {
			throw new IllegalPathException("Type and method name do not match the given path.");
		}

		return result;
	}

	@Override
	public RestMethodResult doPut(final Map<String, Object> propertySet) throws FrameworkException {
		throw new IllegalMethodException("PUT not allowed on " + getResourceSignature());
	}

	// ----- private methods -----
	public static RestMethodResult invoke(final SecurityContext securityContext, final GraphObject entity, final String source, final Map<String, Object> propertySet) throws FrameworkException {
		return SchemaMethodResource.wrapInResult(Actions.execute(securityContext, entity, "${" + source + "}", propertySet));
	}

	public static RestMethodResult wrapInResult(final Object obj) {

		RestMethodResult result = null;

		if (obj instanceof RestMethodResult) {

			result = (RestMethodResult)obj;

		} else {

			result = new RestMethodResult(200);

			// unwrap nested object(s)
			SchemaMethodResource.unwrapTo(obj, result);
		}

		return result;

	}

	public static String findMethodSource(final Class type, final String methodName) throws IllegalPathException {

		try {
			final App app               = StructrApp.getInstance();
			final String typeName       = type.getSimpleName();

			// first step: schema node or one of its parents
			SchemaNode schemaNode = app.nodeQuery(SchemaNode.class).andName(typeName).getFirst();
			while (schemaNode != null) {

				for (final SchemaMethod method : schemaNode.getProperty(SchemaNode.schemaMethods)) {

					if (methodName.equals(method.getName())) {

						return method.getProperty(SchemaMethod.source);
					}
				}

				Class parentType = type.getSuperclass();
				if (parentType != null) {

					// skip non-dynamic types
					if (parentType.getSimpleName().equals(typeName) || !parentType.getName().startsWith("org.structr.dynamic.")) {
						parentType = parentType.getSuperclass();
					}

					if (parentType != null && parentType.getName().startsWith("org.structr.dynamic.")) {

						schemaNode = app.nodeQuery(SchemaNode.class).andName(parentType.getSimpleName()).getFirst();

					} else {

						break;
					}

				} else {

					break;
				}
			}

		} catch (FrameworkException fex) {}

		throw new IllegalPathException("Type and method name do not match the given path.");
	}

	public static void unwrapTo(final Object source, final RestMethodResult result) {

		if (source != null) {

			final Object unwrapped = Context.jsToJava(source, ScriptRuntime.ObjectClass);
			if (unwrapped.getClass().isArray()) {

				for (final Object element : (Object[])unwrapped) {
					unwrapTo(element, result);
				}

			} else if (unwrapped instanceof Collection) {

				for (final Object element : (Collection)unwrapped) {
					unwrapTo(element, result);
				}

			} else if (unwrapped instanceof GraphObject) {

				result.addContent((GraphObject)unwrapped);
			}
		}
	}

	private Method determineMethod(final Class type, final String methodName) throws IllegalPathException {

		Method result = null;

		try {
			result = type.getMethod(methodName, Map.class);

		} catch (NoSuchMethodException ignore) {

			// fallback: check exported schema methods
			final Map<String, Method> methods = StructrApp.getConfiguration().getExportedMethodsForType(type);
			result =  methods.get(methodName);
		}

		return result;
	}
}
