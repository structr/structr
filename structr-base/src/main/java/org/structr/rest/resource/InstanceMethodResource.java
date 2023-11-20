/*
 * Copyright (C) 2010-2023 Structr GmbH
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.search.SortOrder;
import org.structr.api.util.ResultStream;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedScriptException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.Tx;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalMethodException;
import org.structr.rest.exception.IllegalPathException;
import org.structr.schema.action.Actions;

import java.util.Collection;
import java.util.Map;
import org.structr.api.APICall;
import org.structr.api.APICallHandler;
import org.structr.schema.SchemaHelper;

/**
 *
 */
public class InstanceMethodResource extends AbstractTypeIdNameResource {

	@Override
	public APICallHandler handleTypeIdName(final SecurityContext securityContext, final APICall call, final String typeName, final String uuid, final String name) {

		final Class entityClass = SchemaHelper.getEntityClassForRawType(typeName);
		if (entityClass != null) {

			final Map<String, Method> methods = StructrApp.getConfiguration().getExportedMethodsForType(entityClass);
			if (methods.containsKey(name)) {

				return new InstanceMethodResourceHandler(securityContext, call.getURL(), entityClass, uuid, methods.get(name));
			}
		}

		return null;
	}


	private class InstanceMethodResourceHandler extends APICallHandler {

		private static final Logger logger = LoggerFactory.getLogger(InstanceMethodResourceHandler.class);
		private Class entityClass          = null;
		private String uuid                = null;
		private Method method              = null;

		public InstanceMethodResourceHandler(final SecurityContext securityContext, final String url, final Class entityClass, final String uuid, final Method method) {

			super(securityContext, url);

			this.entityClass = entityClass;
			this.uuid        = uuid;
			this.method      = method;
		}

		@Override
		public ResultStream doGet(final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {
			throw new IllegalMethodException("GET not allowed on " + getURL());
		}

		@Override
		public RestMethodResult doPost(final Map<String, Object> propertySet) throws FrameworkException {

			final App app           = StructrApp.getInstance(securityContext);
			RestMethodResult result = null;

			if (method != null) {

				try (final Tx tx = app.tx()) {

					final String source = method.getProperty(SchemaMethod.source);

					result = InstanceMethodResource.invoke(securityContext, null, source, propertySet, methodResource.getUriPart(), method.getUuid());

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
			throw new IllegalMethodException("PUT not allowed on " + getURL());
		}

		@Override
		public RestMethodResult doDelete() throws FrameworkException {
			throw new IllegalMethodException("DELETE not allowed on " + getURL());
		}

		@Override
		public boolean isCollection() {
			return false;
		}

		@Override
		public Class getEntityClass() {
			return null;
		}

		// ----- private methods -----
		public static RestMethodResult invoke(final SecurityContext securityContext, final GraphObject entity, final String source, final Map<String, Object> propertySet, final String methodName, final String codeSource) throws FrameworkException {

			System.out.println("########## METHODS: public static RestMethodResult invoke() with String source parameter, method name and Map property set.");

			try {

				return InstanceMethodResource.wrapInResult(Actions.execute(securityContext, entity, "${" + source.trim() + "}", propertySet, methodName, codeSource));

			} catch (UnlicensedScriptException ex) {
				ex.log(logger);
			}

			return new RestMethodResult(500, "Call to unlicensed function, see server log file for more details.");
		}

		public static RestMethodResult wrapInResult(final Object obj) {

			RestMethodResult result = null;

			if (obj instanceof RestMethodResult) {

				result = (RestMethodResult)obj;

			} else {

				result = new RestMethodResult(200);
				result.addContent(obj);

				if (obj instanceof Collection) {

					result.setOverridenResultCount(((Collection)obj).size());
				}

			}

			return result;

		}

		public static SchemaMethod findMethod(final Class type, final String methodName) throws IllegalPathException {

			System.out.println("########## METHODS: public static SchemaMethod findMethod() with type and methodName.");

			try {
				final App app         = StructrApp.getInstance();
				final String typeName = type.getSimpleName();
				Class currentType     = type;

				// first step: schema node or one of its parents
				SchemaNode schemaNode = app.nodeQuery(SchemaNode.class).andName(typeName).getFirst();
				while (schemaNode != null) {

					for (final SchemaMethod method : schemaNode.getProperty(SchemaNode.schemaMethods)) {

						if (methodName.equals(method.getName()) && !method.isJava()) {

							return method;
						}
					}

					currentType = currentType.getSuperclass();
					if (currentType != null) {

						// skip non-dynamic types
						if (currentType.getSimpleName().equals(typeName) || !currentType.getName().startsWith("org.structr.dynamic.")) {
							currentType = currentType.getSuperclass();
						}

						if (currentType != null && currentType.getName().startsWith("org.structr.dynamic.")) {

							schemaNode = app.nodeQuery(SchemaNode.class).andName(currentType.getSimpleName()).getFirst();

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
	}
}
