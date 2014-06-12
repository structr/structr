/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.schema.compiler;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.structr.module.JarConfigurationProvider;

/**
 *
 * @author Christian Morgner (christian@morgner.de)
 */
public class NodeExtender {

	private static final Logger logger   = Logger.getLogger(NodeExtender.class.getName());

	private JavaCompiler compiler        = null;
	private JavaFileManager fileManager  = null;
	private List<JavaFileObject> jfiles  = null;
	private Set<String> fqcns            = null;

	public NodeExtender() {

		compiler    = ToolProvider.getSystemJavaCompiler();
		fileManager = new ClassFileManager(compiler.getStandardFileManager(null, null, null));
		jfiles      = new ArrayList<>();
		fqcns       = new LinkedHashSet<>();
	}

	public ClassLoader getClassLoader() {
		return fileManager.getClassLoader(null);
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

		final Map<String, Class> classes = new LinkedHashMap<>();
		final Writer errorWriter         = new StringWriter();

		if (!jfiles.isEmpty()) {

			logger.log(Level.INFO, "Compiling {0} dynamic entities", jfiles.size());

			compiler.getTask(errorWriter, fileManager, new Listener(errorBuffer), null, null, jfiles).call();

			final ClassLoader loader = fileManager.getClassLoader(null);
			for (final String fqcn : fqcns) {

				try {
					classes.put(fqcn, loader.loadClass(fqcn));

				} catch (Throwable t) {

					logger.log(Level.WARNING, "Unable to compile dynamic entity {0}: {1}", new Object[] { fqcn, t.getMessage() });
				}
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

				errorBuffer.add(name, new DiagnosticErrorToken(diagnostic));
			}
		}
	}
}
