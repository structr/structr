/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.graalvm.polyglot.io.IOAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.script.polyglot.AccessProvider;
import org.structr.core.script.polyglot.StructrBinding;
import org.structr.schema.action.ActionContext;

import java.sql.Struct;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public abstract class ContextFactory {

	private static final String debuggerPath        = "/structr/scripting/remotedebugger/";
	private static String currentDebuggerUUID = "";
	private static Engine engine              = buildEngine();

	// javascript context builder
	private static final Context.Builder jsBuilder = Context.newBuilder("js")
				.engine(engine)
				.out(new PolyglotOutputStream(LoggerFactory.getLogger("JavaScriptPolyglotContext")))
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
			.out(new PolyglotOutputStream(LoggerFactory.getLogger("PythonPolyglotContext")))
			.allowPolyglotAccess(AccessProvider.getPolyglotAccessConfig())
			.allowHostAccess(AccessProvider.getHostAccessConfig())
			//.allowIO(AccessProvider.getIOAccessConfig())
			/*
			.allowIO(
					IOAccess.newBuilder()
							.allowHostSocketAccess(true)
							.allowHostFileAccess(true)
							.build()
			)
			.allowCreateThread(true)
			*/
			.allowExperimentalOptions(true);
			//.option("python.CoreHome", "/.python/core")
			//.option("python.PythonHome", "/.python/.venv")
			//.option("python.StdLibHome", "/.python/lib/std")
			//.option("python.CAPI", "/.python/lib/c")
			//.option("python.ForceImportSite", "true")
			//.option("python.Executable", "/.python/.venv/bin/python")
			//.option("python.PosixModuleBackend", "java")
			//.option("python.NoUserSiteFlag", "true");


	// other languages context builder
	private static final Context.Builder genericBuilder = Context.newBuilder()
				.engine(engine)
				.allowPolyglotAccess(AccessProvider.getPolyglotAccessConfig())
				.allowHostAccess(AccessProvider.getHostAccessConfig())
				.allowIO(AccessProvider.getIOAccessConfig())
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

		// Loggers and IO
		engineBuilder
				.logHandler(new PolyglotLogHandler())
				//.option("log.level","FINE")
				.out(new PolyglotOutputStream(LoggerFactory.getLogger("GenericPolyglotContext")));


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

	public static ContextFactory.LockedContext getContext(final String language) throws FrameworkException {
		return getContext(language, null, null);
	}

	public static ContextFactory.LockedContext getContext(final String language, final ActionContext actionContext, final GraphObject entity) throws FrameworkException {
		return getContext(language, actionContext, entity, true);
	}

	public static ContextFactory.LockedContext getContext(final String language, final ActionContext actionContext, final GraphObject entity, final boolean allowEntityOverride) throws FrameworkException {

		switch (language) {

			case "js":
				return getOrCreateContext(language, actionContext, entity, ()->buildJSContext(actionContext, entity), allowEntityOverride);

			case "python":
				return getOrCreateContext(language, actionContext, entity, ()->buildPythonContext(actionContext, entity), allowEntityOverride);

			default:
				throw new FrameworkException(500, "Could not initialize context for language: " + language);
		}
	}

	private static ContextFactory.LockedContext getOrCreateContext(final String language, final ActionContext actionContext, final GraphObject entity, final Callable<ContextFactory.LockedContext> contextCreationFunc, final boolean allowEntityOverride) throws FrameworkException {

		ContextFactory.LockedContext storedContext = actionContext != null ? actionContext.getScriptingContext(language) : null;

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
			final StructrBinding structrBinding = storedContext.getBinding();
			structrBinding.setEntity(entity);
		}

		return  storedContext;
	}

	private static ContextFactory.LockedContext buildJSContext(final ActionContext actionContext, final GraphObject entity) {
		return updateBindings(new LockedContext(jsBuilder.build()), "js", actionContext, entity);
	}

	private static ContextFactory.LockedContext buildPythonContext(final ActionContext actionContext, final GraphObject entity) {
		return updateBindings(new LockedContext(pythonBuilder.build()), "python", actionContext, entity);
	}

	private static ContextFactory.LockedContext buildGenericContext(final String language, final ActionContext actionContext, final GraphObject entity) {
		return updateBindings(new LockedContext(genericBuilder.build()), language, actionContext, entity);
	}

	private static ContextFactory.LockedContext updateBindings(final ContextFactory.LockedContext lockedContext, final String language, final ActionContext actionContext, final GraphObject entity) {

		final StructrBinding structrBinding = new StructrBinding(actionContext, entity);

		lockedContext.getLock().lock();
		try {
			Context context = lockedContext.getContext();

			context.getBindings(language).putMember("Structr", structrBinding);

			if (!language.equals("python")) {
				context.getBindings(language).putMember("$", structrBinding);
			}
		} finally {
			lockedContext.getLock().unlock();
		}

		lockedContext.setBinding(structrBinding);
		return lockedContext;
	}

	public static class LockedContext {
		private final ReentrantLock lock = new ReentrantLock();
		private final Context context;
		private StructrBinding binding = null;

		public LockedContext(final Context context) {
			this.context = context;
		}

		public ReentrantLock getLock() {
			return this.lock;
		}

		public void setBinding(final StructrBinding binding) {
			this.binding = binding;
		}

		public StructrBinding getBinding() {
			return this.binding;
		}

		public boolean locksContext(final Context context) {
			return context.equals(this.context);
		}

		public Context getContext() {
			if (this.lock.isHeldByCurrentThread()) {
				return context;
			}
			throw new IllegalStateException("Lock for context is not held by current thread.");
		}
	}
}
