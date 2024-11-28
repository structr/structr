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
package org.structr.core.script.polyglot.context;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.script.polyglot.AccessProvider;
import org.structr.core.script.polyglot.StructrBinding;
import org.structr.schema.action.ActionContext;

import java.util.concurrent.Callable;

public abstract class ContextFactory {

	private static final String debuggerPath        = "/structr/scripting/remotedebugger/";
	private static String currentDebuggerUUID = "";
	private static Engine engine              = buildEngine();

	// javascript context builder
	private static final Context.Builder jsBuilder = Context.newBuilder("js")
				.engine(engine)
				.allowPolyglotAccess(AccessProvider.getPolyglotAccessConfig())
				.allowHostAccess(AccessProvider.getHostAccessConfig())
				.allowHostClassLookup(s -> Settings.AllowedHostClasses.getValue("").contains(s))
				.allowIO(AccessProvider.getIOAccessConfig())
				.allowExperimentalOptions(true)
				.option("js.foreign-object-prototype", "true")
				.option("js.ecmascript-version", "latest")
				.option("js.temporal", "true");

	// Python context builder
	private static final Context.Builder pythonBuilder = Context.newBuilder("python")
			.engine(engine)
			.allowPolyglotAccess(AccessProvider.getPolyglotAccessConfig())
			.allowHostAccess(AccessProvider.getHostAccessConfig())
			.allowIO(AccessProvider.getIOAccessConfig())
			.allowHostAccess(AccessProvider.getHostAccessConfig())
			.allowExperimentalOptions(true)
			.option("python.CoreHome", "/.python/core")
			.option("python.PythonHome", "/.python/.venv")
			.option("python.StdLibHome", "/.python/lib/std")
			.option("python.CAPI", "/.python/lib/c")
			.option("python.JNIHome", "/.python/lib/jni")
			.option("python.ForceImportSite", "true")
			.option("python.Executable", "/.python/.venv/bin/python")
			.option("python.PosixModuleBackend", "java")
			.option("python.NoUserSiteFlag", "true");

	// other languages context builder
	private static final Context.Builder genericBuilder = Context.newBuilder()
				.engine(engine)
				.allowPolyglotAccess(AccessProvider.getPolyglotAccessConfig())
				.allowHostAccess(AccessProvider.getHostAccessConfig())
				.allowIO(AccessProvider.getIOAccessConfig())
				.allowHostAccess(AccessProvider.getHostAccessConfig())
				.allowHostClassLookup(s -> Settings.AllowedHostClasses.getValue("").contains(s));

	public static String getDebuggerPath() {

		return debuggerPath + currentDebuggerUUID;
	}

	public static Engine buildEngine() {

		if (Settings.ScriptingDebugger.getChangeHandler() == null) {

			Settings.ScriptingDebugger.setChangeHandler((setting, oldValue, newValue) -> {

				if (oldValue != newValue) {

					engine = buildEngine();

					genericBuilder.engine(engine);
					jsBuilder.engine(engine);
					pythonBuilder.engine(engine);
				}
			});
		}

		Engine.Builder engineBuilder = Engine.newBuilder();

		// Generic options
		engineBuilder
				.allowExperimentalOptions(true)
				//.option("lsp", "true")
				;

		// Debugging
		if (Settings.ScriptingDebugger.getValue(false)) {

			currentDebuggerUUID = java.util.UUID.randomUUID().toString();

			engineBuilder
					.option("inspect", "4242")
					.option("inspect.Path", getDebuggerPath())
					.option("inspect.Suspend", "false");

		}

		return engineBuilder.build();
	}

	public static Context getContext(final String language) throws FrameworkException {
		return getContext(language, null, null);
	}

	public static Context getContext(final String language, final ActionContext actionContext, final GraphObject entity) throws FrameworkException {
		return getContext(language, actionContext, entity, true);
	}

	public static Context getContext(final String language, final ActionContext actionContext, final GraphObject entity, final boolean allowEntityOverride) throws FrameworkException {

		switch (language) {

			case "js":
				return getOrCreateContext(language, actionContext, entity, ()->buildJSContext(actionContext, entity), allowEntityOverride);

			case "python":
				return getOrCreateContext(language, actionContext, entity, ()->buildPythonContext(actionContext, entity), allowEntityOverride);

			case "R":
				return getOrCreateContext(language, actionContext, entity, ()->buildGenericContext(language, actionContext, entity), allowEntityOverride);

			default:
				throw new FrameworkException(500, "Could not initialize context for language: " + language);
		}
	}

	private static Context getOrCreateContext(final String language, final ActionContext actionContext, final GraphObject entity, final Callable<Context> contextCreationFunc, final boolean allowEntityOverride) throws FrameworkException {

		Context storedContext = actionContext != null ? actionContext.getScriptingContext(language) : null;

		if (actionContext != null && storedContext == null) {

			try {

				storedContext = contextCreationFunc.call();
				updateBindings(storedContext, language, actionContext, entity);
				actionContext.putScriptingContext(language, storedContext);

			} catch (Exception ex) {

				LoggerFactory.getLogger(ContextFactory.class).error("Unexpected exception while initializing language context for language \"{}\".", language, ex);
				throw new FrameworkException(500, "Exception while trying to initialize new context for language: " + language + ". Cause: " + ex.getMessage());
			}
		} else if (actionContext != null && allowEntityOverride) {

			// If binding exists in context, ensure entity is up to date
			final StructrBinding structrBinding = storedContext.getBindings(language).getMember("Structr").asProxyObject();
			structrBinding.setEntity(entity);
		}

		return  storedContext;
	}

	private static Context buildJSContext(final ActionContext actionContext, final GraphObject entity) {
		return updateBindings(jsBuilder.build(), "js", actionContext, entity);
	}

	private static Context buildPythonContext(final ActionContext actionContext, final GraphObject entity) {
		Context ctx = pythonBuilder.build();
		return updateBindings(ctx, "python", actionContext, entity);
	}

	private static Context buildGenericContext(final String language, final ActionContext actionContext, final GraphObject entity) {
		return updateBindings(genericBuilder.build(), language, actionContext, entity);
	}

	private static Context updateBindings(final Context context, final String language, final ActionContext actionContext, final GraphObject entity) {

		final StructrBinding structrBinding = new StructrBinding(actionContext, entity);

		context.getBindings(language).putMember("Structr", structrBinding);

		if (!language.equals("python") && !language.equals("R")) {
			context.getBindings(language).putMember("$", structrBinding);
		}

		return context;
	}
}
