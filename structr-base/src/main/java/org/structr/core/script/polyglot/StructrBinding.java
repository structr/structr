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
package org.structr.core.script.polyglot;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.CaseHelper;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Methods;
import org.structr.core.function.Functions;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.script.polyglot.function.*;
import org.structr.core.script.polyglot.wrappers.*;
import org.structr.core.traits.Traits;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;
import org.structr.schema.action.Function;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Set;

import static org.structr.core.script.polyglot.PolyglotWrapper.wrap;

public class StructrBinding implements ProxyObject {

	private final static Logger logger   = LoggerFactory.getLogger(StructrBinding.class);
	private  ActionContext actionContext = null;
	private GraphObject entity           = null;
	private Value methodParameters       = null;

	public StructrBinding(final ActionContext actionContext, final GraphObject entity) {

		this.actionContext = actionContext;
		this.entity        = entity;
	}

	public ActionContext getActionContext() {
		return actionContext;
	}

	public void setActionContext(final ActionContext actionContext) {
		this.actionContext = actionContext;
	}

	public GraphObject getEntity() {
		return entity;
	}

	public void setEntity(final GraphObject entity) {
		this.entity = entity;
	}

	@Override
	public Object getMember(String name) {

		switch (name) {

			case "get":
				return getGetFunctionWrapper();

			case "this":
				return wrap(actionContext, entity);

			case "me":
				return wrap(actionContext, actionContext.getSecurityContext().getUser(false));

			case "now":
				return ZonedDateTime.now();

			case "predicate":
				return new PredicateBinding(actionContext, entity);

			case "do_in_new_transaction":
			case "doInNewTransaction":
				return new DoInNewTransactionFunction(actionContext, entity);

			case "include_js":
			case "includeJs":
				return new IncludeJSFunction(actionContext);

			case "do_privileged":
			case "doPrivileged":
				return new DoPrivilegedFunction(actionContext);

			case "do_as":
			case "doAs":
				return new DoAsFunction(actionContext);

			case "request":
				return new HttpServletRequestWrapper(actionContext, actionContext.getSecurityContext().getRequest());

			case "session":
				return new HttpSessionWrapper(actionContext, actionContext.getSecurityContext().getSession());

			case "cache":
				return new CacheFunction(actionContext, entity);

			case "vars":
			case "requestStore":
				return new PolyglotProxyMap(actionContext, actionContext.getRequestStore());

			case "applicationStore":
				return new PolyglotProxyMap(actionContext, Services.getInstance().getApplicationStore());

			case "methodParameters":
			case "arguments":
			case "args":
				if (methodParameters != null) {
					return methodParameters;
				}
				return new PolyglotProxyMap(actionContext, actionContext.getContextStore().getTemporaryParameters());

			case "globalSchemaMethods":

				// TODO: remove in first stable release after 6.0
				final String deprecationWarning = "Using deprecated \"$.globalSchemaMethods\" to call a user-defined function, please call it using \"$.\" directly. Support for this will be dropped in the near future.";
				TransactionCommand.simpleBroadcastDeprecationWarning("SCRIPTING", "Deprecation warning", deprecationWarning);
				logger.warn(deprecationWarning);

				return new UserDefinedFunctionWrapper(actionContext);

			case "_functions":

				return new FunctionBinding(actionContext, entity);

			default:

				// look for user-defined function with the given name
				final AbstractMethod method = Methods.resolveMethod(null, name);
				if (method != null) {

					return method.getProxyExecutable(actionContext, null);
				}

				// look for built-in function with the given name first (because it is fast)
				Function<Object, Object> func = Functions.get(CaseHelper.toUnderscore(name, false));
				if (func != null) {

					return new FunctionWrapper(actionContext, entity, func);
				}

				// check if a named constant exists
				if (actionContext.getConstant(name) != null) {
					return wrap(actionContext, actionContext.getConstant(name));
				}

				// check request store
				if (actionContext.getRequestStore().containsKey(name)) {
					return wrap(actionContext, actionContext.getRequestStore().get(name));
				}

				// static type?
				if (Traits.exists(name)) {
					return new StaticTypeWrapper(actionContext, Traits.of(name));
				}

				try {

					return PolyglotWrapper.wrap(actionContext, actionContext.evaluate(entity, name, null, null, 0, new EvaluationHints(), 1, 1));

				} catch (FrameworkException ex) {

					logger.error("Unexpected exception while trying to apply get function shortcut on script binding object.", ex);
				}
		}

		return null;
	}

	@Override
	public Object getMemberKeys() {
		Set<String> keys = actionContext.getRequestStore().keySet();
		keys.add("this");
		keys.add("me");
		keys.add("predicate");
		keys.add("doInNewTransaction");
		keys.add("includeJs");
		keys.add("doPrivileged");
		keys.add("request");
		keys.add("session");
		keys.add("cache");
		keys.add("applicationStore");
		keys.add("methodParameters");
		return keys;
	}

	@Override
	public boolean hasMember(String key) {
		return true;
	}

	@Override
	public void putMember(String key, Value value) {
	}

	public void setMethodParameters(final Value methodParameters) {
		this.methodParameters = methodParameters;
	}

	public Value getMethodParameters() {
		return methodParameters;
	}

	private ProxyExecutable getGetFunctionWrapper() {

		return arguments -> {

			try {
				Object[] args = Arrays.stream(arguments).map(arg -> PolyglotWrapper.unwrap(actionContext, arg)).toArray();

				if (args.length == 1) {

					// Special handling for request keyword, as it needs a wrapper
					if (args[0].toString().equals("request")) {

						return new HttpServletRequestWrapper(actionContext, actionContext.getSecurityContext().getRequest());
					}

					final Object value = actionContext.evaluate(entity, args[0].toString(), null, null, 0, new EvaluationHints(), 1, 1);

					return PolyglotWrapper.wrap(actionContext, value);

				} else if (args.length > 1) {

					final Function<Object, Object> function = Functions.get("get");

					return wrap(actionContext, function.apply(actionContext, entity, args));
				}

			} catch (FrameworkException ex) {

				logger.error("Unexpected exception in StructrBinding.", ex);
			}

			return null;
		};
	}

}
