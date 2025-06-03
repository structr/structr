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
package org.structr.core.script;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.function.Functions;
import org.structr.schema.action.ActionContext;
import org.structr.schema.compiler.CharSequenceJavaFileObject;
import org.structr.schema.compiler.ClassFileManager;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.lang.reflect.Constructor;
import java.util.LinkedList;
import java.util.List;

/**
 *
 *
 */
public class FunctionsCompiler {

	private static final Logger logger = LoggerFactory.getLogger(FunctionsCompiler.class.getName());

	public static final String PROXY_CLASS_NAME      = "org.structr.core.script.FunctionsProxy";
	private static final JavaCompiler compiler       = ToolProvider.getSystemJavaCompiler();
	private static final JavaFileManager fileManager = new ClassFileManager(compiler.getStandardFileManager(null, null, null));
	private static Constructor constructor           = null;

	private static boolean initialized               = false;

	public static Object getProxy(final SecurityContext securityContext, final ActionContext actionContext, final GraphObject entity) {

		try {
			initializeProxy();

			if (constructor != null) {
				return constructor.newInstance(securityContext, actionContext, entity);
			}

		} catch (Throwable t) {

			logger.warn("", t);
		}

		return null;
	}

	public static void initializeProxy() throws ClassNotFoundException, NoSuchMethodException {

		if (!initialized) {

			final String className             = "org.structr.core.script.FunctionsProxy";
			final List<JavaFileObject> jfiles  = new LinkedList<>();

			jfiles.add(new CharSequenceJavaFileObject(className, getContent()));

			compiler.getTask(null, fileManager, null, null, null, jfiles).call();

			final ClassLoader loader = fileManager.getClassLoader(null);
			final Class proxyClass   = loader.loadClass(PROXY_CLASS_NAME);
			constructor              = proxyClass.getConstructor(SecurityContext.class, ActionContext.class, GraphObject.class);

			initialized = true;
		}
	}

	private static String getContent() {

		final StringBuilder buf = new StringBuilder();

		buf.append("package org.structr.core.script;\n\n");

		buf.append("import org.structr.common.error.FrameworkException;\n");
		buf.append("import org.structr.core.parser.Functions;\n");
		buf.append("import org.structr.schema.action.ActionContext;\n");
		buf.append("import org.structr.common.SecurityContext;\n");
		buf.append("import org.structr.schema.action.Function;\n");
		buf.append("import org.structr.core.GraphObject;\n\n");

		buf.append("public class FunctionsProxy {\n\n");

		buf.append("	private SecurityContext securityContext = null;\n");
		buf.append("	private ActionContext actionContext     = null;\n");
		buf.append("	private GraphObject entity              = null;\n\n");

		buf.append("	public FunctionsProxy(final SecurityContext securityContext, final ActionContext actionContext, final GraphObject entity) {\n");
		buf.append("		this.securityContext = securityContext;\n");
		buf.append("		this.actionContext   = actionContext;\n");
		buf.append("		this.entity          = entity;\n");
		buf.append("	}\n\n");

		for (final String name : Functions.getNames()) {

			if (!"if".equals(name)) {

				buf.append("	public Object ").append(name).append("(final Object... params) throws FrameworkException {\n");
				buf.append("		return Functions.functions.get(\"").append(name).append("\").apply(actionContext, entity, params);\n");
				buf.append("	}\n\n");
			}
		}

		buf.append("}\n");

		return buf.toString();
	}
}
