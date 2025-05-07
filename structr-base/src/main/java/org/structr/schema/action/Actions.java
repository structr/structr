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
package org.structr.schema.action;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.ContextStore;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedScriptException;
import org.structr.core.GraphObject;
import org.structr.core.api.Methods;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.FunctionProperty;
import org.structr.core.property.PropertyMap;
import org.structr.core.script.Scripting;
import org.structr.core.script.polyglot.config.ScriptConfig;
import org.structr.core.traits.StructrTraits;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 *
 */
public class Actions {

	private static final Logger logger                         = LoggerFactory.getLogger(Actions.class.getName());
	private static final Map<String, CachedMethod> methodCache = new ConcurrentHashMap<>();

	public static final String NOTIFICATION_LOGIN  = "onStructrLogin";
	public static final String NOTIFICATION_LOGOUT = "onStructrLogout";

	public enum Type {

		OnNodeCreation("onNodeCreation","SecurityContext securityContext", "securityContext", "onNodeCreation", SecurityContext.class),
		Create("onCreation","SecurityContext securityContext, ErrorBuffer errorBuffer", "securityContext, errorBuffer", "onCreate", SecurityContext.class, ErrorBuffer.class),
		AfterCreate("afterCreation","SecurityContext securityContext", "securityContext", "afterCreate", SecurityContext.class),
		Save("onModification", "SecurityContext securityContext, ErrorBuffer errorBuffer, ModificationQueue modificationQueue", "securityContext, errorBuffer, modificationQueue", "onSave", SecurityContext.class, ErrorBuffer.class, ModificationQueue.class),
		AfterSave("afterModification", "SecurityContext securityContext", "securityContext", "afterSave", SecurityContext.class),
		Delete("onNodeDeletion", "SecurityContext securityContext", "securityContext", "onDelete", SecurityContext.class),
		AfterDelete("afterDeletion", "SecurityContext securityContext, PropertyMap properties", "securityContext, properties", "afterDelete", SecurityContext.class, PropertyMap.class),
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
	public static Object execute(final SecurityContext securityContext, final GraphObject entity, final String source, final String methodName, final ModificationQueue modificationEvents, final String codeSource) throws FrameworkException, UnlicensedScriptException {

		final Map<String, Object> parameters = new LinkedHashMap<>();

		parameters.put("modifications", modificationEvents.getModifications(entity));

		return execute(securityContext, entity, source, parameters, methodName, codeSource);
	}

	public static Object execute(final SecurityContext securityContext, final GraphObject entity, final String source, final String methodName) throws FrameworkException, UnlicensedScriptException {
		return execute(securityContext, entity, source, methodName, null);
	}

	public static Object execute(final SecurityContext securityContext, final GraphObject entity, final String source, final String methodName, final String codeSource) throws FrameworkException, UnlicensedScriptException {
		return execute(securityContext, entity, source, Collections.EMPTY_MAP, methodName, codeSource);
	}

	public static Object execute(final SecurityContext securityContext, final GraphObject entity, final String source, final Map<String, Object> parameters, final String methodName, final String codeSource) throws FrameworkException, UnlicensedScriptException {
		final ScriptConfig scriptConfig = ScriptConfig.builder()
				.wrapJsInMain(Settings.WrapJSInMainFunction.getValue(false))
				.build();

		return execute(securityContext, entity, source, parameters, methodName, codeSource, scriptConfig);
	}

	public static Object execute(final SecurityContext securityContext, final GraphObject entity, final String source, final Map<String, Object> parameters, final String methodName, final String codeSource, final ScriptConfig scriptConfig) throws FrameworkException, UnlicensedScriptException {

		final ContextStore store = securityContext.getContextStore();
		final Map<String, Object> previousParams = store.getTemporaryParameters();

		store.setTemporaryParameters(new HashMap<>());

		final ActionContext context = new ActionContext(securityContext, parameters);
		final Object result         = Scripting.evaluate(context, entity, source, methodName, codeSource, scriptConfig);

		store.setTemporaryParameters(previousParams);

		// check for errors raised by scripting
		if (context.hasError()) {
			throw new FrameworkException(422, "Server-side scripting error", context.getErrorBuffer());
		}

		return result;
	}

	public static Object callAsSuperUser(final String key, final Map<String, Object> parameters) throws FrameworkException, UnlicensedScriptException {

		return callAsSuperUser(key, parameters, null);
	}

	public static Object callAsSuperUser(final String key, final Map<String, Object> parameters, final HttpServletRequest request) throws FrameworkException, UnlicensedScriptException {

		final SecurityContext superUserContext = SecurityContext.getSuperUserInstance(request);

		return callWithSecurityContext(key, superUserContext, parameters);
	}

	public static Object callWithSecurityContext(final String key, final SecurityContext securityContext, final Map<String, Object> parameters) throws FrameworkException, UnlicensedScriptException {

		CachedMethod cachedSource = methodCache.get(key);
		if (cachedSource == null) {

			final List<NodeInterface> methods = StructrApp.getInstance().nodeQuery(StructrTraits.SCHEMA_METHOD).name(key).getAsList();
			if (methods.isEmpty()) {

				if (!NOTIFICATION_LOGIN.equals(key) && !NOTIFICATION_LOGOUT.equals(key)) {

					logger.warn("Tried to call method {} but no SchemaMethod entity was found.", key);

					throw new FrameworkException(422, "Cannot execute user-defined function " + key + ": function not found.");
				}

			} else {

				for (final NodeInterface node : methods) {

					final SchemaMethod method = node.as(SchemaMethod.class);

					// only call methods that are NOT part of a schema node
					final AbstractSchemaNode entity = method.getSchemaNode();
					if (entity == null) {

						final String source = method.getSource();
						if (source != null) {

							cachedSource = new CachedMethod(source, method.getName(), method.getUuid(), method.returnRawResult());

							// store in cache
							methodCache.put(key, cachedSource);

						} else {

							logger.warn("Schema method {} has no source code, will NOT be executed.", key);
						}

					} else {

						logger.warn("Schema method {} is attached to an entity, will NOT be executed.", key);
					}
				}
			}

		}

		if (cachedSource != null) {

			final ScriptConfig scriptConfig = ScriptConfig.builder()
					.wrapJsInMain(true)
					.build();

			if (cachedSource.shouldReturnRawResult) {

				securityContext.enableReturnRawResult();
			}

			return Actions.execute(securityContext, null, "${" + StringUtils.strip(cachedSource.sourceCode) + "}", parameters, cachedSource.name, cachedSource.uuidOfSource, scriptConfig);
		}

		return null;
	}

	public static void clearCache() {
		FunctionProperty.clearCache();
		Methods.clearMethodCache();
		methodCache.clear();
	}

	// ----- nested classes -----
	private static class CachedMethod {

		public final String sourceCode;
		public final String uuidOfSource;
		public final String name;
		public final boolean shouldReturnRawResult;

		public CachedMethod(final String sourceCode, final String name, final String uuidOfSource, final boolean shouldReturnRawResult) {

			this.sourceCode            = sourceCode;
			this.uuidOfSource          = uuidOfSource;
			this.name                  = name;
			this.shouldReturnRawResult = shouldReturnRawResult;
		}
	}
}
