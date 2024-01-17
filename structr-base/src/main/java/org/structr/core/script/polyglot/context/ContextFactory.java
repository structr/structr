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
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.script.polyglot.AccessProvider;
import org.structr.core.script.polyglot.StructrBinding;
import org.structr.schema.action.ActionContext;

import java.util.concurrent.Callable;
import java.util.function.Predicate;

public abstract class ContextFactory {

	private static String debuggerPath        = "/structr/scripting/remotedebugger/";
	private static String currentDebuggerUUID = "";
	private static Engine engine              = buildEngine();

	// javascript context builder
	private static final Context.Builder jsBuilder = Context.newBuilder("js")
				.engine(engine)
				.allowPolyglotAccess(AccessProvider.getPolyglotAccessConfig())
				.allowHostAccess(AccessProvider.getHostAccessConfig())
				// TODO: Add config switch to toggle Host Class Lookup
				//.allowHostClassLookup(new StructrClassPredicate())
				.allowIO(AccessProvider.getIOAccessConfig())
				.allowExperimentalOptions(true)
				.option("js.foreign-object-prototype", "true")
				.option("js.ecmascript-version", "latest")
				.option("js.temporal", "true");

	// other languages context builder
	private static final Context.Builder genericBuilder = Context.newBuilder()
				.engine(engine)
				.allowPolyglotAccess(AccessProvider.getPolyglotAccessConfig())
				.allowHostAccess(AccessProvider.getHostAccessConfig())
				.allowIO(AccessProvider.getIOAccessConfig())
				.allowHostAccess(AccessProvider.getHostAccessConfig());
				//.allowHostClassLookup(new StructrClassPredicate());

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

			/*
			engineBuilder
					// TODO: Add configurable chrome debug
					.option("inspect", "4242")
					.option("inspect.Path", getDebuggerPath())
					.option("inspect.Suspend", "false");

			 */
		}

		return engineBuilder.build();
	}

	public static Context getContext(final String language) throws FrameworkException {
		return getContext(language, null, null);
	}

	public static Context getContext(final String language, final ActionContext actionContext, final GraphObject entity) throws FrameworkException {

		switch (language) {
			case "js":
				return getAndUpdateContext(language, actionContext, entity, ()->buildJSContext(actionContext, entity));
			case "python":
			case "R":
				return getAndUpdateContext(language, actionContext, entity, ()->buildGenericContext(language, actionContext, entity));
			default:
				throw new FrameworkException(500, "Could not initialize context for language: " + language);
		}
	}

	private static Context getAndUpdateContext(final String language, final ActionContext actionContext, final GraphObject entity, final Callable<Context> contextCreationFunc) throws FrameworkException {

		Context storedContext = actionContext != null ? actionContext.getScriptingContext(language) : null;

		if (actionContext != null && storedContext != null) {

			storedContext = updateBindings(storedContext, language, actionContext, entity);
			actionContext.putScriptingContext(language, storedContext);

		} else {

			try {

				storedContext = contextCreationFunc.call();
				actionContext.putScriptingContext(language, storedContext);

			} catch (Exception ex) {

				throw new FrameworkException(500, "Exception while trying to initialize new context for language: " + language + ". Cause: " + ex.getMessage());
			}
		}

		return  storedContext;
	}

	private static Context buildJSContext(final ActionContext actionContext, final GraphObject entity) {
		return updateBindings(jsBuilder.build(), "js", actionContext, entity);
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

	private static class StructrClassPredicate implements Predicate<String> {
		// Allows manually selected Structr classes to be accessed from scripting contexts

		@Override
		public boolean test(String s) {
			//return s.startsWith("org.structr.api.config.Settings");
			return false;
		}
	}
}
