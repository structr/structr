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
package org.structr.schema.compiler;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import org.structr.common.error.DiagnosticErrorToken;
import org.structr.common.error.ErrorBuffer;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.module.JarConfigurationProvider;

/**
 *
 *
 */
public class NodeExtender {

	private static final Logger logger   = Logger.getLogger(NodeExtender.class.getName());

	private static final JavaCompiler compiler       = ToolProvider.getSystemJavaCompiler();
	private static final JavaFileManager fileManager = new ClassFileManager(compiler.getStandardFileManager(null, null, null));
	private static final ClassLoader classLoader     = fileManager.getClassLoader(null);
	private static final Map<String, Class> classes  = new TreeMap<>();

	private List<JavaFileObject> jfiles  = null;
	private Set<String> fqcns            = null;

	public NodeExtender() {

		jfiles      = new ArrayList<>();
		fqcns       = new LinkedHashSet<>();
	}

	public static ClassLoader getClassLoader() {
		return classLoader;
	}

	public static Class getClass(final String fqcn) {
		return classes.get(fqcn);
	}

	public void addClass(final String className, final String content) throws ClassNotFoundException {

		if (className != null && content != null) {

			final String packageName = JarConfigurationProvider.DYNAMIC_TYPES_PACKAGE;

			jfiles.add(new CharSequenceJavaFileObject(className, content));
			fqcns.add(packageName.concat(".".concat(className)));

			if ("true".equals(Services.getInstance().getConfigurationValue("NodeExtender.log"))) {

				System.out.println("########################################################################################################################################################");
				System.out.println(content);
			}
		}
	}

	public synchronized Map<String, Class> compile(final ErrorBuffer errorBuffer) throws ClassNotFoundException {

		final Writer errorWriter     = new StringWriter();
		final List<Class> newClasses = new LinkedList<>();

		if (!jfiles.isEmpty()) {

			logger.log(Level.FINE, "Compiling {0} dynamic entities...", jfiles.size());

			compiler.getTask(errorWriter, fileManager, new Listener(errorBuffer), null, null, jfiles).call();

			final ClassLoader loader = fileManager.getClassLoader(null);
			boolean success          = true;

			for (final String fqcn : fqcns) {

				try {

					newClasses.add(loader.loadClass(fqcn));

				} catch (Throwable t) {

					logger.log(Level.WARNING, "Unable to load dynamic entity {0}: {1}", new Object[] { fqcn, t.toString() });
					t.printStackTrace();

					success = false;
				}
			}

			if (success) {

				for (final Class oldType : classes.values()) {
					StructrApp.getConfiguration().unregisterEntityType(oldType);
				}

				// clear classes map
				classes.clear();

				// add new classes to map
				for (final Class newType : newClasses) {
					classes.put(newType.getName(), newType);
				}
				
				logger.log(Level.INFO, "Successfully compiled {0} dynamic entities: {1}", new Object[] { jfiles.size(), jfiles.stream().map(f -> f.getName().replaceFirst("/", "")).collect(Collectors.joining(", ")) });
			}

		}

		return classes;
	}

	private static class Listener implements DiagnosticListener<JavaFileObject> {

		private ErrorBuffer errorBuffer = null;

		public Listener(final ErrorBuffer errorBuffer) {
			this.errorBuffer = errorBuffer;
}

		@Override
		public void report(Diagnostic<? extends JavaFileObject> diagnostic) {

			if (diagnostic.getKind().equals(Kind.ERROR)) {

				final JavaFileObject obj = diagnostic.getSource();
				String name        = "unknown";

				if (obj != null && obj instanceof CharSequenceJavaFileObject) {
					name = ((CharSequenceJavaFileObject)obj).getClassName();
				}

				errorBuffer.add(new DiagnosticErrorToken(name, diagnostic));

                                // log also to log file
                                logger.log(Level.WARNING, "Unable to compile dynamic entity {0}: {1}", new Object[] { name, diagnostic.getMessage(Locale.ENGLISH) });
			}
		}
	}
}
