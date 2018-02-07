/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.schema.action;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.script.Scripting;
import org.structr.core.entity.Principal;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.property.PropertyMap;

/**
 *
 *
 */
public class Actions {

	private static final Logger logger = LoggerFactory.getLogger(Actions.class.getName());

	public static final String NOTIFICATION_LOGIN  = "onStructrLogin";
	public static final String NOTIFICATION_LOGOUT = "onStructrLogout";


	public enum Type {

		Create("onCreation","SecurityContext securityContext, ErrorBuffer errorBuffer", "securityContext, errorBuffer", "onCreate", SecurityContext.class, ErrorBuffer.class),
		Save("onModification", "SecurityContext securityContext, ErrorBuffer errorBuffer, ModificationQueue modificationQueue", "securityContext, errorBuffer, modificationQueue", "onSave", SecurityContext.class, ErrorBuffer.class, ModificationQueue.class),
		Delete("onDeletion", "SecurityContext securityContext, ErrorBuffer errorBuffer, PropertyMap properties", "securityContext, errorBuffer, properties", "onDelete", SecurityContext.class, ErrorBuffer.class, PropertyMap.class),
		Custom("", "", "", "custom"),
		Java("", "", "", "java");

		Type(final String method, final String signature, final String parameters, final String logName, final Class... parameterTypes) {
			this.method         = method;
			this.signature      = signature;
			this.parameters     = parameters;
			this.logName        = logName;
			this.parameterTypes = parameterTypes;
		}

		private String method          = null;
		private String logName         = null;
		private String signature       = null;
		private String parameters      = null;
		private Class[] parameterTypes = null;

		public String getMethod() {
			return method;
		}

		public String getLogName() {
			return logName;
		}

		public String getSignature() {
			return signature;
		}

		public String getParameters() {
			return parameters;
		}

		public Class[] getParameterTypes() {
			return parameterTypes;
		}
	}

	// ----- public static methods -----
	public static Object execute(final SecurityContext securityContext, final GraphObject entity, final String source, final String methodName) throws FrameworkException, UnlicensedException {
		return execute(securityContext, entity, source, Collections.EMPTY_MAP, methodName);
	}

	public static Object execute(final SecurityContext securityContext, final GraphObject entity, final String source, final Map<String, Object> parameters, final String methodName) throws FrameworkException, UnlicensedException {

		final ActionContext context = new ActionContext(securityContext, parameters);
		final Object result         = Scripting.evaluate(context, entity, source, methodName);

		// check for errors raised by scripting
		if (context.hasError()) {
			throw new FrameworkException(422, "Server-side scripting error", context.getErrorBuffer());
		}

		return result;
	}

	/**
	 * Convenience method to call a schema method with a user parameter.
	 * This method is currently used to broadcast login and logout events.
	 *
	 * @param key
	 * @param user
	 *
	 * @return Object the method call result
	 *
	 * @throws FrameworkException
	 */
	public static Object call(final String key, final Principal user) throws FrameworkException, UnlicensedException {

		final Map<String, Object> params = new HashMap<>();
		params.put("user", user);

		return callAsSuperUser(key, params);
	}

	public static Object callAsSuperUser(final String key, final Map<String, Object> parameters) throws FrameworkException, UnlicensedException {

		final SecurityContext superUserContext = SecurityContext.getSuperUserInstance();

		return callWithSecurityContext(key, superUserContext, parameters);

	}

	public static Object callWithSecurityContext(final String key, final SecurityContext securityContext, final Map<String, Object> parameters) throws FrameworkException, UnlicensedException {

		final App app = StructrApp.getInstance(securityContext);

		// we might want to introduce caching here at some point in the future..
		// Cache can be invalidated when the schema is rebuilt for example..

		final List<SchemaMethod> methods = app.nodeQuery(SchemaMethod.class).andName(key).getAsList();
		if (methods.isEmpty()) {

			if (!NOTIFICATION_LOGIN.equals(key) && !NOTIFICATION_LOGOUT.equals(key)) {
				logger.warn("Tried to call method {} but no SchemaMethod entity was found.", key);
			}

		} else {

			for (final SchemaMethod method : methods) {

				// only call methods that are NOT part of a schema node
				final AbstractSchemaNode entity = method.getProperty(SchemaMethod.schemaNode);
				if (entity == null) {

					final String source = method.getProperty(SchemaMethod.source);
					if (source != null) {

						return Actions.execute(securityContext, null, "${" + source + "}", parameters, method.getName());

					} else {

						logger.warn("Schema method {} has no source code, will NOT be executed.", key);
					}

				} else {

					logger.warn("Schema method {} is attached to an entity, will NOT be executed.", key);
				}
			}
		}

		return null;
	}

}
